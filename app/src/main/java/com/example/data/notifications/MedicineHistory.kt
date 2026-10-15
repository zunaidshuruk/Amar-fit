package com.example.data.notifications

import java.time.LocalDate

enum class DoseStatus { TAKEN, SKIPPED, MISSED, DUE, UPCOMING }

data class DoseRecord(
    val reminderId: String,
    val name: String,
    val dose: String,
    val minuteOfDay: Int,
    val status: DoseStatus,
    /** The minute of the day it was marked taken, when known. */
    val takenMinute: Int?
)

data class DayHistory(val date: LocalDate, val doses: List<DoseRecord>)

/** [taken] of [counted] doses (taken, skipped or missed; doses still to come are not counted). */
data class Adherence(val taken: Int, val counted: Int) {
    val percent: Int get() = if (counted == 0) 0 else Math.round(taken * 100f / counted)
}

data class MedicineHistory(val days: List<DayHistory>, val last7: Adherence, val last14: Adherence)

/**
 * The medicine history, worked out from the CURRENT reminders (each from the day it was created) and the phone's taken and skipped records.
 * Editing a reminder's time or days therefore changes how earlier days look; the reminder's creation day keeps new reminders from
 * showing doses as missed before they existed.
 */
object MedicineHistoryBuilder {
    const val DAYS = 14

    fun build(
        reminders: List<MedicineReminder>,
        taken: Set<String>,
        skipped: Set<String>,
        today: LocalDate,
        nowMinute: Int,
        graceMinutes: Int = MedicineReminders.MISSED_AFTER_DEFAULT
    ): MedicineHistory {
        val active = reminders.take(MedicineReminders.MAX).filter { it.enabled && MedicineReminders.isValid(it) }
        val windowStart = today.minusDays((DAYS - 1).toLong())
        val days = ArrayList<DayHistory>()
        var date = today
        while (!date.isBefore(windowStart)) {
            val doses = ArrayList<DoseRecord>()
            for (r in active) {
                val created = try { LocalDate.parse(r.createdOn) } catch (e: Exception) { today }
                if (date.isBefore(created) || !Days.has(r.days, date.dayOfWeek.value)) continue
                val status = when {
                    MedicineTaken.contains(taken, date, r.id) -> DoseStatus.TAKEN
                    MedicineTaken.contains(skipped, date, r.id) -> DoseStatus.SKIPPED
                    date.isBefore(today) -> DoseStatus.MISSED
                    nowMinute >= r.minuteOfDay + graceMinutes -> DoseStatus.MISSED
                    nowMinute >= r.minuteOfDay -> DoseStatus.DUE
                    else -> DoseStatus.UPCOMING
                }
                doses.add(
                    DoseRecord(
                        r.id, r.name.trim(), r.dose.trim(), r.minuteOfDay, status,
                        if (status == DoseStatus.TAKEN) MedicineTaken.minuteOf(taken, date, r.id) else null
                    )
                )
            }
            if (doses.isNotEmpty()) days.add(DayHistory(date, doses.sortedBy { it.minuteOfDay }))
            date = date.minusDays(1)
        }
        return MedicineHistory(days, adherence(days, today.minusDays(6)), adherence(days, windowStart))
    }

    private fun adherence(days: List<DayHistory>, from: LocalDate): Adherence {
        var taken = 0
        var counted = 0
        for (day in days) {
            if (day.date.isBefore(from)) continue
            for (d in day.doses) {
                when (d.status) {
                    DoseStatus.TAKEN -> { taken++; counted++ }
                    DoseStatus.SKIPPED, DoseStatus.MISSED -> counted++
                    DoseStatus.DUE, DoseStatus.UPCOMING -> Unit
                }
            }
        }
        return Adherence(taken, counted)
    }
}

object MedicineMissed {
    /** Whether the "Missed dose?" alert is still needed when its alarm fires. */
    fun needed(prefs: NotificationPrefs, taken: Boolean, skipped: Boolean): Boolean =
        prefs.masterEnabled && prefs.medicineMissedAlert && !taken && !skipped

    const val TITLE = "Missed dose?"

    fun message(reminder: MedicineReminder): String = messageFor(MedicineReminders.describe(reminder))

    /** [description] is what the reminder says, for example "Take Metformin (500 mg)". */
    fun messageFor(description: String): String = "You haven't marked ${description.removePrefix("Take ")} as taken."
}
