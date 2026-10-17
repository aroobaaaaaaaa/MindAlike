package com.example.mindalike.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mindalike.data.supabase
import com.example.mindalike.ui.components.LocalAppNavController
import com.example.mindalike.ui.screens.AuthScreen
import com.example.mindalike.ui.screens.CreatePostScreen
import com.example.mindalike.ui.screens.DirectMessagesScreen
import com.example.mindalike.ui.screens.DmChatRoute
import com.example.mindalike.ui.screens.MainScreen
import com.example.mindalike.ui.screens.NotificationsScreen
import com.example.mindalike.ui.screens.SplashScreen
import com.example.mindalike.ui.screens.community.CommunityChatScreen
import com.example.mindalike.ui.screens.fyp.FypFeedScreen
import com.example.mindalike.ui.screens.profile.ProfileFeedScreen
import com.example.mindalike.ui.screens.profile.ProfileScreen
import io.github.jan.supabase.auth.auth

@Composable
fun AppNavGraph(navController: NavHostController) {
    CompositionLocalProvider(LocalAppNavController provides navController) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Main.route) {
                val currentUserId = supabase.auth.currentUserOrNull()?.id ?: ""

                MainScreen(
                    currentUserId = currentUserId,
                    onSignOut = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onNavigateToDirectMessages = {
                        navController.navigate(Screen.DirectMessages.route)
                    },
                    onNavigateToAddPost = { postType ->
                        navController.navigate(Screen.CreatePost.createRoute(postType))
                    },
                    onNavigateToProfileFeed = { userId, postId, postType ->
                        navController.navigate(
                            Screen.ProfileFeed.createRoute(userId, postId, postType)
                        )
                    },
                    onNavigateToUserProfile = { userId ->
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    },
                    onNavigateToFypFeed = { initialPostId ->
                        navController.navigate(Screen.FypFeed.createRoute(initialPostId))
                    }
                )
            }

            composable(
                route = Screen.UserProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val currentUserId = supabase.auth.currentUserOrNull()?.id ?: ""

                ProfileScreen(
                    userId = userId,
                    isSelfProfile = userId == currentUserId,
                    onSignOut = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    },
                    onPostClick = { postId, postType ->
                        navController.navigate(
                            Screen.ProfileFeed.createRoute(userId, postId, postType)
                        )
                    },
                    onCommunityClick = { communityId ->
                        navController.navigate("community/$communityId")
                    },
                    onUserProfileClick = { otherId ->
                        navController.navigate(Screen.UserProfile.createRoute(otherId))
                    },
                    onMessageClick = { otherId ->
                        navController.navigate("chat/$otherId")
                    }
                )
            }

            // Direct chat with one user (opened from the profile Message button)
            composable(
                route = "chat/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val otherId = backStackEntry.arguments?.getString("userId") ?: ""
                DmChatRoute(
                    userId = otherId,
                    onBack = { navController.popBackStack() }
                )
            }

            // Community chat page (opened from a profile's hubs list)
            composable(
                route = "community/{communityId}",
                arguments = listOf(navArgument("communityId") { type = NavType.StringType })
            ) { backStackEntry ->
                val communityId = backStackEntry.arguments?.getString("communityId") ?: ""

                CommunityChatScreen(
                    communityId = communityId,
                    onBack = { navController.popBackStack() },
                    onUserProfileClick = { userId ->
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    }
                )
            }

            composable(
                route = Screen.FypFeed.route,
                arguments = listOf(navArgument("initialPostId") { type = NavType.StringType })
            ) { backStackEntry ->
                val initialPostId = backStackEntry.arguments?.getString("initialPostId") ?: ""

                FypFeedScreen(
                    initialPostId = initialPostId,
                    onBackClick = { navController.popBackStack() },
                    onProfileClick = { userId ->
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onUserProfileClick = { userId ->
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    },
                    onPostClick = { userId, postId, postType ->
                        navController.navigate(
                            Screen.ProfileFeed.createRoute(userId, postId, postType)
                        )
                    }
                )
            }

            composable(Screen.DirectMessages.route) {
                DirectMessagesScreen(
                    onBack = { navController.popBackStack() },
                    bottomBarPadding = androidx.compose.ui.unit.Dp(0f)
                )
            }

            composable(
                route = Screen.CreatePost.route,
                arguments = listOf(navArgument("postType") { type = NavType.StringType })
            ) { backStackEntry ->
                val postType = backStackEntry.arguments?.getString("postType") ?: "media"
                CreatePostScreen(
                    postType = postType,
                    onPostSuccess = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ProfileFeed.route,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("postId") { type = NavType.StringType },
                    navArgument("postType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                val postType = backStackEntry.arguments?.getString("postType") ?: "media"
                val currentUserId = supabase.auth.currentUserOrNull()?.id ?: ""

                ProfileFeedScreen(
                    userId = userId,
                    initialPostId = postId,
                    postType = postType,
                    currentUserId = currentUserId,
                    onBackClick = { navController.popBackStack() },
                    onProfileClick = { authorId ->
                        navController.navigate(Screen.UserProfile.createRoute(authorId))
                    }
                )
            }
        }
    }
}