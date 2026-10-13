package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalContentTest {

    @Test
    fun nobodyIsAskedAgainOnceTheyAcceptedTheCurrentVersion() {
        assertFalse(LegalContent.needsConsent(hasAccepted = true, acceptedVersion = LegalContent.POLICY_VERSION))
        assertFalse(LegalContent.needsConsent(hasAccepted = true, acceptedVersion = LegalContent.POLICY_VERSION + 1))
    }

    @Test
    fun everyoneWhoAcceptedAnOlderVersionIsAskedOnce() {
        // 0 = accepted before versions existed (every existing user after the database upgrade)
        assertTrue(LegalContent.needsConsent(hasAccepted = true, acceptedVersion = 0))
        assertTrue(LegalContent.needsConsent(hasAccepted = true, acceptedVersion = LegalContent.POLICY_VERSION - 1))
    }

    @Test
    fun aUserWhoNeverAcceptedIsAlwaysAsked() {
        assertTrue(LegalContent.needsConsent(hasAccepted = false, acceptedVersion = 0))
        assertTrue(LegalContent.needsConsent(hasAccepted = false, acceptedVersion = LegalContent.POLICY_VERSION))
    }

    @Test
    fun theVersionWasRaisedForTheOctoberUpdate() {
        assertEquals(2, LegalContent.POLICY_VERSION)
        assertTrue(LegalContent.PRIVACY_POLICY.contains("**Last updated:** October 13, 2026"))
    }

    @Test
    fun theTermsKeepTheirOwnDate() {
        assertTrue(LegalContent.TERMS_OF_SERVICE.contains("**Last updated:** September 26, 2026"))
    }

    @Test
    fun thePolicyDescribesWhatTheAppNowDoes() {
        val p = LegalContent.PRIVACY_POLICY
        assertTrue("sync across phones", p.contains("Sync across your phones"))
        assertTrue("assistant chats in the cloud", p.contains("AI Assistant conversations"))
        assertTrue("deleted-item marker", p.contains("about 30 days"))
        assertTrue("notification settings", p.contains("Settings > Notifications"))
        assertTrue("what account deletion removes", p.contains("Deleting your account"))
        assertTrue("what can remain after deletion", p.contains("challenge and kudos records"))
        assertTrue("drive backup is not touched", p.contains("not touched"))
    }

    @Test
    fun thePolicyDoesNotMakeAClaimTheAppCannotKeep() {
        val p = LegalContent.PRIVACY_POLICY.lowercase()
        assertFalse("must not claim encryption", p.contains("encrypt"))
        assertFalse("old over-claim about deletion", p.contains("removes your data from our systems"))
    }
}
