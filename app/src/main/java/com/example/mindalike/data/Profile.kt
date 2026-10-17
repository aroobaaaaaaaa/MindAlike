package com.example.mindalike.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName(value = "id") val id: String,
    @SerialName(value = "full_name") val fullName: String,
    @SerialName(value = "university_email") val universityEmail: String,
    @SerialName(value = "university_name") val universityName: String? = null,
    @SerialName(value = "degree") val degree: String? = null,
    @SerialName(value = "location") val location: String? = null,
    @SerialName(value = "bio") val bio: String? = null,
    @SerialName(value = "avatar_url") val avatarUrl: String? = null,
    @SerialName(value = "badges") val badges: List<String> = emptyList(),
    @SerialName(value = "strike_count") val strikeCount: Int = 0,
    @SerialName(value = "is_private") val isPrivate: Boolean = false,
    @SerialName(value = "created_at") val createdAt: String? = null
)

// ✅ ADD THIS DATA CLASS FOR UPDATES
@Serializable
data class ProfileUpdate(
    @SerialName(value = "full_name") val fullName: String,
    @SerialName(value = "bio") val bio: String?,
    @SerialName(value = "degree") val degree: String?,
    @SerialName(value = "university_name") val universityName: String?,
    @SerialName(value = "location") val location: String?,
    @SerialName(value = "avatar_url") val avatarUrl: String?,
    @SerialName(value = "badges") val badges: List<String>
)