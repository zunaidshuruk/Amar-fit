package com.example.data.notifications

import com.example.data.local.DailyMetric
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalNotifierTest {

    private val goals = GoalSettings(stepGoal = 10000, waterGoalLiters = 3f, calorieLimit = 2000, sleepGoalHours = 8f, weeklyWorkoutGoal = 3)
    private val date = LocalDate.of(2026, 10, 14) // a Wednesday
    private val allOn = NotificationPrefs(
        stepGoalReached = true, waterGoalReached = true, calorieLimitReached = true,
        sleepGoalReached = true, weeklyWorkoutGoalMet = true
    )

    private fun eval(
        prefs: NotificationPrefs = allOn, today: DailyMetric? = null, workoutDays: Int = 0,
        notified: Set<String> = emptySet(), minute: Int = 12 * 60
    ) = GoalNotifier.evaluate(prefs, goals, today, workoutDays, date, notified, minute)

    @Test
    fun nothingIsShownByDefault() {
        val today = DailyMetric(date = "2026-10-14", steps = 20000, waterLiters = 5f, caloriesConsumed = 3000, sleepHours = 9f)
        assertTrue(eval(prefs = NotificationPrefs(), today = today, workoutDays = 7).isEmpty())
    }

    @Test
    fun stepGoalIsAnnouncedOnceWhenReached() {
        val below = DailyMetric(date = "2026-10-14", steps = 9999)
        assertTrue(eval(today = below).isEmpty())
        val reached = DailyMetric(date = "2026-10-14", steps = 10000)
        val notices = eval(today = reached)
        assertEquals(listOf(GoalEvent.STEPS), notices.map { it.event })
        assertEquals("2026-10-14:steps", notices[0].guardKey)
        // the guard of an earlier notice stops a second one the same day
        assertTrue(eval(today = reached, notified = setOf("2026-10-14:steps")).isEmpty())
        // but a new day is a new chance
        assertEquals(1, GoalNotifier.evaluate(allOn, goals, reached, 0, date.plusDays(1), setOf("2026-10-14:steps"), 600).size)
    }

    @Test
    fun waterCaloriesAndSleepGoals() {
        val today = DailyMetric(date = "2026-10-14", waterLiters = 3.1f, caloriesConsumed = 2150, sleepHours = 8.2f)
        assertEquals(
            setOf(GoalEvent.WATER, GoalEvent.CALORIES, GoalEvent.SLEEP),
            eval(today = today).map { it.event }.toSet()
        )
        // exactly on the calorie limit is not "over" it
        assertTrue(eval(today = DailyMetric(date = "2026-10-14", caloriesConsumed = 2000)).none { it.event == GoalEvent.CALORIES })
    }

    @Test
    fun eachGoalNeedsItsOwnSwitch() {
        val today = DailyMetric(date = "2026-10-14", steps = 20000, waterLiters = 5f)
        val onlySteps = NotificationPrefs(stepGoalReached = true)
        assertEquals(listOf(GoalEvent.STEPS), eval(prefs = onlySteps, today = today).map { it.event })
    }

    @Test
    fun aGoalOfZeroNeverTriggers() {
        val none = GoalSettings(0, 0f, 0, 0f, 0)
        val today = DailyMetric(date = "2026-10-14", steps = 5, waterLiters = 1f, caloriesConsumed = 100, sleepHours = 1f)
        assertTrue(GoalNotifier.evaluate(allOn, none, today, 7, date, emptySet(), 600).isEmpty())
    }

    @Test
    fun weeklyWorkoutGoalIsOncePerWeek() {
        assertTrue(eval(workoutDays = 2).isEmpty())
        val notices = eval(workoutDays = 3)
        assertEquals(listOf(GoalEvent.WEEKLY_WORKOUTS), notices.map { it.event })
        assertEquals("2026-W42:weekly_workouts", notices[0].guardKey)
        assertTrue(eval(workoutDays = 5, notified = setOf("2026-W42:weekly_workouts")).isEmpty())
    }

    @Test
    fun quietHoursAndTheMasterSwitchSuppressEverything() {
        val today = DailyMetric(date = "2026-10-14", steps = 20000)
        val quiet = allOn.copy(quietStartMinute = 22 * 60, quietEndMinute = 7 * 60)
        assertTrue(eval(prefs = quiet, today = today, minute = 23 * 60).isEmpty())
        assertEquals(1, eval(prefs = quiet, today = today, minute = 12 * 60).size)
        assertTrue(eval(prefs = allOn.copy(masterEnabled = false), today = today).isEmpty())
    }

    @Test
    fun workoutDaysAreCountedFromMondayOfThisWeek() {
        val metrics = listOf(
            DailyMetric(date = "2026-10-11", exerciseMinutes = 30), // Sunday of last week: not counted
            DailyMetric(date = "2026-10-12", exerciseMinutes = 20), // Monday
            DailyMetric(date = "2026-10-13", exerciseMinutes = 0),
            DailyMetric(date = "2026-10-14", exerciseMinutes = 45)  // today
        )
        assertEquals(2, GoalNotifier.workoutDaysInWeek(metrics, date))
    }

    @Test
    fun weekKeyUsesTheIsoWeek() {
        assertEquals("2026-W42", GoalNotifier.weekKey(LocalDate.of(2026, 10, 14)))
        assertEquals("2026-W01", GoalNotifier.weekKey(LocalDate.of(2025, 12, 29)))
    }
}
