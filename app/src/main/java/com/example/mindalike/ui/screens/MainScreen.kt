package com.example.mindalike.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.mindalike.ui.screens.fyp.FypScreen
import com.example.mindalike.ui.screens.home.HomeScreen
import com.example.mindalike.ui.screens.profile.ProfileScreen

@Composable
fun MainScreen(
    currentUserId: String,
    onSignOut: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToDirectMessages: () -> Unit = {},
    onNavigateToAddPost: (postType: String) -> Unit = {},
    onNavigateToProfileFeed: (userId: String, postId: String, postType: String) -> Unit = { _, _, _ -> },
    onNavigateToUserProfile: (userId: String) -> Unit = {},
    onNavigateToFypFeed: (initialPostId: String) -> Unit = {}
) {
    // Uses rememberSaveable to maintain selected tab across navigation backstack
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> HomeScreen(
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToDirectMessages = { selectedTab = 2 },
                    onNavigateToAddPost = onNavigateToAddPost,
                    onNavigateToProfile = { authorId ->
                        if (authorId == currentUserId) {
                            selectedTab = 3
                        } else {
                            onNavigateToUserProfile(authorId)
                        }
                    }
                )
                1 -> FypScreen(
                    onNavigateToUserProfile = onNavigateToUserProfile,
                    onNavigateToFypFeed = onNavigateToFypFeed
                )
                2 -> DirectMessagesScreen(
                    onBack = { selectedTab = 0 }
                )
                3 -> ProfileScreen(
                    userId = currentUserId,
                    isSelfProfile = true,
                    onSignOut = onSignOut,
                    onPostClick = { postId, postType ->
                        onNavigateToProfileFeed(currentUserId, postId, postType)
                    }
                )
            }
        }

        SleekFloatingBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SleekFloatingBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(0, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem(1, "FYP", Icons.Filled.Explore, Icons.Outlined.Explore),
        NavItem(2, "DMs", Icons.Filled.Chat, Icons.Outlined.Chat),
        NavItem(3, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(27.dp)
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = selectedTab == item.index

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    label = "iconColor"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    label = "iconScale"
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(item.index) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(iconScale)
                    )
                }
            }
        }
    }
}

private data class NavItem(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)