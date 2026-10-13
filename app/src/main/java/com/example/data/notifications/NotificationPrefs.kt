package com.example.data.notifications

/** Days of the week as a bit mask: Monday = 1, Tuesday = 2, Wednesday = 4 ... Sunday = 64. */
object Days {
    const val MON = 1
    const val TUE = 2
    const val WED = 4
    const val THU = 8
    const val FRI = 16
    const val SAT = 32
    const val SUN = 64
    const val ALL = 127
    const val WEEKDAYS = MON or TUE or WED or THU or FRI

    /** [dayOfWeek] follows java.time: Monday = 1 ... Sunday = 7. */
    fun has(mask: Int, dayOfWeek: Int): Boolean = mask and (1 shl (dayOfWeek - 1)) != 0

    fun toggle(mask: Int, dayOfWeek: Int): Int = mask xor (1 shl (dayOfWeek - 1))

    private val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    /** "Every day", "Weekdays" or "Mon, Wed". */
    fun label(mask: Int): String {
        val m = mask and ALL
        return when (m) {
            ALL, 0 -> "Every day"
            WEEKDAYS -> "Weekdays"
            else -> names.filterIndexed { i, _ -> m and (1 shl i) != 0 }.joinToString(", ")
        }
    }
}

/** A reminder at one time of day on the chosen days. Off unless the user turns it on. */
data class ReminderPref(
    val enabled: Boolean = false,
    val minuteOfDay: Int = 0,
    val days: Int = Days.ALL
)

/** One medicine reminder: [name] and an optional [dose] at one time of day on the chosen days. Up to [MedicineReminders.MAX] of them. */
data class MedicineReminder(
    val id: String = "",
    val name: String = "",
    val dose: String = "",
    val minuteOfDay: Int = 8 * 60,
    val days: Int = Days.ALL,
    val enabled: Boolean = true
)

/** Rules for the list of medicine reminders (the screen and the scheduler both go through these). */
object MedicineReminders {
    const val MAX = 6
    const val NAME_MAX = 40
    const val DOSE_MAX = 40

    fun canAdd(list: List<MedicineReminder>): Boolean = list.size < MAX

    /** Trims the text, caps its length and never lets the time or the days be invalid. */
    fun clean(r: MedicineReminder): MedicineReminder = r.copy(
        name = r.name.trim().take(NAME_MAX),
        dose = r.dose.trim().take(DOSE_MAX),
        minuteOfDay = r.minuteOfDay.coerceIn(0, 24 * 60 - 1),
        days = if (r.days and Days.ALL == 0) Days.ALL else r.days and Days.ALL
    )

    fun isValid(r: MedicineReminder): Boolean = r.name.trim().isNotEmpty()

    /** Replaces the reminder with the same id, or appends it when there is room. A full list is returned unchanged. */
    fun upsert(list: List<MedicineReminder>, reminder: MedicineReminder): List<MedicineReminder> {
        val cleaned = clean(reminder)
        if (!isValid(cleaned)) return list
        val index = list.indexOfFirst { it.id == cleaned.id }
        return when {
            index >= 0 -> list.mapIndexed { i, old -> if (i == index) cleaned else old }
            canAdd(list) -> list + cleaned
            else -> list
        }
    }

    fun remove(list: List<MedicineReminder>, id: String): List<MedicineReminder> = list.filter { it.id != id }

    /** "Take Metformin (500 mg)". */
    fun describe(r: MedicineReminder): String {
        val name = r.name.trim()
        val dose = r.dose.trim()
        return if (dose.isEmpty()) "Take $name" else "Take $name ($dose)"
    }
}

/** Water reminders: every [everyHours] hours between [startMinute] and [endMinute]. */
data class WaterReminderPref(
    val enabled: Boolean = false,
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 21 * 60,
    val everyHours: Int = 2
)

/**
 * Everything the user can switch in Settings > Notifications, stored as one JSON text in the profile
 * (so it is saved with the profile and follows the user to their other phones).
 * Defaults for a new user: only the three meal reminders are on; every other notification is off until switched on.
 * Messages, friend requests and challenges are on, because they only ever arrive after the user added friends.
 */
