package com.example.data.notifications

import com.example.data.local.DailyMetric
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class GoalSettings(
    val stepGoal: Int,
    val waterGoalLiters: Float,
    val calorieLimit: Int,
    val sleepGoalHours: Float,
    val weeklyWorkoutGoal: Int
)

enum class GoalEvent { STEPS, WATER, CALORIES, SLEEP, WEEKLY_WORKOUTS }

/** A goal notification to show. [guardKey] is remembered so it is shown only once per day (or week). */
data class GoalNotice(val event: GoalEvent, val guardKey: String, val title: String, val message: String)

object GoalNotifier {

    fun weekKey(date: LocalDate): String {
        val year = date.get(IsoFields.WEEK_BASED_YEAR)
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "%d-W%02d".format(year, week)
    }

    /** Number of days from Monday of [today]'s week up to [today] with any exercise recorded. */
    fun workoutDaysInWeek(metrics: List<DailyMetric>, today: LocalDate): Int {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
        val end = today.toString()
        return metrics.filter { it.date >= monday && it.date <= end && it.exerciseMinutes > 0 }
            .map { it.date }.distinct().size
    }

    /**
     * Goal notifications that should be shown now. Each is event driven (run after today's numbers change),
     * switchable, silent during quiet hours (and then retried later because nothing is remembered), and shown at most
     * once per day, or once per week for the weekly workout goal.
     */
    fun evaluate(
        prefs: NotificationPrefs,
        goals: GoalSettings,
        today: DailyMetric?,
        workoutDaysThisWeek: Int,
        date: LocalDate,
        alreadyNotified: Set<String>,
        minuteNow: Int
    ): List<GoalNotice> {
        if (!prefs.masterEnabled || QuietHours.isQuiet(prefs, minuteNow)) return emptyList()
        val notices = ArrayList<GoalNotice>()
        val day = date.toString()

        fun add(event: GoalEvent, guard: String, title: String, message: String) {
            if (guard !in alreadyNotified) notices.add(GoalNotice(event, guard, title, message))
        }

        if (today != null) {
            if (prefs.stepGoalReached && goals.stepGoal > 0 && today.steps >= goals.stepGoal) {
                add(
                    GoalEvent.STEPS, "$day:steps", "Step goal reached \uD83C\uDF89",
                    "You walked %,d steps today. Well done!".format(Locale.US, today.steps)
                )
            }
            if (prefs.waterGoalReached && goals.waterGoalLiters > 0f && today.waterLiters >= goals.waterGoalLiters) {
                add(
                    GoalEvent.WATER, "$day:water", "Water goal reached \uD83D\uDCA7",
                    "You drank %.1f L today. Nicely hydrated!".format(Locale.US, today.waterLiters)
                )
            }
            if (prefs.calorieLimitReached && goals.calorieLimit > 0 && today.caloriesConsumed > goals.calorieLimit) {
                add(
                    GoalEvent.CALORIES, "$day:calories", "Calorie limit reached",
                    "You have eaten %,d kcal, over your %,d kcal goal for today.".format(Locale.US, today.caloriesConsumed, goals.calorieLimit)
                )
            }
            if (prefs.sleepGoalReached && goals.sleepGoalHours > 0f && today.sleepHours >= goals.sleepGoalHours) {
                add(
                    GoalEvent.SLEEP, "$day:sleep", "Sleep goal reached \uD83D\uDE34",
                    "You slept %.1f hours. A great night's rest!".format(Locale.US, today.sleepHours)
                )
            }
        }

        if (prefs.weeklyWorkoutGoalMet && goals.weeklyWorkoutGoal > 0 && workoutDaysThisWeek >= goals.weeklyWorkoutGoal) {
            add(
                GoalEvent.WEEKLY_WORKOUTS, "${weekKey(date)}:weekly_workouts", "Weekly workout goal met \uD83D\uDCAA",
                "You worked out on $workoutDaysThisWeek days this week. Goal: ${goals.weeklyWorkoutGoal}."
            )
        }
        return notices
    }
}
