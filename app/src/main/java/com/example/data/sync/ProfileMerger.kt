package com.example.data.sync

import com.example.data.local.UserProfile

/**
 * Merges the phone's profile with the cloud's copy. The most recently saved profile wins as a whole,
 * except for the things two phones can legitimately both move forward: points (higher wins),
 * badges (union), the streak (the side with the later active day) and flags that only ever turn on.
 */
object ProfileMerger {

    fun merge(local: UserProfile, remote: UserProfile): UserProfile {
        val remoteNewer = remote.updatedAt > local.updatedAt
        val base = if (remoteNewer) remote else local
        val other = if (remoteNewer) local else remote

        // Streak and its day belong together: take them from the phone that was active most recently.
        val streakFromRemote = remote.lastActiveDate > local.lastActiveDate
        val streakFromLocal = local.lastActiveDate > remote.lastActiveDate
        val streak = when {
            streakFromRemote -> remote.currentStreak
            streakFromLocal -> local.currentStreak
            else -> maxOf(local.currentStreak, remote.currentStreak)
        }
        val lastActive = if (streakFromRemote) remote.lastActiveDate else local.lastActiveDate

        return base.copy(
            points = maxOf(local.points, remote.points),
            badges = unionBadges(base.badges, other.badges),
            currentStreak = streak,
            lastActiveDate = lastActive,
            onboardingCompleted = local.onboardingCompleted || remote.onboardingCompleted,
            hasAcceptedPrivacyPolicy = local.hasAcceptedPrivacyPolicy || remote.hasAcceptedPrivacyPolicy,
            friendCode = base.friendCode.ifBlank { other.friendCode },
            profilePictureUri = base.profilePictureUri ?: other.profilePictureUri,
            updatedAt = maxOf(local.updatedAt, remote.updatedAt)
        )
    }

    /** Comma separated badge ids: everything in [first], then whatever [second] adds. */
    fun unionBadges(first: String, second: String): String {
        val result = LinkedHashSet<String>()
        first.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { result.add(it) }
        second.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { result.add(it) }
        return result.joinToString(",")
    }
}
