package com.example.presentation.notifications

import android.content.Context
import com.example.data.notifications.MedicineTaken
import java.time.LocalDate
import java.time.LocalTime

/** Which doses were marked taken (with the time) or skipped. Kept on this phone only, never synced; the last 14 days are kept. */
class MedicineTakenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("MedicineTaken", Context.MODE_PRIVATE)

    fun takenEntries(): Set<String> = prefs.getStringSet("entries", emptySet()) ?: emptySet()
    fun skippedEntries(): Set<String> = prefs.getStringSet("skipped", emptySet()) ?: emptySet()

    private fun nowMinute(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

    fun markTaken(medicineId: String, date: LocalDate = LocalDate.now(), minute: Int = nowMinute()) {
        prefs.edit()
            .putStringSet("entries", MedicineTaken.add(takenEntries(), date, medicineId, minute = minute))
            .putStringSet("skipped", MedicineTaken.remove(skippedEntries(), date, medicineId))
            .commit()
    }

    fun markSkipped(medicineId: String, date: LocalDate = LocalDate.now()) {
        prefs.edit()
            .putStringSet("skipped", MedicineTaken.add(skippedEntries(), date, medicineId))
            .putStringSet("entries", MedicineTaken.remove(takenEntries(), date, medicineId))
            .commit()
    }

    fun undo(medicineId: String, date: LocalDate = LocalDate.now()) {
        prefs.edit()
            .putStringSet("entries", MedicineTaken.remove(takenEntries(), date, medicineId))
            .putStringSet("skipped", MedicineTaken.remove(skippedEntries(), date, medicineId))
            .commit()
    }

    fun isTaken(medicineId: String, date: LocalDate = LocalDate.now()): Boolean = MedicineTaken.contains(takenEntries(), date, medicineId)
    fun isSkipped(medicineId: String, date: LocalDate = LocalDate.now()): Boolean = MedicineTaken.contains(skippedEntries(), date, medicineId)

    fun takenOn(date: LocalDate = LocalDate.now()): Set<String> = MedicineTaken.takenOn(takenEntries(), date)

    /** Doses that need no reminder today: taken or skipped. */
    fun handledOn(date: LocalDate = LocalDate.now()): Set<String> = takenOn(date) + MedicineTaken.takenOn(skippedEntries(), date)
}
