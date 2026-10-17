package com.example.mindalike.data
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    @SerialName("id") val id: String? = null,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("type") val type: String, // like, comment, friend_request, friend_accept
    @SerialName("post_id") val postId: String? = null,
    @SerialName("is_read") val isRead: Boolean? = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
private data class AcceptParams(@SerialName("p_request_id") val requestId: String)