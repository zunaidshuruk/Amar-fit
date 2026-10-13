package com.example.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPrefsCodecTest {

    @Test
    fun blankOrBrokenJsonGivesTheDefaults() {
        assertEquals(NotificationPrefs(), NotificationPrefsCodec.decode(null))
        assertEquals(NotificationPrefs(), NotificationPrefsCodec.decode(""))
        assertEquals(NotificationPrefs(), NotificationPrefsCodec.decode("not json"))
    }

    @Test
    fun everythingSurvivesARoundTrip() {
        val p = NotificationPrefs(
            masterEnabled = false, quietStartMinute = 22 * 60, quietEndMinute = 7 * 60,
            breakfast = ReminderPref(false, 7 * 60 + 15),
            waterReminders = WaterReminderPref(true, 8 * 60, 20 * 60, 3),
            stepGoalReached = true, workoutReminder = ReminderPref(true, 17 * 60, Days.MON or Days.WED),
            glucoseRemindersEnabled = true, socialMessages = false
        )
        assertEquals(p, NotificationPrefsCodec.decode(NotificationPrefsCodec.encode(p)))
    }

    @Test
    fun theMigrationJsonKeepsTheOldSwitchesAndDefaultsTheRest() {
        val p = NotificationPrefsCodec.decode("""{"masterEnabled":false,"mealRemindersEnabled":true}""")
        assertFalse(p.masterEnabled)
        assertTrue(p.mealRemindersEnabled)
        assertEquals(ReminderPref(true, 13 * 60 + 30), p.lunch)
        assertFalse(p.nutritionGaps)
        assertFalse(p.waterReminders.enabled)
        assertTrue(p.socialMessages)
        assertEquals(6, p.glucose.size)
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val p = NotificationPrefsCodec.decode("""{"masterEnabled":true,"somethingNew":42}""")
        assertTrue(p.masterEnabled)
    }
}
