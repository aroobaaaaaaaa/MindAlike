package com.example.mindalike.data

import android.util.Log
import io.github.jan.supabase.postgrest.from

private const val TAG = "MessageActions"

// Change these if your table / column names differ (check your Message @SerialName values).
private const val TABLE = "messages"
private const val COL_ID = "id"
private const val COL_SENDER = "sender_id"
private const val COL_RECEIVER = "receiver_id"

/**
 * Deletes a single message by id (works for DMs and community messages).
 * RLS only allows this if you are the sender. Returns true if the call succeeded.
 */
suspend fun deleteMessage(id: Any): Boolean = try {
    supabase.from(TABLE).delete {
        filter { eq(COL_ID, id) }
    }
    true
} catch (e: Exception) {
    Log.e(TAG, "deleteMessage failed", e)
    false
}

/**
 * Deletes every direct message between [me] and [other], in both directions.
 * NOTE: this removes the conversation for BOTH people.
 */
suspend fun deleteDirectChat(me: String, other: String): Boolean = try {
    supabase.from(TABLE).delete {
        filter {
            or {
                and {
                    eq(COL_SENDER, me)
                    eq(COL_RECEIVER, other)
                }
                and {
                    eq(COL_SENDER, other)
                    eq(COL_RECEIVER, me)
                }
            }
        }
    }
    true
} catch (e: Exception) {
    Log.e(TAG, "deleteDirectChat failed", e)
    false
}