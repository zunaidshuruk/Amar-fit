package com.example.data.notifications

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicineHistoryTest {
    private fun d(s: String) = LocalDate.parse(s)
    private val today = d("2026-10-14") // a Wednesday

    private fun med(
        id: String = "a1", name: String = "Metformin", dose: String = "500 mg", minute: Int = 8 * 60,
        days: Int = Days.ALL, enabled: Boolean = true, created: String = "2026-10-08"
    ) = MedicineReminder(id, name, dose, minute, days, enabled, created)

    private fun statusOn(history: MedicineHistory, date: String, id: String = "a1") =
        history.days.firstOrNull { it.date == d(date) }?.doses?.firstOrNull { it.reminderId == id }?.status

    @Test
    fun everyDayFromCreationShowsWhatHappenedNewestFirst() {
        var taken = MedicineTaken.add(emptySet(), d("2026-10-09"), "a1", today, 8 * 60 + 10)
        taken = MedicineTaken.add(taken, d("2026-10-10"), "a1", today, 8 * 60 + 15)
        val skipped = MedicineTaken.add(emptySet(), d("2026-10-11"), "a1", today)
        val h = MedicineHistoryBuilder.build(listOf(med()), taken, skipped, today, 8 * 60 + 30)
        assertEquals(
            listOf("2026-10-14", "2026-10-13", "2026-10-12", "2026-10-11", "2026-10-10", "2026-10-09", "2026-10-08").map { d(it) },
            h.days.map { it.date }
        )
        assertEquals(DoseStatus.DUE, statusOn(h, "2026-10-14"))
        assertEquals(DoseStatus.MISSED, statusOn(h, "2026-10-13"))
        assertEquals(DoseStatus.MISSED, statusOn(h, "2026-10-12"))
        assertEquals(DoseStatus.SKIPPED, statusOn(h, "2026-10-11"))
        assertEquals(DoseStatus.TAKEN, statusOn(h, "2026-10-10"))
        assertEquals(DoseStatus.TAKEN, statusOn(h, "2026-10-09"))
        assertEquals(DoseStatus.MISSED, statusOn(h, "2026-10-08"))
    }

    @Test
    fun todayMovesFromUpcomingToDueToMissed() {
        fun status(now: Int) = statusOn(MedicineHistoryBuilder.build(listOf(med()), emptySet(), emptySet(), today, now, 60), "2026-10-14")
        assertEquals(DoseStatus.UPCOMING, status(7 * 60))
        assertEquals(DoseStatus.DUE, status(8 * 60))
        assertEquals(DoseStatus.DUE, status(8 * 60 + 59))
        assertEquals(DoseStatus.MISSED, status(9 * 60))
    }

    @Test
    fun theTimeAThoseWasTakenIsShownOnlyForTakenDoses() {
        val taken = MedicineTaken.add(emptySet(), today, "a1", today, 8 * 60 + 12)
        val h = MedicineHistoryBuilder.build(listOf(med()), taken, emptySet(), today, 10 * 60)
        val dose = h.days.first().doses.single()
        assertEquals(DoseStatus.TAKEN, dose.status)
        assertEquals(8 * 60 + 12, dose.takenMinute)
        val missed = MedicineHistoryBuilder.build(listOf(med()), emptySet(), emptySet(), today, 10 * 60).days.first().doses.single()
        assertNull(missed.takenMinute)
    }

    @Test
    fun aTakenDoseWithoutARecordedTimeStillCounts() {
        val taken = MedicineTaken.add(emptySet(), today, "a1", today) // no minute
        val dose = MedicineHistoryBuilder.build(listOf(med()), taken, emptySet(), today, 10 * 60).days.first().doses.single()
        assertEquals(DoseStatus.TAKEN, dose.status)
        assertNull(dose.takenMinute)
    }

    @Test
    fun aReminderMadeBeforeTheHistoryExistedNeverShowsInventedMissedDoses() {
        // createdOn is empty for those: the history starts today
        val h = MedicineHistoryBuilder.build(listOf(med(created = "")), emptySet(), emptySet(), today, 10 * 60)
        assertEquals(listOf(today), h.days.map { it.date })
    }

    @Test
    fun aNewReminderOnlyCountsFromItsCreationDay() {
        val h = MedicineHistoryBuilder.build(listOf(med(created = "2026-10-13")), emptySet(), emptySet(), today, 7 * 60)
        assertEquals(listOf(d("2026-10-14"), d("2026-10-13")), h.days.map { it.date })
    }

    @Test
    fun theHistoryNeverLooksBackMoreThanFourteenDays() {
        val h = MedicineHistoryBuilder.build(listOf(med(created = "2026-01-01")), emptySet(), emptySet(), today, 7 * 60)
        assertEquals(14, h.days.size)
        assertEquals(d("2026-10-01"), h.days.last().date)
    }

    @Test
    fun onlyTheChosenWeekdaysAreScheduled() {
        val h = MedicineHistoryBuilder.build(listOf(med(days = Days.MON or Days.WED, created = "2026-10-05")), emptySet(), emptySet(), today, 7 * 60)
        // Mon 12 and Wed 14 (Wed 7 is before the creation day... Oct 5 is a Monday, so Mon 5, Wed 7, Mon 12, Wed 14)
        assertEquals(listOf("2026-10-14", "2026-10-12", "2026-10-07", "2026-10-05").map { d(it) }, h.days.map { it.date })
    }

    @Test
    fun disabledAndNamelessRemindersAreLeftOut() {
        val h = MedicineHistoryBuilder.build(
            listOf(med("a1", enabled = false), med("b2", name = "  "), med("c3", name = "Vitamin D", dose = "")),
            emptySet(), emptySet(), today, 10 * 60
        )
        assertEquals(listOf("c3"), h.days.first().doses.map { it.reminderId })
    }

    @Test
    fun severalDosesAreListedByTime() {
        val h = MedicineHistoryBuilder.build(
            listOf(med("late", name = "Evening", minute = 21 * 60), med("early", name = "Morning", minute = 7 * 60)),
            emptySet(), emptySet(), today, 7 * 60 + 30
        )
        assertEquals(listOf("early", "late"), h.days.first().doses.map { it.reminderId })
    }

    @Test
    fun adherenceCountsTakenAgainstTakenSkippedAndMissedButNotWhatIsStillToCome() {
        // created Oct 8: days Oct 8..Oct 13 are complete (6 doses), today is still upcoming
        var taken = emptySet<String>()
        for (day in listOf("2026-10-08", "2026-10-09", "2026-10-10", "2026-10-12")) taken = MedicineTaken.add(taken, d(day), "a1", today)
        val skipped = MedicineTaken.add(emptySet(), d("2026-10-11"), "a1", today)
        val h = MedicineHistoryBuilder.build(listOf(med()), taken, skipped, today, 7 * 60)
        assertEquals(Adherence(4, 6), h.last14) // Oct 13 is missed
        assertEquals(Adherence(4, 6), h.last7)
        assertEquals(67, h.last7.percent)
    }

    @Test
    fun theSevenDayFigureLeavesOutOlderDays() {
        // created Oct 1; only Oct 8 to Oct 13 are in the last 7 days (today excluded as upcoming)
        val taken = MedicineTaken.add(emptySet(), d("2026-10-02"), "a1", today)
        val h = MedicineHistoryBuilder.build(listOf(med(created = "2026-10-01")), taken, emptySet(), today, 7 * 60)
        assertEquals(13, h.last14.counted)
        assertEquals(1, h.last14.taken)
        assertEquals(6, h.last7.counted)
        assertEquals(0, h.last7.taken)
    }

    @Test
    fun noDosesMeansNoPercentage() {
        val h = MedicineHistoryBuilder.build(emptyList(), emptySet(), emptySet(), today, 10 * 60)
        assertTrue(h.days.isEmpty())
        assertEquals(0, h.last7.percent)
    }

    // ---- missed dose alert
    @Test
    fun theMissedAlertIsOnByDefaultAfterAnHour() {
        val p = NotificationPrefs()
        assertTrue(p.medicineMissedAlert)
        assertEquals(60, p.medicineMissedAfterMinutes)
        assertEquals(60, MedicineReminders.missedAfter(p))
    }

    @Test
    fun theMissedDelayIsKeptInSensibleBounds() {
        assertEquals(15, MedicineReminders.missedAfter(NotificationPrefs(medicineMissedAfterMinutes = 1)))
        assertEquals(180, MedicineReminders.missedAfter(NotificationPrefs(medicineMissedAfterMinutes = 999)))
        assertEquals(90, MedicineReminders.missedAfter(NotificationPrefs(medicineMissedAfterMinutes = 90)))
    }

    @Test
    fun theMissedAlertOnlyFiresForADoseThatIsNeitherTakenNorSkipped() {
        val p = NotificationPrefs()
        assertTrue(MedicineMissed.needed(p, taken = false, skipped = false))
        assertFalse(MedicineMissed.needed(p, taken = true, skipped = false))
        assertFalse(MedicineMissed.needed(p, taken = false, skipped = true))
        assertFalse(MedicineMissed.needed(p.copy(medicineMissedAlert = false), taken = false, skipped = false))
        assertFalse(MedicineMissed.needed(p.copy(masterEnabled = false), taken = false, skipped = false))
    }

    @Test
    fun theMissedMessageNamesTheMedicine() {
        assertEquals("You haven't marked Metformin (500 mg) as taken.", MedicineMissed.message(med()))
        assertEquals("You haven't marked Vitamin D as taken.", MedicineMissed.message(med(name = "Vitamin D", dose = "")))
        assertEquals("Missed dose?", MedicineMissed.TITLE)
    }

    // ---- reminders keep their creation day
    @Test
    fun editingAReminderKeepsItsCreationDay() {
        val list = listOf(med("a1", created = "2026-10-08"))
        val edited = MedicineReminders.upsert(list, med("a1", name = "Insulin", created = ""))
        assertEquals("2026-10-08", edited.single().createdOn)
        assertEquals("Insulin", edited.single().name)
    }

    @Test
    fun aNewReminderKeepsTheDayTheScreenGaveIt() {
        val added = MedicineReminders.upsert(emptyList(), med("n1", created = "2026-10-14"))
        assertEquals("2026-10-14", added.single().createdOn)
    }

    @Test
    fun theTakenTimeRoundTrips() {
        val set = MedicineTaken.add(emptySet(), today, "a1", today, 8 * 60 + 12)
        assertEquals(8 * 60 + 12, MedicineTaken.minuteOf(set, today, "a1"))
        assertTrue(MedicineTaken.contains(set, today, "a1"))
        assertEquals(setOf("a1"), MedicineTaken.takenOn(set, today))
        // marking again replaces the earlier entry instead of adding a second one
        val again = MedicineTaken.add(set, today, "a1", today, 9 * 60)
        assertEquals(1, again.size)
        assertEquals(9 * 60, MedicineTaken.minuteOf(again, today, "a1"))
        assertTrue(MedicineTaken.remove(again, today, "a1").isEmpty())
        assertNull(MedicineTaken.minuteOf(emptySet(), today, "a1"))
    }
}
