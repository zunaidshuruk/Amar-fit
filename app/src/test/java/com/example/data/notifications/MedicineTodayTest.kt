package com.example.data.notifications

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicineTodayTest {
    private val today = LocalDate.parse("2026-10-15")
    private fun med(id: String, name: String, minute: Int) = MedicineReminder(id, name, "", minute, Days.ALL, true, "2026-10-01")

    private fun summary(taken: Set<String> = emptySet(), skipped: Set<String> = emptySet(), now: Int = 12 * 60, vararg meds: MedicineReminder) =
        MedicineToday.of(MedicineHistoryBuilder.build(meds.toList(), taken, skipped, today, now), today)

    @Test
    fun noRemindersMeansNoCard() {
        val s = MedicineToday.of(MedicineHistoryBuilder.build(emptyList(), emptySet(), emptySet(), today, 12 * 60), today)
        assertEquals(0, s.total)
        assertEquals("", MedicineToday.headline(s))
    }

    @Test
    fun todaysDosesAreListedByTimeWithTheirStatus() {
        val taken = MedicineTaken.add(emptySet(), today, "a", today, 8 * 60 + 5)
        val s = summary(taken, emptySet(), 12 * 60, med("b", "Evening", 21 * 60), med("a", "Morning", 8 * 60), med("c", "Noon", 11 * 60))
        assertEquals(listOf("a", "c", "b"), s.doses.map { it.reminderId })
        assertEquals(listOf(DoseStatus.TAKEN, DoseStatus.MISSED, DoseStatus.UPCOMING), s.doses.map { it.status })
        assertEquals(8 * 60 + 5, s.doses.first().takenMinute)
    }

    @Test
    fun theHeadlineCountsWhatWasTaken() {
        val taken = MedicineTaken.add(emptySet(), today, "a", today)
        assertEquals("1 of 2 taken today", MedicineToday.headline(summary(taken, emptySet(), 12 * 60, med("a", "A", 8 * 60), med("b", "B", 21 * 60))))
        assertEquals("0 of 1 taken today", MedicineToday.headline(summary(emptySet(), emptySet(), 7 * 60, med("a", "A", 8 * 60))))
    }

    @Test
    fun whenEverythingIsTakenOrSkippedItSaysSo() {
        val taken = MedicineTaken.add(emptySet(), today, "a", today)
        val skipped = MedicineTaken.add(emptySet(), today, "b", today)
        val s = summary(taken, skipped, 12 * 60, med("a", "A", 8 * 60), med("b", "B", 9 * 60))
        assertEquals(1, s.taken)
        assertEquals(2, s.handled)
        assertEquals("All doses done today", MedicineToday.headline(s))
    }

    @Test
    fun aSkippedDoseIsNotCountedAsTaken() {
        val skipped = MedicineTaken.add(emptySet(), today, "a", today)
        val s = summary(emptySet(), skipped, 12 * 60, med("a", "A", 8 * 60), med("b", "B", 21 * 60))
        assertEquals(0, s.taken)
        assertEquals(1, s.handled)
        assertEquals("0 of 2 taken today", MedicineToday.headline(s))
    }

    @Test
    fun otherDaysNeverLeakIn() {
        val yesterdayTaken = MedicineTaken.add(emptySet(), today.minusDays(1), "a", today)
        val s = summary(yesterdayTaken, emptySet(), 12 * 60, med("a", "A", 8 * 60))
        assertEquals(DoseStatus.MISSED, s.doses.single().status)
    }

    @Test
    fun theLabelsArePlain() {
        assertEquals(listOf("Taken", "Skipped", "Missed", "Due now", "Later today"), DoseStatus.values().map { MedicineToday.statusLabel(it) })
        assertTrue(MedicineToday.statusLabel(DoseStatus.DUE).isNotBlank())
    }
}
