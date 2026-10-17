package com.example.mindalike.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("community_id") val communityId: String? = null,
    @SerialName("content") val content: String,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("post_type") val postType: String = "media", // "media" for grid, "thread" for text feed
    @SerialName("created_at") val createdAt: String? = null
)