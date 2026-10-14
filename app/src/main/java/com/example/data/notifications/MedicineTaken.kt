package com.example.data.notifications

import java.time.LocalDate

/**
 * Which medicine doses were marked "Taken" (and when) or "Skipped", on which day. Kept on the phone only (never synced). Entries are
 * "date|id" or "date|id|minuteOfDay". A taken dose stops that day's reminder, any snoozed repeat and the missed-dose alert; entries older
 * than [KEEP_DAYS] days are dropped whenever a new one is added. The same functions serve the taken set and the skipped set.
 */
object MedicineTaken {
    const val KEEP_DAYS = 14L
    const val SNOOZE_MINUTES = 10

    fun key(date: LocalDate, medicineId: String): String = "$date|$medicineId"

    private fun matches(entry: String, date: LocalDate, medicineId: String): Boolean {
        val k = key(date, medicineId)
        return entry == k || entry.startsWith("$k|")
    }

    fun contains(entries: Set<String>, date: LocalDate, medicineId: String): Boolean =
        entries.any { matches(it, date, medicineId) }

    /** Stores the dose for the day (replacing an earlier entry for it). [minute] is the minute of the day it happened, or -1 when unknown. */
    fun add(entries: Set<String>, date: LocalDate, medicineId: String, today: LocalDate = date, minute: Int = -1): Set<String> {
        val oldest = today.minusDays(KEEP_DAYS)
        val kept = entries.filter { entry ->
            val day = try { LocalDate.parse(entry.substringBefore('|')) } catch (e: Exception) { null }
            day != null && !day.isBefore(oldest) && !matches(entry, date, medicineId)
        }.toMutableSet()
        kept.add(if (minute in 0 until 24 * 60) "${key(date, medicineId)}|$minute" else key(date, medicineId))
        return kept
    }

    fun remove(entries: Set<String>, date: LocalDate, medicineId: String): Set<String> =
        entries.filterNot { matches(it, date, medicineId) }.toSet()

    /** The ids of the medicines with an entry on [date]. */
    fun takenOn(entries: Set<String>, date: LocalDate): Set<String> =
        entries.filter { it.substringBefore('|') == date.toString() }.map { it.substringAfter('|').substringBefore('|') }.toSet()

    /** The minute of the day the dose was marked, or null when it was not recorded. */
    fun minuteOf(entries: Set<String>, date: LocalDate, medicineId: String): Int? {
        val entry = entries.firstOrNull { matches(it, date, medicineId) } ?: return null
        return entry.split('|').getOrNull(2)?.toIntOrNull()
    }
}
