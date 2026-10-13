package com.example.presentation.notifications

import android.content.Context
import com.example.data.notifications.MedicineTaken
import java.time.LocalDate

/** Which medicine doses were marked taken (kept on this phone only, never synced). */
class MedicineTakenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("MedicineTaken", Context.MODE_PRIVATE)

    private fun entries(): Set<String> = prefs.getStringSet("entries", emptySet()) ?: emptySet()

    fun markTaken(medicineId: String, date: LocalDate = LocalDate.now()) {
        prefs.edit().putStringSet("entries", MedicineTaken.add(entries(), date, medicineId)).commit()
    }

    fun undo(medicineId: String, date: LocalDate = LocalDate.now()) {
        prefs.edit().putStringSet("entries", MedicineTaken.remove(entries(), date, medicineId)).commit()
    }

    fun isTaken(medicineId: String, date: LocalDate = LocalDate.now()): Boolean = MedicineTaken.contains(entries(), date, medicineId)

    fun takenOn(date: LocalDate = LocalDate.now()): Set<String> = MedicineTaken.takenOn(entries(), date)
}
