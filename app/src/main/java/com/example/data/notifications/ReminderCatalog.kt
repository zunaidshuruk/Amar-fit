package com.example.data.notifications

import java.time.Instant
import java.time.ZoneId

/** Notification channel ids, one per category so Android's own settings mirror the app's. */
object Channels {
    const val MEALS = "shastho_reminders"      // kept from the first version so existing settings carry over
    const val HYDRATION = "kardiq_hydration"
    const val ACTIVITY = "kardiq_activity"
    const val SLEEP_MIND = "kardiq_sleep_mind"
    const val HEALTH_CHECKS = "kardiq_health_checks"
    const val GOALS = "kardiq_goals"
    const val CYCLE = "kardiq_cycle"
    const val SOCIAL = "amarfit_social"
}

enum class ReminderKind { MEAL, WATER, WORKOUT, BEDTIME, MINDFUL, WEEKLY_CHECKIN, GLUCOSE, WEIGH_IN, CYCLE, CYCLE_LOG, FASTING }

/** One alarm the phone must arm: when, on which days, and what to show. */
data class ReminderSpec(
    val id: String,
    val requestCode: Int,
    val kind: ReminderKind,
    val minuteOfDay: Int,
    val days: Int,
    val title: String,
    val message: String,
    val route: String,
    val channel: String,
    /** Meal name ("breakfast"), glucose slot index ("0".."5") or water window text, depending on [kind]. */
    val detail: String = ""
)

object ReminderCatalog {
    private const val MEAL_BASE = 7000
    private const val WATER_BASE = 7100
    private const val WATER_MAX = 32
    private const val WORKOUT_CODE = 7200
    private const val BEDTIME_CODE = 7300
    private const val MINDFUL_CODE = 7400
    private const val WEEKLY_CODE = 7500
    private const val GLUCOSE_BASE = 7600
    private const val WEIGH_CODE = 7700
    private const val CYCLE_CHECK_CODE = 7800
    private const val CYCLE_LOG_CODE = 7801
    private const val FASTING_CODE = 7802

    /** Request codes of every alarm the app may ever arm, used to cancel everything before re-arming. */
    fun allRequestCodes(): List<Int> {
        val codes = ArrayList<Int>()
        for (i in 1..3) codes.add(MEAL_BASE + i)
        for (i in 0 until WATER_MAX) codes.add(WATER_BASE + i)
        codes.add(WORKOUT_CODE); codes.add(BEDTIME_CODE); codes.add(MINDFUL_CODE); codes.add(WEEKLY_CODE)
        for (i in 0 until 6) codes.add(GLUCOSE_BASE + i)
        codes.add(WEIGH_CODE)
        codes.add(CYCLE_CHECK_CODE); codes.add(CYCLE_LOG_CODE); codes.add(FASTING_CODE)
        return codes
    }

    /** Minutes of the day of every water reminder: start, start + N hours, ... up to and including the end. */
    fun waterTimes(p: WaterReminderPref): List<Int> {
        val step = (p.everyHours.coerceIn(1, 6)) * 60
        val times = ArrayList<Int>()
        var t = p.startMinute
        while (t <= p.endMinute && times.size < WATER_MAX) {
            times.add(t)
            t += step
        }
        return times
    }

