package com.example.data.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialPrivacyTest {

    @Test
    fun aProfileWithoutTheFlagIsVisibleAndOnlyFalseHides() {
        assertTrue(SocialPrivacy.isVisibleOnLeaderboard(null))
        assertTrue(SocialPrivacy.isVisibleOnLeaderboard(true))
        assertFalse(SocialPrivacy.isVisibleOnLeaderboard(false))
    }

    @Test
    fun hiddenPeopleAreRemovedBeforeTheListIsCut() {
        val people = listOf("a" to true, "b" to false, "c" to true, "d" to false, "e" to true, "f" to true)
        assertEquals(listOf("a", "c", "e"), SocialPrivacy.visibleTop(people, 3) { it.second }.map { it.first })
        assertEquals(listOf("a", "c"), SocialPrivacy.visibleTop(people, 2) { it.second }.map { it.first })
        assertEquals(4, SocialPrivacy.visibleTop(people, 10) { it.second }.size)
    }

    @Test
    fun theQueryReadsMoreThanItShows() {
        assertEquals(100L, SocialPrivacy.queryLimit(50))
    }

    @Test
    fun switchingProgressSharingOffRemovesItAndOnPublishesAgain() {
        assertEquals(SocialPrivacy.ProgressChange.REMOVE, SocialPrivacy.progressChange(before = true, after = false))
        assertEquals(SocialPrivacy.ProgressChange.PUBLISH, SocialPrivacy.progressChange(before = false, after = true))
        assertEquals(SocialPrivacy.ProgressChange.NONE, SocialPrivacy.progressChange(before = true, after = true))
        assertEquals(SocialPrivacy.ProgressChange.NONE, SocialPrivacy.progressChange(before = false, after = false))
        assertEquals("a first save never deletes or publishes by itself", SocialPrivacy.ProgressChange.NONE, SocialPrivacy.progressChange(before = null, after = false))
    }

    @Test
    fun progressIsOnlyWrittenWhileSharingIsOn() {
        assertTrue(SocialPrivacy.mayShareProgress(true))
        assertFalse(SocialPrivacy.mayShareProgress(false))
    }
}
