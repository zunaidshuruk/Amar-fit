package com.example.data.cycle

import java.time.format.DateTimeFormatter
import java.util.Locale

/** The wording shown on the cycle cards and screen, kept here so it can be tested. */
object CycleTexts {
    private val dateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    const val DISCLAIMER = "Predictions are estimates based on the dates you log. KardIQ is not a medical device and cannot be used as contraception."

    fun headline(p: CyclePrediction?): String {
        val day = p?.cycleDay
        val phase = p?.phase
        return if (p == null || p.lastPeriodStart == null || day == null || phase == null) "Log your last period" else "Day $day \u00B7 ${phase.label}"
    }

    fun subline(p: CyclePrediction?): String {
        if (p == null || p.lastPeriodStart == null) return "Predictions start after you log a period."
        val next = p.nextPeriodStart
        val until = p.daysUntilNextPeriod
        return when {
            p.isStale -> "Log your latest period to refresh the predictions."
            p.daysLate > 0 -> "Your period is ${p.daysLate} day${if (p.daysLate == 1) "" else "s"} later than expected."
            until == 0 -> "Your period is expected today."
            until == 1 -> "Your period is expected tomorrow."
            until != null && next != null -> "Next period in $until days (${next.format(dateFormat)})."
            else -> "Predictions start after you log a period."
        }
    }

    fun basis(stats: CycleStats): String = when (stats.cyclesUsed) {
        0 -> "Based on typical values until you have logged more cycles."
        1 -> "Based on 1 logged cycle."
        else -> "Based on your last ${stats.cyclesUsed} logged cycles."
    }

    /** A gentle note when the cycles are very irregular or unusual; null when there is nothing to point out. */
    fun regularityNote(stats: CycleStats): String? = when {
        stats.isIrregular -> "Your cycle lengths vary by more than a week, so predictions are less accurate. If this is new for you, consider talking to a doctor."
        stats.hasUnusualLength -> "Some of your cycles are shorter than 21 or longer than 35 days. If this is unusual for you, consider talking to a doctor."
        else -> null
    }

    fun period(start: java.time.LocalDate, end: java.time.LocalDate?): String =
        if (end == null) "${start.format(dateFormat)} (ongoing or end not logged)" else "${start.format(dateFormat)} \u2013 ${end.format(dateFormat)}"
}
