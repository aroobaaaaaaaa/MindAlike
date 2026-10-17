package com.example.mindalike.data
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(
    @SerialName("id") val id: String? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("status") val status: String = "pending", // "pending", "accepted", or "rejected"
    @SerialName("created_at") val createdAt: String? = null
)