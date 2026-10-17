package com.example.mindalike.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityMember(
    @SerialName("id") val id: String? = null,
    @SerialName("community_id") val communityId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("role") val role: String = "member", // "owner", "admin", or "member"
    @SerialName("joined_at") val joinedAt: String? = null
)