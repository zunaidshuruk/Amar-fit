package com.example.data.cycle

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The only cycle information the AI Assistant and the health analyst receive: a short summary of where the user is in her cycle.
 * Never the daily logs, symptoms or mood. Sent only while tracking is active and "Use my cycle in AI answers" is on.
 */
object CycleAiSummary {
    private val dateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    fun build(prediction: CyclePrediction, today: LocalDate): String? {
        val last = prediction.lastPeriodStart ?: return null
        val day = prediction.cycleDay ?: return null
        val phase = prediction.phase ?: return null
        val stats = prediction.stats
        val parts = ArrayList<String>()
        parts.add("day $day of a typical ${stats.avgCycleLength}-day cycle, ${phase.label.lowercase(Locale.US)}")
        if (prediction.isStale) {
            parts.add("the last logged period started ${last.format(dateFormat)} and a newer one has not been logged")
        } else if (prediction.daysLate > 0) {
            parts.add("period is ${prediction.daysLate} day${if (prediction.daysLate == 1) "" else "s"} later than expected")
        } else {
            val next = prediction.nextPeriodStart
            val until = prediction.daysUntilNextPeriod
            if (next != null && until != null) parts.add("next period expected about ${next.format(dateFormat)} (in $until day${if (until == 1) "" else "s"})")
        }
        parts.add(if (stats.isIrregular) "cycle lengths vary a lot" else "cycle lengths are fairly regular")
        return "Cycle tracker (estimates from the user's own logs): " + parts.joinToString("; ") + "."
    }
}
