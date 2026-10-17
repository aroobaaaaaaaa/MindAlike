package com.example.mindalike.data

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

private const val CHAT_TAG = "ChatUtils"

// ---------------- Communities ----------------

suspend fun fetchAllCommunities(): List<Community> = try {
    supabase.from("communities")
        .select { order("member_count", Order.DESCENDING) }
        .decodeList<Community>()
} catch (e: Exception) {
    Log.e(CHAT_TAG, "fetchAllCommunities failed", e)
    emptyList()
}

suspend fun fetchCommunity(communityId: String): Community? = try {
    supabase.from("communities")
        .select { filter { eq("id", communityId) } }
        .decodeSingleOrNull<Community>()
} catch (e: Exception) {
    Log.e(CHAT_TAG, "fetchCommunity failed", e)
    null
}

suspend fun fetchJoinedCommunityIds(userId: String): Set<String> = try {
    supabase.from("community_members")
        .select { filter { eq("user_id", userId) } }
        .decodeList<CommunityMember>()
        .map { it.communityId }
        .toSet()
} catch (e: Exception) {
    Log.e(CHAT_TAG, "fetchJoinedCommunityIds failed", e)
    emptySet()
}

suspend fun joinCommunity(communityId: String, userId: String): Boolean = try {
    val existing = supabase.from("community_members")
        .select {
            filter {
                eq("community_id", communityId)
                eq("user_id", userId)
            }
        }.decodeList<CommunityMember>()
    if (existing.isEmpty()) {
        supabase.from("community_members").insert(
            CommunityMember(communityId = communityId, userId = userId)
        )
    }
    true
} catch (e: Exception) {
    Log.e(CHAT_TAG, "joinCommunity failed", e)
    false
}

suspend fun leaveCommunity(communityId: String, userId: String): Boolean = try {
    supabase.from("community_members").delete {
        filter {
            eq("community_id", communityId)
            eq("user_id", userId)
        }
    }
    true
} catch (e: Exception) {
    Log.e(CHAT_TAG, "leaveCommunity failed", e)
    false
}

// ---------------- Messages ----------------

suspend fun fetchCommunityMessages(communityId: String): List<Message> = try {
    supabase.from("messages")
        .select {
            filter { eq("community_id", communityId) }
            order("created_at", Order.DESCENDING)
            limit(150)
        }.decodeList<Message>().reversed()
} catch (e: Exception) {
    Log.e(CHAT_TAG, "fetchCommunityMessages failed", e)
    emptyList()
}

suspend fun fetchDirectMessages(me: String, other: String): List<Message> = try {
    supabase.from("messages")
        .select {
            filter {
                or {
                    and {
                        eq("sender_id", me)
                        eq("receiver_id", other)
                    }
                    and {
                        eq("sender_id", other)
                        eq("receiver_id", me)
                    }
                }
            }
            order("created_at", Order.DESCENDING)
            limit(200)
        }.decodeList<Message>().reversed()
} catch (e: Exception) {
    Log.e(CHAT_TAG, "fetchDirectMessages failed", e)
    emptyList()
}

/** Newest first. Only 1-on-1 messages involving me. */
suspend fun fetchRecentDirectMessages(me: String): List<Message> = try {
    supabase.from("messages")
        .select {
            filter {
                or {
                    eq("sender_id", me)
                    eq("receiver_id", me)
                }
            }
            order("created_at", Order.DESCENDING)
            limit(300)
        }.decodeList<Message>().filter { it.receiverId != null }
} catch (e: Exception) {
    Log.e(CHAT_TAG, "fetchRecentDirectMessages failed", e)
    emptyList()
}

suspend fun sendMessage(message: Message): Boolean = try {
    supabase.from("messages").insert(message)
    true
} catch (e: Exception) {
    Log.e(CHAT_TAG, "sendMessage failed", e)
    false
}

// ---------------- Profiles ----------------

suspend fun fetchProfilesByIds(ids: Collection<String>): Map<String, Profile> {
    if (ids.isEmpty()) return emptyMap()
    return try {
        supabase.from("profiles")
            .select { filter { isIn("id", ids.toList()) } }
            .decodeList<Profile>()
            .associateBy { it.id }
    } catch (e: Exception) {
        Log.e(CHAT_TAG, "fetchProfilesByIds failed", e)
        emptyMap()
    }
}

suspend fun fetchFriendProfiles(userId: String): List<Profile> = try {
    val f1 = supabase.from("friends")
        .select { filter { eq("user_1", userId) } }
        .decodeList<Friend>()
    val f2 = supabase.from("friends")
        .select { filter { eq("user_2", userId) } }
        .decodeList<Friend>()
    val ids = (f1.map { it.user2 } + f2.map { it.user1 }).distinct()
    if (ids.isEmpty()) emptyList()
    else supabase.from("profiles")
        .select { filter { isIn("id", ids) } }
        .decodeList<Profile>()
} catch (e: Exception) {
    Log.e(CHAT_TAG, "fetchFriendProfiles failed", e)
    emptyList()
}