    /** All reminders that are switched on. The master switch is checked by the caller. */
    fun build(prefs: NotificationPrefs): List<ReminderSpec> {
        val specs = ArrayList<ReminderSpec>()

        if (prefs.mealRemindersEnabled) {
            val meals = listOf(
                Triple("breakfast", "Breakfast", prefs.breakfast),
                Triple("lunch", "Lunch", prefs.lunch),
                Triple("dinner", "Dinner", prefs.dinner)
            )
            meals.forEachIndexed { index, (key, label, pref) ->
                if (pref.enabled) {
                    specs.add(
                        ReminderSpec(
                            id = "meal_$key", requestCode = MEAL_BASE + index + 1, kind = ReminderKind.MEAL,
                            minuteOfDay = pref.minuteOfDay, days = Days.ALL,
                            title = "Time for $label!",
                            message = "Don't forget to log your $label using the AI scanner.",
                            route = "scanner", channel = Channels.MEALS, detail = key
                        )
                    )
                }
            }
        }

        if (prefs.waterReminders.enabled) {
            waterTimes(prefs.waterReminders).forEachIndexed { index, minute ->
                specs.add(
                    ReminderSpec(
                        id = "water_$minute", requestCode = WATER_BASE + index, kind = ReminderKind.WATER,
                        minuteOfDay = minute, days = Days.ALL,
                        title = "Time for some water \uD83D\uDCA7",
                        message = "Have a glass of water and log it to stay on track.",
                        route = "today", channel = Channels.HYDRATION
                    )
                )
            }
        }

        if (prefs.workoutReminder.enabled) {
            specs.add(
                ReminderSpec(
                    id = "workout", requestCode = WORKOUT_CODE, kind = ReminderKind.WORKOUT,
                    minuteOfDay = prefs.workoutReminder.minuteOfDay, days = prefs.workoutReminder.days,
                    title = "Workout time", message = "Ready to move? Start a workout now.",
                    route = "fitness", channel = Channels.ACTIVITY
                )
            )
        }

        if (prefs.bedtimeReminder.enabled) {
            specs.add(
                ReminderSpec(
                    id = "bedtime", requestCode = BEDTIME_CODE, kind = ReminderKind.BEDTIME,
                    minuteOfDay = prefs.bedtimeReminder.minuteOfDay, days = prefs.bedtimeReminder.days,
                    title = "Time to wind down \uD83C\uDF19", message = "A regular bedtime helps you sleep better.",
                    route = "sleep", channel = Channels.SLEEP_MIND
                )
            )
        }

        if (prefs.mindfulMoment.enabled) {
            specs.add(
                ReminderSpec(
                    id = "mindful", requestCode = MINDFUL_CODE, kind = ReminderKind.MINDFUL,
                    minuteOfDay = prefs.mindfulMoment.minuteOfDay, days = prefs.mindfulMoment.days,
                    title = "A mindful moment", message = "Take a few minutes to breathe and reset.",
                    route = "mindfulness_timer", channel = Channels.SLEEP_MIND
                )
            )
        }

        if (prefs.weeklyCheckIn.enabled) {
            specs.add(
                ReminderSpec(
                    id = "weekly_checkin", requestCode = WEEKLY_CODE, kind = ReminderKind.WEEKLY_CHECKIN,
                    minuteOfDay = prefs.weeklyCheckIn.minuteOfDay, days = prefs.weeklyCheckIn.days,
                    title = "Your week in review", message = "Tap to see how your week went.",
                    route = "health", channel = Channels.HEALTH_CHECKS
                )
            )
        }

        if (prefs.glucoseRemindersEnabled) {
            prefs.glucose.take(6).forEachIndexed { index, pref ->
                if (pref.enabled) {
                    val label = NotificationPrefs.GLUCOSE_SLOT_LABELS[index]
                    specs.add(
                        ReminderSpec(
                            id = "glucose_$index", requestCode = GLUCOSE_BASE + index, kind = ReminderKind.GLUCOSE,
                            minuteOfDay = pref.minuteOfDay, days = pref.days,
                            title = "Blood glucose check",
                            message = "$label: log your blood sugar reading.",
                            route = "glucoselog", channel = Channels.HEALTH_CHECKS, detail = index.toString()
                        )
                    )
                }
            }
        }

        if (prefs.weighIn.enabled) {
            specs.add(
                ReminderSpec(
                    id = "weigh_in", requestCode = WEIGH_CODE, kind = ReminderKind.WEIGH_IN,
                    minuteOfDay = prefs.weighIn.minuteOfDay, days = prefs.weighIn.days,
                    title = "Weekly weigh-in", message = "Step on the scale and log your weight.",
                    route = "weightlog", channel = Channels.HEALTH_CHECKS
                )
            )
        }

        // Cycle tracker: one daily check at a fixed time decides on the day whether any cycle notice applies (period soon, late, fertile
        // window), because those depend on the logs; the log reminder is a plain daily reminder. The runner only shows either of them
        // while tracking is active for a Female profile, so stale settings are harmless.
        if (prefs.cyclePeriodSoon || prefs.cyclePeriodLate || prefs.cycleFertileWindow) {
            specs.add(
                ReminderSpec(
                    id = "cycle_check", requestCode = CYCLE_CHECK_CODE, kind = ReminderKind.CYCLE,
                    minuteOfDay = prefs.cycleCheckMinute, days = Days.ALL,
                    title = "Cycle tracker", message = "Open KardIQ to see your cycle.",
                    route = "cycle", channel = Channels.CYCLE
                )
            )
        }
        if (prefs.cycleLogReminder.enabled) {
            specs.add(
                ReminderSpec(
                    id = "cycle_log", requestCode = CYCLE_LOG_CODE, kind = ReminderKind.CYCLE_LOG,
                    minuteOfDay = prefs.cycleLogReminder.minuteOfDay, days = prefs.cycleLogReminder.days,
                    title = "How are you feeling today?", message = "Log your day in the cycle tracker.",
                    route = "cycle", channel = Channels.CYCLE
                )
            )
        }
        if (prefs.fastingMakeUp.enabled) {
            specs.add(
                ReminderSpec(
                    id = "fasting_makeup", requestCode = FASTING_CODE, kind = ReminderKind.FASTING,
                    minuteOfDay = prefs.fastingMakeUp.minuteOfDay, days = prefs.fastingMakeUp.days,
                    title = "Fasting make-up", message = "Open KardIQ to see your make-up fasts.",
                    route = "cycle", channel = Channels.CYCLE
                )
            )
        }
        return specs
    }
}

object ReminderPlanner {
    /**
     * The next moment after [nowMillis] that is [minuteOfDay] on one of the [days]; null when no day is chosen.
     * Alarms are armed one occurrence at a time and re-armed after they fire.
     */
    fun nextTrigger(minuteOfDay: Int, days: Int, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (days and Days.ALL == 0) return null
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        var date = now.toLocalDate()
        for (i in 0..7) {
            val candidate = date.atTime(minuteOfDay / 60, minuteOfDay % 60).atZone(zone)
            val millis = candidate.toInstant().toEpochMilli()
            if (millis > nowMillis && Days.has(days, candidate.dayOfWeek.value)) return millis
            date = date.plusDays(1)
        }
        return null
    }
}
