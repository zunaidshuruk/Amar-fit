package com.example.data.notifications

import java.time.LocalDate

/** Today's doses for the "Today's medicine" card on the Today screen, taken from the medicine history. */
object MedicineToday {
    data class Summary(val doses: List<DoseRecord>) {
        val total: Int get() = doses.size
        val taken: Int get() = doses.count { it.status == DoseStatus.TAKEN }
        /** Taken or deliberately skipped: nothing more to do for these. */
        val handled: Int get() = doses.count { it.status == DoseStatus.TAKEN || it.status == DoseStatus.SKIPPED }
    }

    fun of(history: MedicineHistory, today: LocalDate): Summary =
        Summary(history.days.firstOrNull { it.date == today }?.doses ?: emptyList())

    fun headline(summary: Summary): String = when {
        summary.total == 0 -> ""
        summary.handled == summary.total -> "All doses done today"
        else -> "${summary.taken} of ${summary.total} taken today"
    }

    /** The label under a dose: its details and where it stands. */
    fun statusLabel(status: DoseStatus): String = when (status) {
        DoseStatus.TAKEN -> "Taken"
        DoseStatus.SKIPPED -> "Skipped"
        DoseStatus.MISSED -> "Missed"
        DoseStatus.DUE -> "Due now"
        DoseStatus.UPCOMING -> "Later today"
    }
}
