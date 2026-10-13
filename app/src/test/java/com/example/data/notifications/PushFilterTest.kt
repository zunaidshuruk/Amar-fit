package com.example.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PushFilterTest {

    @Test
    fun pushTypesAreRecognisedTheSameWayTheServerSendsThem() {
        assertEquals(PushKind.DIRECT_MESSAGE, PushFilter.classify(mapOf("type" to "direct_message")))
        assertEquals(PushKind.DIRECT_MESSAGE, PushFilter.classify(mapOf("type" to "dm")))
        assertEquals(PushKind.DIRECT_MESSAGE, PushFilter.classify(mapOf("pairId" to "a_b")))
        assertEquals(PushKind.CHALLENGE, PushFilter.classify(mapOf("type" to "challenge")))
        assertEquals(PushKind.CHALLENGE, PushFilter.classify(mapOf("challengeId" to "c1")))
        assertEquals(PushKind.FRIEND_REQUEST, PushFilter.classify(mapOf("type" to "friend_request")))
        assertEquals(PushKind.OTHER, PushFilter.classify(mapOf("type" to "announcement")))
        assertEquals(PushKind.OTHER, PushFilter.classify(emptyMap()))
    }

    @Test
    fun eachSocialSwitchControlsItsOwnKind() {
        val p = NotificationPrefs(socialMessages = false)
        assertFalse(PushFilter.allows(PushKind.DIRECT_MESSAGE, p, 600))
        assertTrue(PushFilter.allows(PushKind.CHALLENGE, p, 600))
        assertTrue(PushFilter.allows(PushKind.FRIEND_REQUEST, p, 600))
        val noChallenges = NotificationPrefs(socialChallenges = false)
        assertFalse(PushFilter.allows(PushKind.CHALLENGE, noChallenges, 600))
        val noFriends = NotificationPrefs(socialFriendRequests = false)
        assertFalse(PushFilter.allows(PushKind.FRIEND_REQUEST, noFriends, 600))
    }

    @Test
    fun masterSwitchAndQuietHoursStopEveryPush() {
        assertFalse(PushFilter.allows(PushKind.DIRECT_MESSAGE, NotificationPrefs(masterEnabled = false), 600))
        assertFalse(PushFilter.allows(PushKind.OTHER, NotificationPrefs(masterEnabled = false), 600))
        val quiet = NotificationPrefs(quietStartMinute = 22 * 60, quietEndMinute = 7 * 60)
        assertFalse(PushFilter.allows(PushKind.DIRECT_MESSAGE, quiet, 23 * 60))
        assertTrue(PushFilter.allows(PushKind.DIRECT_MESSAGE, quiet, 9 * 60))
    }

    @Test
    fun unclassifiedPushesAreShownWhenNotifictionsAreOn() {
        assertTrue(PushFilter.allows(PushKind.OTHER, NotificationPrefs(), 600))
    }
}
