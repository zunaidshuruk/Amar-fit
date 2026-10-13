package com.example.data.cycle

import com.example.data.notifications.NotificationPrefs
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CycleNoticeKind { PERIOD_SOON, PERIOD_LATE, FERTILE_START }

/** The real text of a cycle notification. By default only a generic text shows on the lock screen (see NotificationHelper). */
data class CycleNotice(val kind: CycleNoticeKind, val title: String, val message: String)

object CycleNotices {
    private val dateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    /** The notice to show today, or null. Late beats soon beats the fertile window. Each fires on exactly one day, so it is never repeated. */
    fun forToday(prediction: CyclePrediction, prefs: NotificationPrefs, today: LocalDate): CycleNotice? {
        val next = prediction.nextPeriodStart ?: return null
        if (prediction.isStale) return null

        if (prefs.cyclePeriodLate && prediction.daysLate > 0 && prediction.daysLate == prefs.cyclePeriodLateDays) {
            val n = prediction.daysLate
            return CycleNotice(
                CycleNoticeKind.PERIOD_LATE,
                "Period is late",
                "Your period is $n day${if (n == 1) "" else "s"} later than expected. Cycles vary; if this is unusual for you, consider talking to a doctor."
            )
        }
        val until = prediction.daysUntilNextPeriod
        if (prefs.cyclePeriodSoon && until != null && until >= 0 && until == prefs.cyclePeriodSoonDays) {
            val whenText = when (until) {
                0 -> "today"
                1 -> "tomorrow"
                else -> "in $until days"
            }
            return CycleNotice(
                CycleNoticeKind.PERIOD_SOON,
                if (until == 0) "Period expected today" else "Period expected soon",
                "Your period is expected $whenText (around ${next.format(dateFormat)}). This is an estimate. Log it when it starts."
            )
        }
        if (prefs.cycleFertileWindow && prediction.fertileStart != null && prediction.fertileStart == today) {
            return CycleNotice(
                CycleNoticeKind.FERTILE_START,
                "Fertile window starts today",
                "Based on your logged cycles, your estimated fertile window starts today. This is an estimate, not a medical test and not contraception."
            )
        }
        return null
    }
}
