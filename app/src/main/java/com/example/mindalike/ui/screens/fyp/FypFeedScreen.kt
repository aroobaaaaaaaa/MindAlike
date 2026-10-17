package com.example.mindalike.ui.screens.fyp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

private val LightBrown = Color(0xFF8C6D58)
private val LightBrownBorder = Color(0xFFE2D7CE)
private val CleanBackground = Color(0xFFFBF8F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FypFeedScreen(
    initialPostId: String,
    viewModel: FypViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onProfileClick: (userId: String) -> Unit = {}
) {
    val postsState by viewModel.postsState.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore Feed", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = CleanBackground
    ) { innerPadding ->
        when (val state = postsState) {
            is FypUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LightBrown)
                }
            }
            is FypUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = Color.Red, fontSize = 14.sp)
                }
            }
            is FypUiState.Success -> {
                val posts = state.data
                val initialIndex = remember(posts) {
                    posts.indexOfFirst { it.post.id == initialPostId }.coerceAtLeast(0)
                }

                LaunchedEffect(initialIndex) {
                    if (posts.isNotEmpty()) {
                        listState.scrollToItem(initialIndex)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(posts) { _, item ->
                        FypFeedPostCard(
                            item = item,
                            onProfileClick = onProfileClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FypFeedPostCard(
    item: PostWithAuthor,
    onProfileClick: (userId: String) -> Unit
) {
    val post = item.post
    val author = item.author

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { author?.id?.let { onProfileClick(it) } }
            ) {
                AsyncImage(
                    model = author?.avatarUrl ?: "https://via.placeholder.com/150",
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(LightBrownBorder),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = author?.fullName ?: "Anonymous User",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (!author?.universityName.isNullOrBlank()) {
                        Text(
                            text = author!!.universityName!!,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    fontSize = 13.sp,
                    color = Color(0xFF2C2C2C)
                )
            }

            if (!post.mediaUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Engagement Metrics Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Likes",
                        tint = LightBrown,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.likeCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = LightBrown,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.commentCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}