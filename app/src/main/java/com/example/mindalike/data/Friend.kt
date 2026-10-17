package com.example.mindalike.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Friend(
    @SerialName("id") val id: String? = null,
    @SerialName("user_1") val user1: String,
    @SerialName("user_2") val user2: String,
    @SerialName("created_at") val createdAt: String? = null
)