data class NotificationPrefs(
    val masterEnabled: Boolean = true,
    /** Quiet hours: -1 means no window is set (nothing is silenced). The window may wrap past midnight. */
    val quietStartMinute: Int = -1,
    val quietEndMinute: Int = -1,

    // Meals and nutrition
    val mealRemindersEnabled: Boolean = true,
    val breakfast: ReminderPref = ReminderPref(true, 8 * 60),
    val lunch: ReminderPref = ReminderPref(true, 13 * 60 + 30),
    val dinner: ReminderPref = ReminderPref(true, 20 * 60),
    val calorieLimitReached: Boolean = false,
    val nutritionGaps: Boolean = false,

    // Hydration
    val waterReminders: WaterReminderPref = WaterReminderPref(),
    val waterGoalReached: Boolean = false,

    // Activity and workouts
    val stepGoalReached: Boolean = false,
    val workoutReminder: ReminderPref = ReminderPref(false, 18 * 60, Days.WEEKDAYS),
    val weeklyWorkoutGoalMet: Boolean = false,

    // Sleep and mindfulness
    val bedtimeReminder: ReminderPref = ReminderPref(false, 22 * 60),
    val sleepGoalReached: Boolean = false,
    val mindfulMoment: ReminderPref = ReminderPref(false, 7 * 60 + 30),

    // Health checks
    val weeklyCheckIn: ReminderPref = ReminderPref(false, 19 * 60, Days.SUN),
    val glucoseRemindersEnabled: Boolean = false,
    val glucose: List<ReminderPref> = defaultGlucoseSlots(),
    val weighIn: ReminderPref = ReminderPref(false, 8 * 60, Days.MON),

    // Cycle tracker: only used while tracking is on for a Female profile (the reminder runner checks that), all off until switched on
    val cyclePeriodSoon: Boolean = false,
    val cyclePeriodSoonDays: Int = 2,
    val cyclePeriodLate: Boolean = false,
    val cyclePeriodLateDays: Int = 3,
    val cycleFertileWindow: Boolean = false,
    /** When the daily cycle check runs (it decides whether any of the cycle notices above applies today). */
    val cycleCheckMinute: Int = 9 * 60,
    val cycleLogReminder: ReminderPref = ReminderPref(false, 20 * 60),
    /** Evening before a good day to make up a fast (default Sunday and Wednesday evenings); only while Ramadan tracking is on. */
    val fastingMakeUp: ReminderPref = ReminderPref(false, 20 * 60, Days.SUN or Days.WED),

    // Health Connect: a nudge on the first login and then every 3 days until Health Connect is connected (it stops by itself once connected)
    val healthConnectNudge: Boolean = true,
    val healthConnectNudgeMinute: Int = 10 * 60,

    // Medicine reminders (up to 6). Never silenced by quiet hours; the lock screen shows only a generic text unless switched off.
    val medicines: List<MedicineReminder> = emptyList(),
    val medicineDiscreet: Boolean = true,

    // Social (server pushes)
    val socialMessages: Boolean = true,
    val socialFriendRequests: Boolean = true,
    val socialChallenges: Boolean = true
) {
    companion object {
        val GLUCOSE_SLOT_LABELS = listOf(
            "Before breakfast", "After breakfast", "Before lunch",
            "After lunch", "Before dinner", "After dinner"
        )

        fun defaultGlucoseSlots(): List<ReminderPref> = listOf(
            ReminderPref(true, 7 * 60 + 45),
            ReminderPref(true, 9 * 60 + 45),
            ReminderPref(true, 13 * 60 + 15),
            ReminderPref(true, 15 * 60 + 30),
            ReminderPref(true, 19 * 60 + 45),
            ReminderPref(true, 22 * 60)
        )
    }
}

/** "8:05 AM" style text for a minute of the day. */
fun formatMinuteOfDay(minuteOfDay: Int): String {
    val m = ((minuteOfDay % 1440) + 1440) % 1440
    val hour24 = m / 60
    val minute = m % 60
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "%d:%02d %s".format(hour12, minute, suffix)
}

object QuietHours {
    /** True when [minuteOfDay] falls inside the quiet window. No window (-1) or an empty one silences nothing. */
    fun isQuiet(prefs: NotificationPrefs, minuteOfDay: Int): Boolean {
        val start = prefs.quietStartMinute
        val end = prefs.quietEndMinute
        if (start < 0 || end < 0 || start == end) return false
        return if (start < end) {
            minuteOfDay in start until end
        } else {
            minuteOfDay >= start || minuteOfDay < end
        }
    }
}
