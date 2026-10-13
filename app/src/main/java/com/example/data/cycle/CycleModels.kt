package com.example.data.cycle

import java.time.LocalDate

/** How heavy the flow was on a day. The codes are stored, so never renumber them. */
enum class FlowLevel(val code: Int, val label: String) {
    NONE(0, "None"),
    SPOTTING(1, "Spotting"),
    LIGHT(2, "Light"),
    MEDIUM(3, "Medium"),
    HEAVY(4, "Heavy");

    companion object {
        fun fromCode(code: Int): FlowLevel = values().firstOrNull { it.code == code } ?: NONE
    }
}

data class Symptom(val bit: Int, val label: String)

/** Symptoms are stored as a bit mask on the day. Never reuse or renumber a bit. */
object Symptoms {
    val ALL = listOf(
        Symptom(1, "Cramps"),
        Symptom(2, "Headache"),
        Symptom(4, "Bloating"),
        Symptom(8, "Back pain"),
        Symptom(16, "Breast tenderness"),
        Symptom(32, "Acne"),
        Symptom(64, "Nausea"),
        Symptom(128, "Fatigue"),
        Symptom(256, "Cravings")
    )

    fun has(mask: Int, bit: Int): Boolean = mask and bit != 0
    fun toggle(mask: Int, bit: Int): Int = mask xor bit
    fun labels(mask: Int): List<String> = ALL.filter { has(mask, it.bit) }.map { it.label }
}

enum class Mood(val code: Int, val label: String) {
    NOT_SET(0, "Not set"),
    HAPPY(1, "Happy"),
    CALM(2, "Calm"),
    ENERGETIC(3, "Energetic"),
    IRRITABLE(4, "Irritable"),
    SAD(5, "Sad"),
    ANXIOUS(6, "Anxious");

    companion object {
        fun fromCode(code: Int): Mood = values().firstOrNull { it.code == code } ?: NOT_SET
    }
}

enum class CyclePhase(val label: String, val blurb: String) {
    MENSTRUAL("Period", "Your period is estimated to be under way."),
    FOLLICULAR("Follicular phase", "The days after your period, before the fertile window."),
    OVULATION("Fertile window", "The estimated fertile days around ovulation."),
    LUTEAL("Luteal phase", "The days before your next period.")
}

enum class Confidence { LOW, MEDIUM, HIGH }

/** A logged period. [end] is null while it is still going or the end was not logged. */
data class PeriodSpan(val start: LocalDate, val end: LocalDate? = null)

data class CycleStats(
    val avgCycleLength: Int,
    val avgPeriodLength: Int,
    /** How many logged cycles the averages are based on (0 = typical values are used). */
    val cyclesUsed: Int,
    val shortestCycle: Int,
    val longestCycle: Int,
    /** Cycle lengths that differ by more than a week. */
    val isIrregular: Boolean,
    /** Any recent cycle shorter than 21 or longer than 35 days. */
    val hasUnusualLength: Boolean,
    val confidence: Confidence
)

data class CyclePrediction(
    val stats: CycleStats,
    val lastPeriodStart: LocalDate?,
    val cycleDay: Int?,
    val phase: CyclePhase?,
    val inPeriodNow: Boolean,
    val nextPeriodStart: LocalDate?,
    val nextPeriodEnd: LocalDate?,
    val ovulationDate: LocalDate?,
    val fertileStart: LocalDate?,
    val fertileEnd: LocalDate?,
    val daysUntilNextPeriod: Int?,
    /** Days past the expected start with no new period logged; 0 when not late. */
    val daysLate: Int,
    /** So late that predictions are hidden and the user is asked to log their last period. */
    val isStale: Boolean
)

data class DayMarks(
    val period: Boolean = false,
    val predictedPeriod: Boolean = false,
    val fertile: Boolean = false,
    val ovulation: Boolean = false
)
