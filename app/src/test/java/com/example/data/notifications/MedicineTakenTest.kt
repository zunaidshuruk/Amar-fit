package com.example.data.notifications

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicineTakenTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun markingADoseTakenIsRememberedForThatDayOnly() {
        val set = MedicineTaken.add(emptySet(), d("2026-10-14"), "a1")
        assertTrue(MedicineTaken.contains(set, d("2026-10-14"), "a1"))
        assertFalse(MedicineTaken.contains(set, d("2026-10-15"), "a1"))
        assertFalse(MedicineTaken.contains(set, d("2026-10-14"), "b2"))
    }

    @Test
    fun theTakenIdsOfADayAreListed() {
        var set = MedicineTaken.add(emptySet(), d("2026-10-14"), "a1")
        set = MedicineTaken.add(set, d("2026-10-14"), "b2")
        set = MedicineTaken.add(set, d("2026-10-13"), "c3")
        assertEquals(setOf("a1", "b2"), MedicineTaken.takenOn(set, d("2026-10-14")))
        assertEquals(setOf("c3"), MedicineTaken.takenOn(set, d("2026-10-13")))
        assertTrue(MedicineTaken.takenOn(set, d("2026-10-12")).isEmpty())
    }

    @Test
    fun oldEntriesAreDroppedWhenANewOneIsAdded() {
        var set = MedicineTaken.add(emptySet(), d("2026-09-01"), "old")
        set = MedicineTaken.add(set, d("2026-10-14"), "new")
        assertFalse(MedicineTaken.contains(set, d("2026-09-01"), "old"))
        assertTrue(MedicineTaken.contains(set, d("2026-10-14"), "new"))
        // the last 14 days are kept
        val kept = MedicineTaken.add(MedicineTaken.add(emptySet(), d("2026-10-01"), "x"), d("2026-10-14"), "y")
        assertTrue(MedicineTaken.contains(kept, d("2026-10-01"), "x"))
    }

    @Test
    fun anUnreadableEntryIsDiscardedInsteadOfBreakingAnything() {
        val set = MedicineTaken.add(setOf("garbage", "also|bad"), d("2026-10-14"), "a1")
        assertEquals(setOf("2026-10-14|a1"), set)
    }

    @Test
    fun aTakenDoseCanBeUndone() {
        val set = MedicineTaken.add(emptySet(), d("2026-10-14"), "a1")
        assertTrue(MedicineTaken.remove(set, d("2026-10-14"), "a1").isEmpty())
    }

    @Test
    fun theSnoozeIsTenMinutes() {
        assertEquals(10, MedicineTaken.SNOOZE_MINUTES)
    }

    @Test
    fun aDoseMarkedTakenStopsThatReminderButNotTheOthers() {
        val prefs = NotificationPrefs(
            medicines = listOf(
                MedicineReminder("a1", "Metformin", "500 mg", 8 * 60),
                MedicineReminder("b2", "Vitamin D", "", 8 * 60)
            )
        )
        val specs = ReminderCatalog.build(prefs).filter { it.kind == ReminderKind.MEDICINE }
        assertEquals(listOf("a1", "b2"), specs.map { it.detail })
        val snapshot = TodaySnapshot(medicineTakenToday = setOf("a1"))
        assertFalse(ReminderDecider.shouldShow(specs[0], snapshot, prefs, 8 * 60))
        assertTrue(ReminderDecider.shouldShow(specs[1], snapshot, prefs, 8 * 60))
        assertTrue(ReminderDecider.shouldShow(specs[0], TodaySnapshot(), prefs, 8 * 60))
    }
}
