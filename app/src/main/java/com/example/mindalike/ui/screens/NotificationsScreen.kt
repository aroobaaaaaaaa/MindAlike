package com.example.mindalike.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mindalike.data.FriendRequest
import com.example.mindalike.data.Post
import com.example.mindalike.data.PostLike
import com.example.mindalike.data.Profile
import com.example.mindalike.data.acceptFriendRequest
import com.example.mindalike.data.declineFriendRequest
import com.example.mindalike.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

private val LightBrown = Color(0xFF8C6D58)
private val LightBrownBorder = Color(0xFFE2D7CE)
private val CleanBackground = Color(0xFFFBF8F5)

sealed interface NotificationData {
    val createdAt: String?

    data class FriendRequestNotice(
        val request: FriendRequest,
        val sender: Profile
    ) : NotificationData {
        override val createdAt: String? get() = request.createdAt
    }

    data class FriendAcceptedNotice(
        val request: FriendRequest,
        val other: Profile,
        val iAccepted: Boolean // true = I accepted them, false = they accepted me
    ) : NotificationData {
        override val createdAt: String? get() = request.createdAt
    }

    data class LikeNotice(
        val like: PostLike,
        val user: Profile,
        val post: Post
    ) : NotificationData {
        override val createdAt: String? get() = like.createdAt
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onUserProfileClick: (userId: String) -> Unit = {},
    onPostClick: (userId: String, postId: String, postType: String) -> Unit = { _, _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val currentUserId = supabase.auth.currentUserOrNull()?.id ?: ""

    var notifications by remember { mutableStateOf<List<NotificationData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadNotifications() {
        scope.launch {
            isLoading = true
            try {
                if (currentUserId.isNotBlank()) {
                    // 1. Pending friend requests sent to me
                    val requests = supabase.from("friend_requests")
                        .select {
                            filter {
                                eq("receiver_id", currentUserId)
                                eq("status", "pending")
                            }
                        }.decodeList<FriendRequest>()

                    // 2. Accepted requests (either I accepted, or they accepted mine)
                    val accepted = supabase.from("friend_requests")
                        .select {
                            filter {
                                eq("status", "accepted")
                                or {
                                    eq("receiver_id", currentUserId)
                                    eq("sender_id", currentUserId)
                                }
                            }
                        }.decodeList<FriendRequest>()

                    // 3. My posts, to find likes
                    val myPosts = supabase.from("posts")
                        .select { filter { eq("user_id", currentUserId) } }
                        .decodeList<Post>()

                    val myPostIds = myPosts.mapNotNull { it.id }
                    val myPostsMap = myPosts.associateBy { it.id }

                    val likes = if (myPostIds.isNotEmpty()) {
                        supabase.from("post_likes")
                            .select {
                                filter {
                                    isIn("post_id", myPostIds)
                                    neq("user_id", currentUserId)
                                }
                            }.decodeList<PostLike>()
                    } else emptyList()

                    // 4. Load all the profiles we need
                    val acceptedOtherIds = accepted.map {
                        if (it.senderId == currentUserId) it.receiverId else it.senderId
                    }
                    val allUserIds = (
                            requests.map { it.senderId } +
                                    likes.map { it.userId } +
                                    acceptedOtherIds
                            ).distinct()

                    val usersMap = if (allUserIds.isNotEmpty()) {
                        supabase.from("profiles")
                            .select { filter { isIn("id", allUserIds) } }
                            .decodeList<Profile>()
                            .associateBy { it.id }
                    } else emptyMap()

                    // 5. Build notices
                    val friendNotices = requests.mapNotNull { req ->
                        usersMap[req.senderId]?.let { sender ->
                            NotificationData.FriendRequestNotice(req, sender)
                        }
                    }

                    val acceptedNotices = accepted.mapNotNull { req ->
                        val iAccepted = req.receiverId == currentUserId
                        val otherId = if (iAccepted) req.senderId else req.receiverId
                        usersMap[otherId]?.let { other ->
                            NotificationData.FriendAcceptedNotice(req, other, iAccepted)
                        }
                    }

                    val likeNotices = likes.mapNotNull { like ->
                        val liker = usersMap[like.userId]
                        val post = myPostsMap[like.postId]
                        if (liker != null && post != null) {
                            NotificationData.LikeNotice(like, liker, post)
                        } else null
                    }

                    notifications = (friendNotices + acceptedNotices + likeNotices)
                        .sortedByDescending { it.createdAt ?: "" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadNotifications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = CleanBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LightBrown)
                }
            } else if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No notifications yet",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications) { item ->
                        when (item) {
                            is NotificationData.FriendRequestNotice -> {
                                FriendRequestCard(
                                    item = item,
                                    currentUserId = currentUserId,
                                    onUserProfileClick = onUserProfileClick,
                                    onRefresh = { loadNotifications() }
                                )
                            }
                            is NotificationData.FriendAcceptedNotice -> {
                                FriendAcceptedCard(
                                    item = item,
                                    onUserProfileClick = onUserProfileClick
                                )
                            }
                            is NotificationData.LikeNotice -> {
                                LikeNoticeCard(
                                    item = item,
                                    onUserProfileClick = onUserProfileClick,
                                    onPostClick = onPostClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRequestCard(
    item: NotificationData.FriendRequestNotice,
    currentUserId: String,
    onUserProfileClick: (userId: String) -> Unit,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val req = item.request
    val sender = item.sender
    val timeAgo = parseTimeAgo(req.createdAt)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                model = sender.avatarUrl ?: "https://via.placeholder.com/150",
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LightBrownBorder)
                    .clickable { onUserProfileClick(sender.id) },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sender.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onUserProfileClick(sender.id) }
                    )
                    if (timeAgo.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "• $timeAgo", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Text(
                    text = "sent you a friend request",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            req.id?.let { reqId ->
                                acceptFriendRequest(reqId, sender.id, currentUserId)
                                onRefresh()
                            }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LightBrown)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accept",
                        modifier = Modifier.size(15.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            req.id?.let { reqId ->
                                declineFriendRequest(reqId)
                                onRefresh()
                            }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reject",
                        modifier = Modifier.size(15.dp),
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendAcceptedCard(
    item: NotificationData.FriendAcceptedNotice,
    onUserProfileClick: (userId: String) -> Unit
) {
    val other = item.other
    val timeAgo = parseTimeAgo(item.request.createdAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserProfileClick(other.id) },
        shape = RoundedCornerShape(12.dp),
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
                model = other.avatarUrl ?: "https://via.placeholder.com/150",
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LightBrownBorder),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = other.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (timeAgo.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "• $timeAgo", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Text(
                    text = if (item.iAccepted) "is now your friend" else "accepted your friend request",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun LikeNoticeCard(
    item: NotificationData.LikeNotice,
    onUserProfileClick: (userId: String) -> Unit,
    onPostClick: (userId: String, postId: String, postType: String) -> Unit
) {
    val liker = item.user
    val post = item.post
    val timeAgo = parseTimeAgo(item.like.createdAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                post.id?.let { postId ->
                    onPostClick(post.userId, postId, post.postType)
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = liker.avatarUrl ?: "https://via.placeholder.com/150",
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(LightBrownBorder)
                        .clickable { onUserProfileClick(liker.id) },
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = liker.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onUserProfileClick(liker.id) }
                    )
                    if (timeAgo.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "• $timeAgo", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Text(
                    text = "liked your post",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (!post.mediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (post.content.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CleanBackground)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.content,
                        fontSize = 9.sp,
                        maxLines = 3,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

// Formats ISO timestamp strings into relative time (e.g. 5m, 2h, 3d)
private fun parseTimeAgo(timestampStr: String?): String {
    if (timestampStr.isNullOrBlank()) return ""
    return try {
        val createdInstant = Instant.parse(timestampStr)
        val seconds = Duration.between(createdInstant, Instant.now()).seconds
        when {
            seconds < 60 -> "just now"
            seconds < 3600 -> "${seconds / 60}m"
            seconds < 86400 -> "${seconds / 3600}h"
            seconds < 604800 -> "${seconds / 86400}d"
            else -> "${seconds / 604800}w"
        }
    } catch (e: DateTimeParseException) {
        ""
    } catch (e: Exception) {
        ""
    }
}