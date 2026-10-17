package com.example.mindalike.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostComment(
    @SerialName("id") val id: String? = null,
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("content") val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("profiles") val profile: Profile? = null
)