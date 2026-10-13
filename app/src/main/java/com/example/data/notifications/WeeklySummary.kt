package com.example.data.notifications

import com.example.data.local.DailyMetric
import java.util.Locale

data class WeeklySummaryText(val title: String, val body: String)

/** Text of the Sunday "week in review" notification, built from the last seven days of numbers. */
object WeeklySummary {

    fun build(last7: List<DailyMetric>): WeeklySummaryText {
        val title = "Your week in review"
        val stepDays = last7.filter { it.steps > 0 }
        val sleepDays = last7.filter { it.sleepHours > 0f }
        val waterDays = last7.filter { it.waterLiters > 0f }
        val workoutDays = last7.filter { it.exerciseMinutes > 0 }.map { it.date }.distinct().size
        val weights = last7.filter { it.weightKg > 0f }.sortedBy { it.date }

        val parts = ArrayList<String>()
        if (stepDays.isNotEmpty()) {
            parts.add("Steps %,d/day".format(Locale.US, stepDays.map { it.steps }.average().toInt()))
        }
        if (sleepDays.isNotEmpty()) {
            parts.add("Sleep %.1f h".format(Locale.US, sleepDays.map { it.sleepHours }.average()))
        }
        if (waterDays.isNotEmpty()) {
            parts.add("Water %.1f L/day".format(Locale.US, waterDays.map { it.waterLiters }.average()))
        }
        if (workoutDays > 0) {
            parts.add("Workouts $workoutDays day" + if (workoutDays == 1) "" else "s")
        }
        if (weights.size >= 2) {
            val change = weights.last().weightKg - weights.first().weightKg
            val sign = if (change > 0.05f) "+" else ""
            parts.add("Weight $sign%.1f kg".format(Locale.US, change))
        }
        val body = if (parts.isEmpty()) {
            "Log your steps, sleep, water and meals to see your weekly summary here."
        } else {
            parts.joinToString(" \u00B7 ")
        }
        return WeeklySummaryText(title, body)
    }
}
