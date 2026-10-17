package com.example.mindalike.ui.screens.profile

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mindalike.data.PostFeedItem
import com.example.mindalike.data.Profile
import com.example.mindalike.data.supabase
import com.example.mindalike.ui.components.PostCard
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

private val LightBrown = Color(0xFF8C6D58)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFeedScreen(
    userId: String,
    initialPostId: String,
    postType: String,
    currentUserId: String,
    onBackClick: () -> Unit,
    onProfileClick: (userId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var posts by remember { mutableStateOf<List<PostFeedItem>>(emptyList()) }
    var authorProfile by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId, postType) {
        Log.d("ProfileFeedDebug", "LaunchedEffect triggered with userId='$userId', postType='$postType'")

        if (userId.isBlank()) {
            Log.w("ProfileFeedDebug", "userId is blank! Stopping loading state.")
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        try {
            // Add a 10-second safety timeout so OkHttp never hangs forever
            withTimeoutOrNull(10_000L) {

                // 1. Fetch Profile cleanly
                try {
                    authorProfile = supabase.from("profiles")
                        .select { filter { eq("id", userId) } }
                        .decodeSingleOrNull<Profile>()
                    Log.d("ProfileFeedDebug", "Author profile loaded: ${authorProfile?.fullName}")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("ProfileFeedDebug", "Failed to load author profile", e)
                }

                // 2. Query posts matching HomeScreen structure
                Log.d("ProfileFeedDebug", "Fetching posts for userId=$userId, postType=$postType...")

                val fetchedPosts = supabase.from("posts")
                    .select(
                        columns = Columns.raw(
                            "*, profiles(*), " +
                                    "post_likes(id, post_id, user_id, created_at, profiles(*)), " +
                                    "post_comments(id, post_id, user_id, content, created_at, profiles(*))"
                        )
                    ) {
                        filter {
                            eq("user_id", userId)
                            eq("post_type", postType)
                        }
                    }
                    .decodeList<PostFeedItem>()

                Log.d("ProfileFeedDebug", "Posts fetched count: ${fetchedPosts.size}")
                posts = fetchedPosts

                // 3. Scroll to initial target post
                if (initialPostId.isNotBlank() && fetchedPosts.isNotEmpty()) {
                    val targetIndex = fetchedPosts.indexOfFirst { it.id == initialPostId }
                    if (targetIndex != -1) {
                        listState.scrollToItem(targetIndex)
                    }
                }
            } ?: run {
                Log.e("ProfileFeedDebug", "Fetch timed out after 10 seconds!")
            }

        } catch (e: CancellationException) {
            Log.d("ProfileFeedDebug", "Coroutine cancelled (user navigated back)")
            throw e
        } catch (e: Exception) {
            Log.e("ProfileFeedDebug", "Error fetching profile feed: ${e.message}", e)
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            Log.d("ProfileFeedDebug", "Setting isLoading = false in finally block")
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (postType == "media") "Posts" else "Threads") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = LightBrown,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (posts.isEmpty()) {
                Text(
                    text = "No ${if (postType == "media") "posts" else "threads"} found.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = posts,
                        key = { _, post -> post.id }
                    ) { _, post ->
                        PostCard(
                            postId = post.id,
                            authorId = post.userId,
                            authorName = authorProfile?.fullName ?: post.profile?.fullName ?: "Anonymous",
                            authorAvatar = authorProfile?.avatarUrl ?: post.profile?.avatarUrl,
                            content = post.content,
                            mediaUrl = post.mediaUrl,
                            postType = post.postType,
                            initialLikes = post.likes,
                            initialComments = post.comments,
                            currentUserId = currentUserId,
                            onProfileClick = onProfileClick
                        )
                    }
                }
            }
        }
    }
}