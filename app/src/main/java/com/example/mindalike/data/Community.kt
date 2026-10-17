package com.example.mindalike.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Community(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("category") val category: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("creator_id") val creatorId: String? = null,
    @SerialName("member_count") val memberCount: Int = 1,
    @SerialName("created_at") val createdAt: String? = null
)