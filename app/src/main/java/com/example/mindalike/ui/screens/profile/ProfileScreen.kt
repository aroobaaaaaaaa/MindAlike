package com.example.mindalike.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.mindalike.data.Community
import com.example.mindalike.data.CommunityMember
import com.example.mindalike.data.Friend
import com.example.mindalike.data.FriendshipStatus
import com.example.mindalike.data.Post
import com.example.mindalike.data.Profile
import com.example.mindalike.data.cancelFriendRequest
import com.example.mindalike.data.getFriendshipStatus
import com.example.mindalike.data.sendFriendRequest
import com.example.mindalike.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

private val LightBrown = Color(0xFF8C6D58)
private val LightBrownBorder = Color(0xFFE2D7CE)
private val OffWhite = Color(0xFFFAF9F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    isSelfProfile: Boolean = true,
    onSignOut: (() -> Unit)? = null,
    onPostClick: (postId: String, postType: String) -> Unit = { _, _ -> },
    onCommunityClick: (communityId: String) -> Unit = {},
    onUserProfileClick: (userId: String) -> Unit = {},
    onMessageClick: (userId: String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val currentUserId = supabase.auth.currentUserOrNull()?.id ?: ""

    var profile by remember { mutableStateOf<Profile?>(null) }
    var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var joinedCommunities by remember { mutableStateOf<List<Community>>(emptyList()) }
    var friendList by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showFullAvatarDialog by remember { mutableStateOf(false) }
    var showFriendsSheet by remember { mutableStateOf(false) }

    var friendshipStatus by remember { mutableStateOf(FriendshipStatus.NONE) }
    var isActionLoading by remember { mutableStateOf(false) }

    fun fetchProfileData() {
        scope.launch {
            isLoading = true
            try {
                profile = supabase.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<Profile>()

                userPosts = supabase.from("posts")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<Post>()

                val friends1 = supabase.from("friends")
                    .select { filter { eq("user_1", userId) } }
                    .decodeList<Friend>()
                val friends2 = supabase.from("friends")
                    .select { filter { eq("user_2", userId) } }
                    .decodeList<Friend>()

                val friendUserIds = (friends1.map { it.user2 } + friends2.map { it.user1 }).distinct()

                if (friendUserIds.isNotEmpty()) {
                    friendList = supabase.from("profiles")
                        .select { filter { isIn("id", friendUserIds) } }
                        .decodeList<Profile>()
                } else {
                    friendList = emptyList()
                }

                val memberships = supabase.from("community_members")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<CommunityMember>()

                if (memberships.isNotEmpty()) {
                    val communityIds = memberships.map { it.communityId }
                    joinedCommunities = supabase.from("communities")
                        .select { filter { isIn("id", communityIds) } }
                        .decodeList<Community>()
                } else {
                    joinedCommunities = emptyList()
                }

                if (!isSelfProfile && currentUserId.isNotBlank()) {
                    friendshipStatus = getFriendshipStatus(currentUserId, userId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun removeFriend(targetFriendId: String) {
        scope.launch {
            try {
                supabase.from("friends").delete {
                    filter {
                        or {
                            and {
                                eq("user_1", currentUserId)
                                eq("user_2", targetFriendId)
                            }
                            and {
                                eq("user_1", targetFriendId)
                                eq("user_2", currentUserId)
                            }
                        }
                    }
                }
                fetchProfileData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(userId) {
        fetchProfileData()
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LightBrown)
        }
        return
    }

    val currentProfile = profile ?: return

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = {
                    Text(
                        text = currentProfile.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1C1B1F)
                    )
                },
                actions = {
                    if (isSelfProfile) {
                        IconButton(onClick = {
                            scope.launch {
                                supabase.auth.signOut()
                                onSignOut?.invoke()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", tint = Color.Gray)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val avatarUrl = currentProfile.avatarUrl?.takeIf { it.isNotBlank() }

                AsyncImage(
                    model = avatarUrl ?: "https://via.placeholder.com/150",
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, LightBrownBorder, CircleShape)
                        .clickable {
                            if (!avatarUrl.isNullOrBlank()) {
                                showFullAvatarDialog = true
                            }
                        },
                    contentScale = ContentScale.Crop
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.weight(1f)
                ) {
                    StatItem(count = userPosts.size.toString(), label = "Posts")
                    StatItem(
                        count = friendList.size.toString(),
                        label = "Friends",
                        onClick = { showFriendsSheet = true }
                    )
                    StatItem(
                        count = joinedCommunities.size.toString(),
                        label = "Hubs",
                        onClick = { selectedTab = 2 }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                val degreeText = currentProfile.degree?.takeIf { it.isNotBlank() }
                val uniText = currentProfile.universityName?.takeIf { it.isNotBlank() }
                val academicInfo = listOfNotNull(degreeText, uniText).joinToString(" • ")

                if (academicInfo.isNotBlank()) {
                    Text(
                        text = academicInfo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightBrown
                    )
                }

                if (!currentProfile.bio.isNullOrBlank()) {
                    Text(
                        text = currentProfile.bio,
                        fontSize = 13.sp,
                        color = Color(0xFF303030),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (!currentProfile.location.isNullOrBlank()) {
                    Text(
                        text = "📍 ${currentProfile.location}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (currentProfile.badges.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(currentProfile.badges) { badge ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LightBrown.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LightBrownBorder.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LightBrown,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSelfProfile) {
                    OutlinedButton(
                        onClick = { showEditSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LightBrownBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LightBrown)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    val isPending = friendshipStatus == FriendshipStatus.PENDING_SENT
                    val isFriends = friendshipStatus == FriendshipStatus.FRIENDS

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (currentUserId.isBlank() || isActionLoading || isFriends) return@Button

                                val previousStatus = friendshipStatus
                                if (isPending) {
                                    friendshipStatus = FriendshipStatus.NONE
                                } else if (friendshipStatus == FriendshipStatus.NONE) {
                                    friendshipStatus = FriendshipStatus.PENDING_SENT
                                }

                                scope.launch {
                                    isActionLoading = true
                                    val success = if (previousStatus == FriendshipStatus.NONE) {
                                        sendFriendRequest(currentUserId, userId)
                                    } else if (previousStatus == FriendshipStatus.PENDING_SENT) {
                                        cancelFriendRequest(currentUserId, userId)
                                    } else {
                                        true
                                    }

                                    if (!success) {
                                        friendshipStatus = previousStatus
                                    }
                                    isActionLoading = false
                                }
                            },
                            enabled = !isActionLoading && !isFriends,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = if (isPending || isFriends) 0.dp else 2.dp
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isFriends -> Color(0xFFEFEFEF)
                                    isPending -> LightBrown.copy(alpha = 0.12f)
                                    else -> LightBrown
                                },
                                contentColor = when {
                                    isFriends -> Color.DarkGray
                                    isPending -> LightBrown
                                    else -> Color.White
                                },
                                disabledContainerColor = if (isFriends) Color(0xFFEFEFEF) else LightBrown.copy(alpha = 0.12f),
                                disabledContentColor = if (isFriends) Color.DarkGray else LightBrown
                            )
                        ) {
                            if (isActionLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = if (isPending) LightBrown else Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val (icon, text) = when (friendshipStatus) {
                                        FriendshipStatus.FRIENDS -> Icons.Default.Group to "Friends ✓"
                                        FriendshipStatus.PENDING_SENT -> Icons.Default.Close to "Requested"
                                        FriendshipStatus.PENDING_RECEIVED -> Icons.Default.Check to "Accept Request"
                                        FriendshipStatus.NONE -> Icons.Default.PersonAdd to "Add Friend"
                                    }

                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = text,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { onMessageClick(userId) },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LightBrownBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LightBrown)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Message", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = LightBrown,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = LightBrown
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Grid", tint = if (selectedTab == 0) LightBrown else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Threads", tint = if (selectedTab == 1) LightBrown else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Hubs", tint = if (selectedTab == 2) LightBrown else Color.Gray) }
                )
            }

            when (selectedTab) {
                0 -> MediaGrid(
                    posts = userPosts.filter { it.postType == "media" },
                    onPostClick = { postId -> onPostClick(postId, "media") }
                )
                1 -> ThreadFeed(
                    posts = userPosts.filter { it.postType == "thread" },
                    onPostClick = { postId -> onPostClick(postId, "thread") }
                )
                2 -> CommunityList(
                    communities = joinedCommunities,
                    onCommunityClick = onCommunityClick
                )
            }
        }
    }

    // FRIENDS BOTTOM SHEET
    if (showFriendsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFriendsSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Friends (${friendList.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1C1B1F),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (friendList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No friends yet", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(friendList) { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showFriendsSheet = false
                                        onUserProfileClick(friend.id)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = friend.avatarUrl ?: "https://via.placeholder.com/150",
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(LightBrownBorder),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = friend.fullName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    if (!friend.universityName.isNullOrBlank()) {
                                        Text(
                                            text = friend.universityName,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                if (isSelfProfile) {
                                    IconButton(
                                        onClick = { removeFriend(friend.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonRemove,
                                            contentDescription = "Remove Friend",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFullAvatarDialog && !currentProfile.avatarUrl.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { showFullAvatarDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { showFullAvatarDialog = false },
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { showFullAvatarDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                AsyncImage(
                    model = currentProfile.avatarUrl,
                    contentDescription = "Full Avatar",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    if (showEditSheet) {
        EditProfileBottomSheet(
            profile = currentProfile,
            onDismiss = { showEditSheet = false },
            onProfileUpdated = {
                showEditSheet = false
                fetchProfileData()
            }
        )
    }
}

@Composable
private fun StatItem(
    count: String,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Text(text = count, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1C1B1F))
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun MediaGrid(
    posts: List<Post>,
    onPostClick: (postId: String) -> Unit
) {
    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No posts yet", fontSize = 13.sp, color = Color.Gray)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(1.dp)
        ) {
            items(posts) { post ->
                AsyncImage(
                    model = post.mediaUrl ?: "",
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(1.dp)
                        .background(OffWhite)
                        .clickable {
                            post.id?.let { id -> onPostClick(id) }
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun ThreadFeed(
    posts: List<Post>,
    onPostClick: (postId: String) -> Unit
) {
    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No threads created yet", fontSize = 13.sp, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(posts) { post ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            post.id?.let { id -> onPostClick(id) }
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = OffWhite)
                ) {
                    Text(
                        text = post.content,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp,
                        color = Color(0xFF1C1B1F)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityList(
    communities: List<Community>,
    onCommunityClick: (communityId: String) -> Unit
) {
    if (communities.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hubs joined yet", fontSize = 13.sp, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp)
        ) {
            items(communities) { community ->
                ListItem(
                    modifier = Modifier.clickable {
                        community.id?.let { id -> onCommunityClick(id) }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.White),
                    headlineContent = { Text(community.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                    supportingContent = { Text(community.description ?: "", maxLines = 1, fontSize = 12.sp, color = Color.Gray) },
                    leadingContent = {
                        AsyncImage(
                            model = community.iconUrl ?: "",
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(OffWhite)
                        )
                    }
                )
                HorizontalDivider(color = LightBrownBorder.copy(alpha = 0.5f))
            }
        }
    }
}