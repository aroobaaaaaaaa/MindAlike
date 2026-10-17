package com.example.mindalike.data

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "FriendUtils"

enum class FriendshipStatus {
    NONE,
    PENDING_SENT,
    PENDING_RECEIVED,
    FRIENDS
}

/** Returns the relationship between the current user and another user. */
suspend fun getFriendshipStatus(currentUserId: String, otherUserId: String): FriendshipStatus {
    return try {
        // Already friends? (the pair can be stored in either order)
        val friendRows = supabase.from("friends")
            .select {
                filter {
                    or {
                        and {
                            eq("user_1", currentUserId)
                            eq("user_2", otherUserId)
                        }
                        and {
                            eq("user_1", otherUserId)
                            eq("user_2", currentUserId)
                        }
                    }
                }
            }.decodeList<Friend>()
        if (friendRows.isNotEmpty()) return FriendshipStatus.FRIENDS

        // Pending request in either direction?
        val requests = supabase.from("friend_requests")
            .select {
                filter {
                    eq("status", "pending")
                    or {
                        and {
                            eq("sender_id", currentUserId)
                            eq("receiver_id", otherUserId)
                        }
                        and {
                            eq("sender_id", otherUserId)
                            eq("receiver_id", currentUserId)
                        }
                    }
                }
            }.decodeList<FriendRequest>()

        val req = requests.firstOrNull()
        when {
            req == null -> FriendshipStatus.NONE
            req.senderId == currentUserId -> FriendshipStatus.PENDING_SENT
            else -> FriendshipStatus.PENDING_RECEIVED
        }
    } catch (e: Exception) {
        Log.e(TAG, "getFriendshipStatus failed", e)
        FriendshipStatus.NONE
    }
}

/** Sends a friend request. Returns true on success. */
suspend fun sendFriendRequest(senderId: String, receiverId: String): Boolean {
    return try {
        // Clear any old (e.g. rejected) row from me to them, because (sender, receiver) is unique
        supabase.from("friend_requests").delete {
            filter {
                eq("sender_id", senderId)
                eq("receiver_id", receiverId)
            }
        }
        supabase.from("friend_requests").insert(
            FriendRequest(senderId = senderId, receiverId = receiverId)
        )
        true
    } catch (e: Exception) {
        Log.e(TAG, "sendFriendRequest failed", e)
        false
    }
}

/** Cancels a request I sent. Returns true on success. */
suspend fun cancelFriendRequest(senderId: String, receiverId: String): Boolean {
    return try {
        supabase.from("friend_requests").delete {
            filter {
                eq("sender_id", senderId)
                eq("receiver_id", receiverId)
                eq("status", "pending")
            }
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "cancelFriendRequest failed", e)
        false
    }
}

/**
 * Accepts a request: creates the friendship row first, then marks the request accepted.
 * Returns true on success. Check Logcat for "FriendUtils" if it returns false.
 */
suspend fun acceptFriendRequest(requestId: String, senderId: String, receiverId: String): Boolean {
    return try {
        supabase.postgrest.rpc(
            "accept_friend_request",
            buildJsonObject { put("p_request_id", requestId) }
        )
        true
    } catch (e: Exception) {
        Log.e(TAG, "acceptFriendRequest failed", e)
        false
    }
}
/** Declines a request. Returns true on success. */
suspend fun declineFriendRequest(requestId: String): Boolean {
    return try {
        supabase.from("friend_requests").update({
            set("status", "rejected")
        }) {
            filter { eq("id", requestId) }
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "declineFriendRequest failed", e)
        false
    }
}