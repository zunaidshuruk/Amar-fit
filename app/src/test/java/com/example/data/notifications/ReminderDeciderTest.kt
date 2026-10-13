package com.example.data.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderDeciderTest {

    private fun spec(kind: ReminderKind, detail: String = "") = ReminderSpec(
        "x", 1, kind, 480, Days.ALL, "t", "m", "today", Channels.MEALS, detail
    )

    private val prefs = NotificationPrefs()

    @Test
    fun aMealReminderIsSkippedWhenThatMealIsAlreadyLogged() {
        val snap = TodaySnapshot(loggedMealTypes = setOf("breakfast"))
        assertFalse(ReminderDecider.shouldShow(spec(ReminderKind.MEAL, "breakfast"), snap, prefs, 480))
        assertTrue(ReminderDecider.shouldShow(spec(ReminderKind.MEAL, "lunch"), snap, prefs, 480))
    }

    @Test
    fun waterIsSkippedWhenOnTrackAndShownWhenBehind() {
        val p = prefs.copy(waterReminders = WaterReminderPref(true, 9 * 60, 21 * 60, 2))
        val noon = 15 * 60 // half way through the 12 hour window
        val onTrack = TodaySnapshot(waterLiters = 1.5f, waterGoalLiters = 3f)
        val behind = TodaySnapshot(waterLiters = 0.5f, waterGoalLiters = 3f)
        val done = TodaySnapshot(waterLiters = 3.2f, waterGoalLiters = 3f)
        assertFalse(ReminderDecider.shouldShow(spec(ReminderKind.WATER), onTrack, p, noon))
        assertTrue(ReminderDecider.shouldShow(spec(ReminderKind.WATER), behind, p, noon))
        assertFalse(ReminderDecider.shouldShow(spec(ReminderKind.WATER), done, p, 10 * 60))
    }

    @Test
    fun waterIsShownWhenThereIsNoGoal() {
        val p = prefs.copy(waterReminders = WaterReminderPref(true))
        assertTrue(ReminderDecider.shouldShow(spec(ReminderKind.WATER), TodaySnapshot(waterLiters = 2f, waterGoalLiters = 0f), p, 700))
    }

    @Test
    fun workoutMindfulAndWeighInSkipWhatIsAlreadyDone() {
        assertFalse(ReminderDecider.shouldShow(spec(ReminderKind.WORKOUT), TodaySnapshot(exerciseMinutes = 20), prefs, 600))
        assertFalse(ReminderDecider.shouldShow(spec(ReminderKind.WORKOUT), TodaySnapshot(workoutEventsToday = 1), prefs, 600))
        assertTrue(ReminderDecider.shouldShow(spec(ReminderKind.WORKOUT), TodaySnapshot(), prefs, 600))
        assertFalse(ReminderDecider.shouldShow(spec(ReminderKind.MINDFUL), TodaySnapshot(mindfulnessMinutes = 5), prefs, 600))
        assertFalse(ReminderDecider.shouldShow(spec(ReminderKind.WEIGH_IN), TodaySnapshot(weightLoggedToday = true), prefs, 600))
        assertTrue(ReminderDecider.shouldShow(spec(ReminderKind.WEIGH_IN), TodaySnapshot(), prefs, 600))
    }

    @Test
    fun glucoseIsSkippedOnlyForTheSlotThatIsLogged() {
        val snap = TodaySnapshot(glucoseSlotLogged = listOf(true, false, false, false, false, false))
        assertFalse(ReminderDecider.shouldShow(spec(ReminderKind.GLUCOSE, "0"), snap, prefs, 600))
        assertTrue(ReminderDecider.shouldShow(spec(ReminderKind.GLUCOSE, "1"), snap, prefs, 600))
    }

    @Test
    fun bedtimeAndWeeklyCheckInAreAlwaysShown() {
        assertTrue(ReminderDecider.shouldShow(spec(ReminderKind.BEDTIME), TodaySnapshot(), prefs, 22 * 60))
        assertTrue(ReminderDecider.shouldShow(spec(ReminderKind.WEEKLY_CHECKIN), TodaySnapshot(), prefs, 19 * 60))
    }

    @Test
    fun masterSwitchAndQuietHoursSilenceEverything() {
        val off = prefs.copy(masterEnabled = false)
        assertFalse(ReminderDecider.shouldShow(spec(ReminderKind.BEDTIME), TodaySnapshot(), off, 22 * 60))
        val quiet = prefs.copy(quietStartMinute = 21 * 60, quietEndMinute = 7 * 60)
        assertFalse(ReminderDecider.shouldShow(spec(ReminderKind.BEDTIME), TodaySnapshot(), quiet, 22 * 60))
        assertTrue(ReminderDecider.shouldShow(spec(ReminderKind.BEDTIME), TodaySnapshot(), quiet, 12 * 60))
    }
}
