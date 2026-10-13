package com.example.data.sync

/**
 * Stable cloud ids for items that only have a local auto number. The same message or event gets the same id
 * on every phone, so a second phone recognises what it already has and never duplicates it.
 */
object SyncIds {
    fun messageId(timestamp: Long, isUser: Boolean, text: String): String {
        return "${timestamp}_${if (isUser) "u" else "a"}_${text.hashCode()}"
    }

    fun activityEventId(timestamp: Long, type: String, description: String): String {
        return "${timestamp}_${type.hashCode()}_${description.hashCode()}"
    }
}
