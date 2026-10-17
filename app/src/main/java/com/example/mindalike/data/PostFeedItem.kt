package com.example.mindalike.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostFeedItem(
    val id: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("post_type") val postType: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("profiles") val profile: Profile? = null,
    @SerialName("post_likes") val likes: List<PostLike> = emptyList(),
    @SerialName("post_comments") val comments: List<PostComment> = emptyList()
)
