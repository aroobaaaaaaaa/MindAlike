package com.example.mindalike.data
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModerationLog(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("flagged_content") val flaggedContent: String,
    @SerialName("flag_reason") val flagReason: String? = null,
    @SerialName("ai_confidence_score") val aiConfidenceScore: Double? = null,
    @SerialName("created_at") val createdAt: String? = null
)