package com.example.mindalike.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindalike.data.Friend
import com.example.mindalike.data.PostFeedItem
import com.example.mindalike.data.supabase
import com.example.mindalike.ui.components.PostCard
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

private val LightBrown = Color(0xFF8C6D58)
private val OffWhite = Color(0xFFFAF9F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToNotifications: () -> Unit,
    onNavigateToDirectMessages: () -> Unit,
    onNavigateToAddPost: (postType: String) -> Unit,
    onNavigateToProfile: (userId: String) -> Unit = {} // Added navigation callback
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var feedItems by remember { mutableStateOf<List<PostFeedItem>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingInitial by remember { mutableStateOf(true) }
    var showAddPostDialog by remember { mutableStateOf(false) }

    var isTopBarVisible by rememberSaveable { mutableStateOf(true) }
    var previousIndex by rememberSaveable { mutableIntStateOf(0) }
    var previousScrollOffset by rememberSaveable { mutableIntStateOf(0) }

    val currentUserId = supabase.auth.currentUserOrNull()?.id ?: ""

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (currentIndex, currentOffset) ->
                if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousScrollOffset + 10)) {
                    isTopBarVisible = false
                } else if (currentIndex < previousIndex || (currentIndex == previousIndex && currentOffset < previousScrollOffset - 10)) {
                    isTopBarVisible = true
                }
                previousIndex = currentIndex
                previousScrollOffset = currentOffset
            }
    }

    fun fetchHomeFeed(isPullToRefresh: Boolean = false) {
        scope.launch {
            if (isPullToRefresh) isRefreshing = true else if (feedItems.isEmpty()) isLoadingInitial = true
            try {
                if (currentUserId.isBlank()) return@launch

                val friends1 = supabase.from("friends")
                    .select { filter { eq("user_1", currentUserId) } }
                    .decodeList<Friend>()
                val friends2 = supabase.from("friends")
                    .select { filter { eq("user_2", currentUserId) } }
                    .decodeList<Friend>()

                val friendUserIds = (friends1.map { it.user2 } + friends2.map { it.user1 } + currentUserId).distinct()

                feedItems = supabase.from("posts")
                    .select(
                        columns = Columns.raw(
                            "*, profiles(*), " +
                                    "post_likes(id, post_id, user_id, created_at, profiles(*)), " +
                                    "post_comments(id, post_id, user_id, content, created_at, profiles(*))"
                        )
                    ) {
                        filter { isIn("user_id", friendUserIds) }
                    }
                    .decodeList<PostFeedItem>()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error loading feed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isRefreshing = false
                isLoadingInitial = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (feedItems.isEmpty()) {
            fetchHomeFeed()
        }
    }

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures { change, dragAmount ->
                change.consume()
                if (dragAmount < -50) {
                    onNavigateToDirectMessages()
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoadingInitial) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LightBrown)
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isTopBarVisible = true
                        fetchHomeFeed(isPullToRefresh = true)
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (feedItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No posts in your feed yet.\nAdd friends or share something!",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 60.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = feedItems,
                                key = { it.id }
                            ) { item ->
                                PostCard(
                                    postId = item.id,
                                    authorId = item.userId,
                                    authorName = item.profile?.fullName ?: "Anonymous",
                                    authorAvatar = item.profile?.avatarUrl,
                                    content = item.content,
                                    mediaUrl = item.mediaUrl,
                                    postType = item.postType,
                                    initialLikes = item.likes,
                                    initialComments = item.comments,
                                    currentUserId = currentUserId,
                                    onPostDeleted = {
                                        fetchHomeFeed(isPullToRefresh = false)
                                    },
                                    onProfileClick = { authorId ->
                                        onNavigateToProfile(authorId)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isTopBarVisible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showAddPostDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Post",
                                tint = Color(0xFF1C1B1F),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Text(
                            text = "Mindalike",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = LightBrown
                        )

                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF1C1B1F),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddPostDialog) {
        AlertDialog(
            onDismissRequest = { showAddPostDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Create New",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1C1B1F)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = OffWhite,
                        onClick = {
                            showAddPostDialog = false
                            onNavigateToAddPost("media")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = LightBrown)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Media Post", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Share photos or images", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = OffWhite,
                        onClick = {
                            showAddPostDialog = false
                            onNavigateToAddPost("thread")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ShortText, contentDescription = null, tint = LightBrown)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Text Thread", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Share thoughts or start discussions", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}