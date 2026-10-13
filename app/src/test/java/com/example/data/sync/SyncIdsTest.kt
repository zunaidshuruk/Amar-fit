package com.example.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SyncIdsTest {

    @Test
    fun sameMessageGetsTheSameIdEverywhere() {
        assertEquals(SyncIds.messageId(1000L, true, "hello"), SyncIds.messageId(1000L, true, "hello"))
    }

    @Test
    fun differentMessagesGetDifferentIds() {
        val base = SyncIds.messageId(1000L, true, "hello")
        assertNotEquals(base, SyncIds.messageId(1001L, true, "hello"))
        assertNotEquals(base, SyncIds.messageId(1000L, false, "hello"))
        assertNotEquals(base, SyncIds.messageId(1000L, true, "hello!"))
    }

    @Test
    fun idsAreSafeAsDocumentNames() {
        val id = SyncIds.messageId(1000L, false, "some text / with slash")
        assertEquals(false, id.contains("/"))
        val eid = SyncIds.activityEventId(5L, "workout", "Completed workout: Legs / Core")
        assertEquals(false, eid.contains("/"))
    }

    @Test
    fun activityEventIdsAreStableAndDistinct() {
        val a = SyncIds.activityEventId(5L, "workout", "Completed workout: Legs")
        assertEquals(a, SyncIds.activityEventId(5L, "workout", "Completed workout: Legs"))
        assertNotEquals(a, SyncIds.activityEventId(5L, "water", "Completed workout: Legs"))
    }
}
