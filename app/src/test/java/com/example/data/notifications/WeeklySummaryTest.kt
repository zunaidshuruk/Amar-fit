package com.example.data.notifications

import com.example.data.local.DailyMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklySummaryTest {

    @Test
    fun emptyWeekAsksTheUserToLog() {
        val s = WeeklySummary.build(emptyList())
        assertEquals("Your week in review", s.title)
        assertTrue(s.body.startsWith("Log your steps"))
    }

    @Test
    fun summaryListsAveragesWorkoutsAndWeightChange() {
        val week = listOf(
            DailyMetric(date = "2026-10-08", steps = 6000, sleepHours = 6f, waterLiters = 1.5f, exerciseMinutes = 30, weightKg = 82.0f),
            DailyMetric(date = "2026-10-09", steps = 8000, sleepHours = 7f, waterLiters = 2.5f, exerciseMinutes = 0),
            DailyMetric(date = "2026-10-10", steps = 10000, sleepHours = 8f, waterLiters = 2.0f, exerciseMinutes = 40, weightKg = 81.6f)
        )
        val s = WeeklySummary.build(week)
        assertTrue(s.body, s.body.contains("Steps 8,000/day"))
        assertTrue(s.body, s.body.contains("Sleep 7.0 h"))
        assertTrue(s.body, s.body.contains("Water 2.0 L/day"))
        assertTrue(s.body, s.body.contains("Workouts 2 days"))
        assertTrue(s.body, s.body.contains("Weight -0.4 kg"))
    }

    @Test
    fun onlyDaysWithDataCountTowardsAverages() {
        val week = listOf(
            DailyMetric(date = "2026-10-08", steps = 4000),
            DailyMetric(date = "2026-10-09", steps = 0),
            DailyMetric(date = "2026-10-10", steps = 6000)
        )
        assertTrue(WeeklySummary.build(week).body.contains("Steps 5,000/day"))
    }

    @Test
    fun weightGainShowsAPlusSign() {
        val week = listOf(
            DailyMetric(date = "2026-10-08", weightKg = 80.0f),
            DailyMetric(date = "2026-10-10", weightKg = 80.5f)
        )
        assertTrue(WeeklySummary.build(week).body.contains("Weight +0.5 kg"))
    }

    @Test
    fun oneWorkoutDayIsSingular() {
        assertTrue(WeeklySummary.build(listOf(DailyMetric(date = "2026-10-08", exerciseMinutes = 20))).body.contains("Workouts 1 day"))
    }
}
