package com.example.mindalike.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Main : Screen("main")
    object Notifications : Screen("notifications")
    object DirectMessages : Screen("direct_messages")
    object CreatePost : Screen("create_post/{postType}") {
        fun createRoute(postType: String) = "create_post/$postType"
    }
    object ProfileFeed : Screen("profile_feed/{userId}/{postId}/{postType}") {
        fun createRoute(userId: String, postId: String, postType: String) =
            "profile_feed/$userId/$postId/$postType"
    }
    object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
    object FypFeed : Screen("fyp_feed/{initialPostId}") {
        fun createRoute(initialPostId: String) = "fyp_feed/$initialPostId"
    }
}