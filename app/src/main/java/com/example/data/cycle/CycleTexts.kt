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

    /** Label and value pairs for the prediction card on the Details view (empty until a period is logged). */
    fun predictionLines(p: CyclePrediction): List<Pair<String, String>> {
        if (p.lastPeriodStart == null) return emptyList()
        val lines = ArrayList<Pair<String, String>>()
        p.cycleDay?.let { lines.add("Cycle day" to it.toString()) }
        p.phase?.let { lines.add("Phase" to it.label) }
        val next = p.nextPeriodStart
        val nextEnd = p.nextPeriodEnd
        if (!p.isStale && next != null && nextEnd != null) {
            lines.add("Next period" to "${next.format(dateFormat)} \u2013 ${nextEnd.format(dateFormat)} (estimate)")
            val cycle = p.stats.avgCycleLength.toLong()
            lines.add("After that" to "${next.plusDays(cycle).format(dateFormat)}, ${next.plusDays(2 * cycle).format(dateFormat)}")
            p.ovulationDate?.let { lines.add("Estimated ovulation" to it.format(dateFormat)) }
            if (p.fertileStart != null && p.fertileEnd != null) {
                lines.add("Estimated fertile window" to "${p.fertileStart.format(dateFormat)} \u2013 ${p.fertileEnd.format(dateFormat)}")
            }
        }
        return lines
    }

    /** Label and value pairs for the averages card on the Details view. */
    fun statsLines(stats: CycleStats): List<Pair<String, String>> {
        val lines = ArrayList<Pair<String, String>>()
        lines.add("Average cycle" to "${stats.avgCycleLength} days")
        lines.add("Average period" to "${stats.avgPeriodLength} days")
        if (stats.cyclesUsed > 0) lines.add("Shortest / longest cycle" to "${stats.shortestCycle} / ${stats.longestCycle} days")
        lines.add("Cycles logged" to stats.cyclesUsed.toString())
        lines.add("Prediction confidence" to when (stats.confidence) {
            Confidence.LOW -> "Low"
            Confidence.MEDIUM -> "Medium"
            Confidence.HIGH -> "High"
        })
        return lines
    }

    /** One line for a logged day, or null when nothing was logged: "Oct 12 \u00B7 Medium flow \u00B7 Cramps, Fatigue \u00B7 Calm". */
    fun dayLine(date: java.time.LocalDate, flow: Int, symptoms: Int, mood: Int): String? {
        val parts = ArrayList<String>()
        val level = FlowLevel.fromCode(flow)
        if (level != FlowLevel.NONE) parts.add(if (level == FlowLevel.SPOTTING) "Spotting" else "${level.label} flow")
        val symptomLabels = Symptoms.labels(symptoms)
        if (symptomLabels.isNotEmpty()) parts.add(symptomLabels.joinToString(", "))
        val moodValue = Mood.fromCode(mood)
        if (moodValue != Mood.NOT_SET) parts.add(moodValue.label)
        if (parts.isEmpty()) return null
        return (listOf(date.format(dateFormat)) + parts).joinToString(" \u00B7 ")
    }

    fun period(start: java.time.LocalDate, end: java.time.LocalDate?): String =
        if (end == null) "${start.format(dateFormat)} (ongoing or end not logged)" else "${start.format(dateFormat)} \u2013 ${end.format(dateFormat)}"
}
