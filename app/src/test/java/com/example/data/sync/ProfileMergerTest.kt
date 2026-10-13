package com.example.data.sync

import com.example.data.local.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileMergerTest {

    private fun p(
        name: String = "A",
        points: Int = 0,
        badges: String = "",
        streak: Int = 0,
        lastActive: String = "",
        updatedAt: Long = 0L,
        friendCode: String = "",
        picture: String? = null
    ) = UserProfile(
        name = name, points = points, badges = badges, currentStreak = streak,
        lastActiveDate = lastActive, updatedAt = updatedAt, friendCode = friendCode, profilePictureUri = picture
    )

    @Test
    fun newerSaveWinsAsAWhole() {
        val merged = ProfileMerger.merge(p(name = "Old", updatedAt = 1), p(name = "New", updatedAt = 2))
        assertEquals("New", merged.name)
        val merged2 = ProfileMerger.merge(p(name = "Local", updatedAt = 9), p(name = "Remote", updatedAt = 3))
        assertEquals("Local", merged2.name)
    }

    @Test
    fun pointsTakeTheHigherValueAndBadgesAreUnited() {
        val merged = ProfileMerger.merge(
            p(points = 120, badges = "Hydration Hero", updatedAt = 5),
            p(points = 200, badges = "10k Steps Master,Hydration Hero", updatedAt = 4)
        )
        assertEquals(200, merged.points)
        assertEquals("Hydration Hero,10k Steps Master", merged.badges)
    }

    @Test
    fun streakComesFromTheMostRecentlyActivePhone() {
        val merged = ProfileMerger.merge(
            p(streak = 9, lastActive = "2026-10-10", updatedAt = 9),
            p(streak = 2, lastActive = "2026-10-12", updatedAt = 1)
        )
        assertEquals(2, merged.currentStreak)
        assertEquals("2026-10-12", merged.lastActiveDate)
    }

    @Test
    fun sameDayStreakTakesTheHigherOne() {
        val merged = ProfileMerger.merge(p(streak = 3, lastActive = "2026-10-12"), p(streak = 5, lastActive = "2026-10-12"))
        assertEquals(5, merged.currentStreak)
    }

    @Test
    fun missingFriendCodeAndPictureAreFilledFromTheOtherSide() {
        val merged = ProfileMerger.merge(p(updatedAt = 9), p(friendCode = "ABC123", picture = "data:x", updatedAt = 1))
        assertEquals("ABC123", merged.friendCode)
        assertEquals("data:x", merged.profilePictureUri)
    }

    @Test
    fun updatedAtIsTheMaximumAndMergingTwiceChangesNothing() {
        val a = p(name = "A", points = 10, badges = "x,y", updatedAt = 7, streak = 1, lastActive = "2026-10-11")
        val b = p(name = "B", points = 20, badges = "y,z", updatedAt = 3, streak = 4, lastActive = "2026-10-12")
        val merged = ProfileMerger.merge(a, b)
        assertEquals(7L, merged.updatedAt)
        assertEquals(merged, ProfileMerger.merge(merged, b))
        assertEquals(merged, ProfileMerger.merge(merged, merged))
    }
}
