package com.example.mindalike.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mindalike.data.PostComment
import com.example.mindalike.data.PostLike
import com.example.mindalike.data.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val PrimaryColor = Color(0xFF8C6D58)
private val LightGrayBg = Color(0xFFF7F7F8)

@Serializable
private data class InsertLikePayload(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
private data class InsertCommentPayload(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    val content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCard(
    postId: String,
    authorId: String,
    authorName: String,
    authorAvatar: String?,
    content: String,
    mediaUrl: String?,
    postType: String,
    initialLikes: List<PostLike>,
    initialComments: List<PostComment>,
    currentUserId: String,
    onPostDeleted: () -> Unit = {},
    onProfileClick: (userId: String) -> Unit = {},
    onPostClick: (postId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var likesList by remember { mutableStateOf(initialLikes) }
    var isLiked by remember { mutableStateOf(initialLikes.any { it.userId == currentUserId }) }
    var commentsList by remember { mutableStateOf(initialComments) }

    var showMenu by remember { mutableStateOf(false) }
    var showLikesSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var isSubmittingComment by remember { mutableStateOf(false) }

    fun toggleLike() {
        val previousState = isLiked
        val previousLikes = likesList

        isLiked = !previousState
        likesList = if (isLiked) {
            likesList + PostLike(postId = postId, userId = currentUserId)
        } else {
            likesList.filter { it.userId != currentUserId }
        }

        scope.launch {
            try {
                if (previousState) {
                    supabase.from("post_likes").delete {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", currentUserId)
                        }
                    }
                } else {
                    supabase.from("post_likes").insert(
                        InsertLikePayload(postId = postId, userId = currentUserId)
                    )
                }
            } catch (e: Exception) {
                isLiked = previousState
                likesList = previousLikes
                Toast.makeText(context, "Failed to update like: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun submitComment() {
        if (newCommentText.isBlank()) return
        val commentContent = newCommentText.trim()
        newCommentText = ""

        scope.launch {
            isSubmittingComment = true
            try {
                supabase.from("post_comments").insert(
                    InsertCommentPayload(postId = postId, userId = currentUserId, content = commentContent)
                )

                val updatedComments = supabase.from("post_comments")
                    .select(columns = Columns.raw("*, profiles(*)")) {
                        filter { eq("post_id", postId) }
                    }
                    .decodeList<PostComment>()
                commentsList = updatedComments
            } catch (e: Exception) {
                Toast.makeText(context, "Error adding comment: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isSubmittingComment = false
            }
        }
    }

    fun deletePost() {
        scope.launch {
            try {
                supabase.from("posts").delete {
                    filter { eq("id", postId) }
                }
                Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                onPostDeleted()
            } catch (e: Exception) {
                Toast.makeText(context, "Delete failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onProfileClick(authorId) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val authorPic = authorAvatar?.takeIf { it.isNotBlank() }
                    if (authorPic != null) {
                        AsyncImage(
                            model = authorPic,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LightGrayBg),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = authorName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1C1B1F)
                        )
                        Text(
                            text = if (postType == "media") "Media Post" else "Thread",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                if (authorId == currentUserId) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    deletePost()
                                }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPostClick(postId) }
            ) {
                if (content.isNotBlank()) {
                    Text(
                        text = content,
                        fontSize = 14.sp,
                        color = Color(0xFF2C2C2C),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                if (postType == "media" && !mediaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .background(LightGrayBg),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { toggleLike() }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else Color.DarkGray
                    )
                }
                Text(
                    text = "${likesList.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { showLikesSheet = true }
                        .padding(end = 16.dp)
                )

                IconButton(onClick = { showCommentsSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = Color.DarkGray
                    )
                }
                Text(
                    text = "${commentsList.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { showCommentsSheet = true }
                )
            }

            HorizontalDivider(color = LightGrayBg, thickness = 1.dp)
        }
    }

    if (showLikesSheet) {
        ModalBottomSheet(onDismissRequest = { showLikesSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Liked by", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                if (likesList.isEmpty()) {
                    Text("No likes yet.", color = Color.Gray, fontSize = 14.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(likesList) { like ->
                            val avatarUrl = like.profile?.avatarUrl
                            val displayName = like.profile?.fullName ?: "User"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showLikesSheet = false
                                        onProfileClick(like.userId)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(LightGrayBg),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = displayName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = displayName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCommentsSheet) {
        ModalBottomSheet(onDismissRequest = { showCommentsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("Comments", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (commentsList.isEmpty()) {
                        item {
                            Text("No comments yet. Be the first to comment!", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        items(commentsList) { comment ->
                            val avatarUrl = comment.profile?.avatarUrl
                            val displayName = comment.profile?.fullName ?: "User"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier.clickable {
                                        showCommentsSheet = false
                                        onProfileClick(comment.userId)
                                    }
                                ) {
                                    if (!avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(LightGrayBg),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = displayName.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(LightGrayBg, RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.clickable {
                                            showCommentsSheet = false
                                            onProfileClick(comment.userId)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = comment.content, fontSize = 13.sp, color = Color(0xFF2C2C2C))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Add a comment...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { submitComment() },
                        enabled = !isSubmittingComment && newCommentText.isNotBlank()
                    ) {
                        if (isSubmittingComment) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryColor)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = PrimaryColor)
                        }
                    }
                }
            }
        }
    }
}