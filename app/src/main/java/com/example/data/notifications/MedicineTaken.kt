package com.example.data.notifications

import java.time.LocalDate

/**
 * Which medicine reminders were marked "Taken" on which day. Kept on the phone only (never synced). A taken dose stops that day's
 * reminder and any snoozed repeat of it. Entries older than [KEEP_DAYS] days are dropped whenever a new one is added.
 */
object MedicineTaken {
    const val KEEP_DAYS = 14L
    const val SNOOZE_MINUTES = 10

    fun key(date: LocalDate, medicineId: String): String = "$date|$medicineId"

    fun contains(entries: Set<String>, date: LocalDate, medicineId: String): Boolean = key(date, medicineId) in entries

    fun add(entries: Set<String>, date: LocalDate, medicineId: String, today: LocalDate = date): Set<String> {
        val oldest = today.minusDays(KEEP_DAYS)
        val kept = entries.filter { entry ->
            val day = try { LocalDate.parse(entry.substringBefore('|')) } catch (e: Exception) { null }
            day != null && !day.isBefore(oldest)
        }.toMutableSet()
        kept.add(key(date, medicineId))
        return kept
    }

    fun remove(entries: Set<String>, date: LocalDate, medicineId: String): Set<String> = entries - key(date, medicineId)

    /** The ids of the medicines marked taken on [date]. */
    fun takenOn(entries: Set<String>, date: LocalDate): Set<String> =
        entries.filter { it.substringBefore('|') == date.toString() }.map { it.substringAfter('|') }.toSet()
}
