package com.example.data.notifications

/** What the phone knows about today when a reminder fires, so reminders can skip what is already done. */
data class TodaySnapshot(
    val loggedMealTypes: Set<String> = emptySet(),
    val waterLiters: Float = 0f,
    val waterGoalLiters: Float = 0f,
    val exerciseMinutes: Int = 0,
    val workoutEventsToday: Int = 0,
    val mindfulnessMinutes: Int = 0,
    val weightLoggedToday: Boolean = false,
    /** For the six glucose slots, in order: has a reading been logged for that slot today. */
    val glucoseSlotLogged: List<Boolean> = List(6) { false },
    /** Something was logged in the cycle tracker today (flow, symptoms or mood). */
    val cycleLoggedToday: Boolean = false
)

object ReminderDecider {

    /** Whether a reminder that just fired should actually be shown. */
    fun shouldShow(spec: ReminderSpec, snap: TodaySnapshot, prefs: NotificationPrefs, minuteNow: Int): Boolean {
        if (!prefs.masterEnabled) return false
        // a medicine reminder is never silenced by quiet hours: a missed dose matters more than a quiet night
        if (spec.kind != ReminderKind.MEDICINE && QuietHours.isQuiet(prefs, minuteNow)) return false
        return when (spec.kind) {
            ReminderKind.MEAL -> spec.detail.lowercase() !in snap.loggedMealTypes
            ReminderKind.WATER -> !waterOnTrack(snap, prefs.waterReminders, minuteNow)
            ReminderKind.WORKOUT -> snap.exerciseMinutes <= 0 && snap.workoutEventsToday <= 0
            ReminderKind.MINDFUL -> snap.mindfulnessMinutes <= 0
            ReminderKind.WEIGH_IN -> !snap.weightLoggedToday
            ReminderKind.GLUCOSE -> {
                val slot = spec.detail.toIntOrNull() ?: -1
                !(slot in snap.glucoseSlotLogged.indices && snap.glucoseSlotLogged[slot])
            }
            ReminderKind.CYCLE_LOG -> !snap.cycleLoggedToday
            // the daily cycle check only runs; whether there is anything to say is decided from the predictions by the runner
            ReminderKind.CYCLE, ReminderKind.FASTING, ReminderKind.HEALTH_CONNECT, ReminderKind.MEDICINE,
            ReminderKind.BEDTIME, ReminderKind.WEEKLY_CHECKIN -> true
        }
    }

    /** True when the water logged so far is at least what an even pace through the reminder window would give. */
    fun waterOnTrack(snap: TodaySnapshot, p: WaterReminderPref, minuteNow: Int): Boolean {
        val goal = snap.waterGoalLiters
        if (goal <= 0f) return false
        if (snap.waterLiters >= goal) return true
        val span = (p.endMinute - p.startMinute).coerceAtLeast(1)
        val elapsed = (minuteNow - p.startMinute).coerceIn(0, span)
        val expected = goal * elapsed / span
        return snap.waterLiters >= expected
    }
}
