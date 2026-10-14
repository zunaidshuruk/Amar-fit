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

    @Test
    fun medicineRemindersSurviveARoundTrip() {
        val p = NotificationPrefs(
            medicines = listOf(
                MedicineReminder("a1", "Metformin", "500 mg", 8 * 60, Days.ALL, true),
                MedicineReminder("b2", "Vitamin D", "", 13 * 60, Days.MON or Days.WED, false)
            ),
            medicineDiscreet = false, healthConnectNudge = false
        )
        assertEquals(p, NotificationPrefsCodec.decode(NotificationPrefsCodec.encode(p)))
    }

    @Test
    fun settingsSavedBeforeTheseFeaturesStillLoad() {
        val p = NotificationPrefsCodec.decode("""{"masterEnabled":true,"mealRemindersEnabled":false}""")
        assertTrue(p.healthConnectNudge)
        assertTrue(p.medicines.isEmpty())
        assertTrue(p.medicineDiscreet)
    }

    @Test
    fun missedAlertSettingsAndCreationDaysSurviveARoundTrip() {
        val p = NotificationPrefs(
            medicines = listOf(MedicineReminder("a1", "Metformin", "500 mg", 8 * 60, Days.ALL, true, "2026-10-08")),
            medicineMissedAlert = false, medicineMissedAfterMinutes = 90
        )
        assertEquals(p, NotificationPrefsCodec.decode(NotificationPrefsCodec.encode(p)))
        // reminders saved before the history existed load with an empty creation day and the missed alert on
        val old = NotificationPrefsCodec.decode("""{"medicines":[{"id":"x","name":"Aspirin","minuteOfDay":480,"days":127,"enabled":true}]}""")
        assertEquals("", old.medicines.single().createdOn)
        assertTrue(old.medicineMissedAlert)
        assertEquals(60, old.medicineMissedAfterMinutes)
    }
}
