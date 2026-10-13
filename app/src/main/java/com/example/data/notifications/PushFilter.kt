package com.example.data.notifications

enum class PushKind { DIRECT_MESSAGE, CHALLENGE, FRIEND_REQUEST, OTHER }

/** Decides whether a push that arrived from the server is shown, according to Settings > Notifications. */
object PushFilter {

    fun classify(data: Map<String, String>): PushKind {
        val type = (data["type"] ?: data["notification_type"] ?: "").lowercase()
        return when {
            type == "direct_message" || type == "dm" || type == "message" || data.containsKey("pairId") -> PushKind.DIRECT_MESSAGE
            type == "challenge" || type == "new_challenge" || type == "health_challenge" || data.containsKey("challengeId") -> PushKind.CHALLENGE
            type == "friend_request" || type == "friend_request_accepted" || type == "friend" -> PushKind.FRIEND_REQUEST
            else -> PushKind.OTHER
        }
    }

    fun allows(kind: PushKind, prefs: NotificationPrefs, minuteNow: Int): Boolean {
        if (!prefs.masterEnabled) return false
        if (QuietHours.isQuiet(prefs, minuteNow)) return false
        return when (kind) {
            PushKind.DIRECT_MESSAGE -> prefs.socialMessages
            PushKind.CHALLENGE -> prefs.socialChallenges
            PushKind.FRIEND_REQUEST -> prefs.socialFriendRequests
            PushKind.OTHER -> true
        }
    }
}
