package com.example.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionReconcilerTest {

    @Test
    fun itemDeletedOnAnotherPhoneIsRemovedHere() {
        val plan = CollectionReconciler.plan(setOf("a", "b"), setOf("a"), setOf("b"))
        assertEquals(listOf("b"), plan.deleteLocalIds)
        assertTrue(plan.pushIds.isEmpty())
    }

    @Test
    fun itemOnlyOnThisPhoneIsUploaded() {
        val plan = CollectionReconciler.plan(setOf("a", "new"), setOf("a"), emptySet())
        assertEquals(listOf("new"), plan.pushIds)
        assertTrue(plan.deleteLocalIds.isEmpty())
    }

    @Test
    fun itemsInTheCloudAreListedForUpsert() {
        val plan = CollectionReconciler.plan(setOf("a"), setOf("a", "c"), emptySet())
        assertEquals(listOf("a", "c"), plan.upsertIds)
    }

    @Test
    fun aDeletedItemThatIsNotHereIsIgnored() {
        val plan = CollectionReconciler.plan(emptySet(), emptySet(), setOf("x"))
        assertTrue(plan.deleteLocalIds.isEmpty())
        assertTrue(plan.pushIds.isEmpty())
    }

    @Test
    fun aTombstonedItemIsNeverUploadedAgain() {
        val plan = CollectionReconciler.plan(setOf("gone"), emptySet(), setOf("gone"))
        assertTrue(plan.pushIds.isEmpty())
        assertEquals(listOf("gone"), plan.deleteLocalIds)
    }

    @Test
    fun tombstonesExpireAfterThirtyDays() {
        val day = 24L * 60L * 60L * 1000L
        val now = 100L * day
        assertFalse(CollectionReconciler.isTombstoneExpired(now - 29 * day, now))
        assertTrue(CollectionReconciler.isTombstoneExpired(now - 31 * day, now))
        assertFalse(CollectionReconciler.isTombstoneExpired(0L, now))
    }
}
