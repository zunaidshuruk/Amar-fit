package com.example.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPrefsTest {

    @Test
    fun newUserHasOnlyTheThreeMealRemindersOn() {
        val p = NotificationPrefs()
        assertTrue(p.masterEnabled)
        assertTrue(p.mealRemindersEnabled)
        assertEquals(ReminderPref(true, 8 * 60), p.breakfast)
        assertEquals(ReminderPref(true, 13 * 60 + 30), p.lunch)
        assertEquals(ReminderPref(true, 20 * 60), p.dinner)
        assertFalse(p.calorieLimitReached)
        assertFalse(p.nutritionGaps)
        assertFalse(p.waterReminders.enabled)
        assertFalse(p.waterGoalReached)
        assertFalse(p.stepGoalReached)
        assertFalse(p.workoutReminder.enabled)
        assertFalse(p.weeklyWorkoutGoalMet)
        assertFalse(p.bedtimeReminder.enabled)
        assertFalse(p.sleepGoalReached)
        assertFalse(p.mindfulMoment.enabled)
        assertFalse(p.weeklyCheckIn.enabled)
        assertFalse(p.glucoseRemindersEnabled)
        assertFalse(p.weighIn.enabled)
    }

    @Test
    fun noQuietWindowSilencesNothing() {
        val p = NotificationPrefs()
        assertEquals(-1, p.quietStartMinute)
        for (minute in listOf(0, 3 * 60, 12 * 60, 23 * 60 + 59)) assertFalse(QuietHours.isQuiet(p, minute))
    }

    @Test
    fun quietWindowInsideOneDay() {
        val p = NotificationPrefs(quietStartMinute = 13 * 60, quietEndMinute = 15 * 60)
        assertFalse(QuietHours.isQuiet(p, 12 * 60 + 59))
        assertTrue(QuietHours.isQuiet(p, 13 * 60))
        assertTrue(QuietHours.isQuiet(p, 14 * 60 + 59))
        assertFalse(QuietHours.isQuiet(p, 15 * 60))
    }

    @Test
    fun quietWindowThatWrapsPastMidnight() {
        val p = NotificationPrefs(quietStartMinute = 22 * 60, quietEndMinute = 7 * 60)
        assertTrue(QuietHours.isQuiet(p, 22 * 60))
        assertTrue(QuietHours.isQuiet(p, 23 * 60 + 30))
        assertTrue(QuietHours.isQuiet(p, 2 * 60))
        assertTrue(QuietHours.isQuiet(p, 6 * 60 + 59))
        assertFalse(QuietHours.isQuiet(p, 7 * 60))
        assertFalse(QuietHours.isQuiet(p, 12 * 60))
    }

    @Test
    fun emptyOrHalfSetWindowSilencesNothing() {
        assertFalse(QuietHours.isQuiet(NotificationPrefs(quietStartMinute = 600, quietEndMinute = 600), 600))
        assertFalse(QuietHours.isQuiet(NotificationPrefs(quietStartMinute = 600, quietEndMinute = -1), 700))
    }

    @Test
    fun timeTextIsTwelveHour() {
        assertEquals("12:00 AM", formatMinuteOfDay(0))
        assertEquals("8:05 AM", formatMinuteOfDay(8 * 60 + 5))
        assertEquals("12:30 PM", formatMinuteOfDay(12 * 60 + 30))
        assertEquals("1:30 PM", formatMinuteOfDay(13 * 60 + 30))
        assertEquals("11:59 PM", formatMinuteOfDay(23 * 60 + 59))
    }

    @Test
    fun dayMaskHelpers() {
        assertTrue(Days.has(Days.WEEKDAYS, 1))
        assertTrue(Days.has(Days.WEEKDAYS, 5))
        assertFalse(Days.has(Days.WEEKDAYS, 6))
        assertFalse(Days.has(Days.WEEKDAYS, 7))
        assertEquals(Days.WEEKDAYS or Days.SAT, Days.toggle(Days.WEEKDAYS, 6))
        assertEquals(Days.WEEKDAYS xor Days.MON, Days.toggle(Days.WEEKDAYS, 1))
    }
}
