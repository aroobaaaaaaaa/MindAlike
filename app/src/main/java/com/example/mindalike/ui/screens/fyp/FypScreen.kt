package com.example.mindalike.ui.screens.fyp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mindalike.data.FriendshipStatus
import com.example.mindalike.data.Profile
import com.example.mindalike.data.cancelFriendRequest
import com.example.mindalike.data.getFriendshipStatus
import com.example.mindalike.data.sendFriendRequest
import com.example.mindalike.data.supabase
import com.example.mindalike.ui.components.LocalAppNavController
import com.example.mindalike.ui.screens.community.CommunitiesTab
import com.example.mindalike.ui.screens.community.CommunityChatScreen
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items

private val LightBrown = Color(0xFF8C6D58)
private val LightBrownBorder = Color(0xFFE2D7CE)
private val CleanBackground = Color(0xFFFBF8F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FypScreen(
    viewModel: FypViewModel = viewModel(),
    onNavigateToUserProfile: (userId: String) -> Unit = {},
    onNavigateToFypFeed: (initialPostId: String) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var openCommunityId by remember { mutableStateOf<String?>(null) }
    val nav = LocalAppNavController.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val postsState by viewModel.postsState.collectAsState()
    val profilesState by viewModel.profilesState.collectAsState()

    // A community chat opened from the Communities tab takes over this screen
    val openId = openCommunityId
    if (openId != null) {
        BackHandler { openCommunityId = null }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp) // Applied bottom padding here
        ) {
            CommunityChatScreen(
                communityId = openId,
                onBack = { openCommunityId = null },
                onUserProfileClick = onNavigateToUserProfile
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            viewModel.loadFypData(query)
                        },
                        placeholder = {
                            Text(
                                text = "Search feeds, groups, people...",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = LightBrown
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightBrown,
                            unfocusedBorderColor = LightBrownBorder,
                            focusedContainerColor = CleanBackground,
                            unfocusedContainerColor = CleanBackground
                        )
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CleanBackground)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = LightBrown,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = LightBrown
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Posts", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Communities", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Profiles", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTabIndex) {
                0 -> MediaGridTabContent(postsState, onNavigateToFypFeed)
                1 -> CommunitiesTab(onCommunityClick = { id ->
                    if (nav != null) nav.navigate("community/$id") else openCommunityId = id
                })
                2 -> ProfilesTabContent(profilesState, onNavigateToUserProfile)
            }
        }
    }
}

// ---------------- TAB 1: INSTAGRAM STYLE 3x3 GRID ----------------
@Composable
private fun MediaGridTabContent(
    state: FypUiState<PostWithAuthor>,
    onNavigateToFypFeed: (initialPostId: String) -> Unit
) {
    when (state) {
        is FypUiState.Loading -> LoadingView()
        is FypUiState.Error -> ErrorView(state.message)
        is FypUiState.Success -> {
            if (state.data.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No posts found", fontSize = 13.sp, color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(1.dp)
                ) {
                    items(state.data) { item ->
                        val post = item.post
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .background(Color.White)
                                .clickable {
                                    post.id?.let { id ->
                                        onNavigateToFypFeed(id)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!post.mediaUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = post.mediaUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = post.content,
                                    fontSize = 11.sp,
                                    maxLines = 4,
                                    color = Color(0xFF1C1B1F),
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 3: PROFILES ----------------
@Composable
private fun ProfilesTabContent(
    state: FypUiState<Profile>,
    onNavigateToUserProfile: (userId: String) -> Unit
) {
    when (state) {
        is FypUiState.Loading -> LoadingView()
        is FypUiState.Error -> ErrorView(state.message)
        is FypUiState.Success -> {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
                items(state.data) { profile ->
                    UserProfileCard(profile, onNavigateToUserProfile)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserProfileCard(
    profile: Profile,
    onNavigateToUserProfile: (userId: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUserId = supabase.auth.currentUserOrNull()?.id ?: ""
    var friendshipStatus by remember { mutableStateOf(FriendshipStatus.NONE) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(profile.id) {
        if (currentUserId.isNotBlank()) {
            friendshipStatus = getFriendshipStatus(currentUserId, profile.id)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToUserProfile(profile.id) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = profile.avatarUrl ?: "https://via.placeholder.com/150",
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(LightBrownBorder)
                    .clickable { onNavigateToUserProfile(profile.id) },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onNavigateToUserProfile(profile.id) }
                )
                if (!profile.universityName.isNullOrBlank()) {
                    Text(text = profile.universityName, fontSize = 12.sp, color = Color.Gray)
                }

                if (profile.badges.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        profile.badges.take(3).forEach { badge ->
                            Surface(
                                color = LightBrown.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 10.sp,
                                    color = LightBrown,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Request Button
            val isRequested = friendshipStatus == FriendshipStatus.PENDING_SENT
            val isFriends = friendshipStatus == FriendshipStatus.FRIENDS

            Button(
                onClick = {
                    if (currentUserId.isBlank() || isLoading || isFriends) return@Button

                    val previous = friendshipStatus
                    friendshipStatus = if (isRequested) FriendshipStatus.NONE else FriendshipStatus.PENDING_SENT

                    scope.launch {
                        isLoading = true
                        val success = if (previous == FriendshipStatus.NONE) {
                            sendFriendRequest(currentUserId, profile.id)
                        } else {
                            cancelFriendRequest(currentUserId, profile.id)
                        }
                        if (!success) friendshipStatus = previous
                        isLoading = false
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isFriends -> Color(0xFFEFEFEF)
                        isRequested -> LightBrown.copy(alpha = 0.15f)
                        else -> LightBrown
                    },
                    contentColor = when {
                        isFriends -> Color.Gray
                        isRequested -> LightBrown
                        else -> Color.White
                    }
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = LightBrown, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = when {
                            isFriends -> "Friends"
                            isRequested -> "Requested"
                            else -> "+ Add"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------- COMMON UI STATES ----------------
@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LightBrown)
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = Color.Red, fontSize = 14.sp)
    }
}