package com.example.mindalike.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("id") val id: String? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String? = null, // For 1-on-1 DMs
    @SerialName("community_id") val communityId: String? = null, // For Community Group Chats
    @SerialName("content") val content: String,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
