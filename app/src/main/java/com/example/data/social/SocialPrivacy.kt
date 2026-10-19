package com.example.data.social

/** The privacy switches of the social features (Settings): who may see you on the leaderboard and whether daily progress is shared with friends. */
object SocialPrivacy {
    /** A public profile without the flag (made before the switch existed) is visible. */
    fun isVisibleOnLeaderboard(flag: Boolean?): Boolean = flag != false

    /** The leaderboard is read with a larger limit so hidden people do not leave empty places; the shown list is cut to [limit] after hiding them. */
    fun queryLimit(limit: Long): Long = limit * 2

    fun <T> visibleTop(items: List<T>, limit: Int, isVisible: (T) -> Boolean): List<T> = items.filter(isVisible).take(limit)

    /** Whether today's progress may be written to the friends-only document. */
    fun mayShareProgress(shareProgressWithFriends: Boolean): Boolean = shareProgressWithFriends

    /** What has to happen to the shared progress when the profile is saved: switched off removes it at once, switched on publishes it again. */
    enum class ProgressChange { NONE, REMOVE, PUBLISH }

    fun progressChange(before: Boolean?, after: Boolean): ProgressChange = when {
        before == null || before == after -> ProgressChange.NONE
        after -> ProgressChange.PUBLISH
        else -> ProgressChange.REMOVE
    }
}
