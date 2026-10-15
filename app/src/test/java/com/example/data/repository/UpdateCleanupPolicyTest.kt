package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCleanupPolicyTest {
    private val hour = 60L * 60L * 1000L
    private val now = 1_000_000_000_000L

    @Test
    fun withNothingBeingDownloadedEverythingGoes() {
        assertTrue(UpdateCleanupPolicy.shouldDelete(33, 0, 0, now))
    }

    @Test
    fun onceTheUpdateIsInstalledItsFileGoes() {
        assertTrue(UpdateCleanupPolicy.shouldDelete(34, 34, now - hour, now))   // exactly the downloaded version
        assertTrue(UpdateCleanupPolicy.shouldDelete(35, 34, now - hour, now))   // a newer one was installed some other way
    }

    @Test
    fun aDownloadedButNotInstalledUpdateIsKept() {
        assertFalse(UpdateCleanupPolicy.shouldDelete(33, 34, now - hour, now))
        assertFalse(UpdateCleanupPolicy.shouldDelete(33, 34, now - 23 * hour, now))
    }

    @Test
    fun anAbandonedDownloadIsDroppedAfterADay() {
        assertFalse(UpdateCleanupPolicy.shouldDelete(33, 34, now - 24 * hour, now))
        assertTrue(UpdateCleanupPolicy.shouldDelete(33, 34, now - 24 * hour - 1, now))
        assertTrue(UpdateCleanupPolicy.shouldDelete(33, 34, now - 48 * hour, now))
    }

    @Test
    fun updateFilesAreRecognisedByName() {
        assertTrue(UpdateCleanupPolicy.isUpdateFile("amar_fit_update.apk"))
        assertTrue(UpdateCleanupPolicy.isUpdateFile("KardIQ-3.2.APK"))
        assertTrue(UpdateCleanupPolicy.isUpdateFile("app-debug.apk"))
        assertTrue(UpdateCleanupPolicy.isUpdateFile("amar_fit_update.apk.part"))
        assertEquals(false, UpdateCleanupPolicy.isUpdateFile("glucose_template.csv"))
        assertEquals(false, UpdateCleanupPolicy.isUpdateFile("model.gguf"))
        assertEquals(false, UpdateCleanupPolicy.isUpdateFile("backup.json"))
    }
}
