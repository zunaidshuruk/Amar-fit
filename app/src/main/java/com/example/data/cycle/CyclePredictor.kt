package com.example.data.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The calendar method: cycle length is the gap between period starts (average of the last six plausible cycles),
 * ovulation is about 14 days before the next period, and the fertile window is the five days before ovulation, and the day after.
 * Everything here is an estimate: cycles vary and this is not a medical test or contraception.
 */
object CyclePredictor {
    const val DEFAULT_CYCLE_LENGTH = 28
    const val DEFAULT_PERIOD_LENGTH = 5
    const val LUTEAL_LENGTH = 14
    const val STALE_AFTER_LATE_DAYS = 60

    private const val MAX_CYCLES_USED = 6
    private const val MIN_PLAUSIBLE_CYCLE = 15
    private const val MAX_PLAUSIBLE_CYCLE = 90

    fun stats(
        periods: List<PeriodSpan>,
        defaultCycle: Int = DEFAULT_CYCLE_LENGTH,
        defaultPeriod: Int = DEFAULT_PERIOD_LENGTH
    ): CycleStats {
        val starts = periods.map { it.start }.distinct().sorted()
        // a gap above 90 days almost always means a period was not logged, so it says nothing about the cycle
        val gaps = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
            .filter { it in MIN_PLAUSIBLE_CYCLE..MAX_PLAUSIBLE_CYCLE }
            .takeLast(MAX_CYCLES_USED)
        val avgCycle = if (gaps.isEmpty()) defaultCycle.coerceIn(MIN_PLAUSIBLE_CYCLE, MAX_PLAUSIBLE_CYCLE) else gaps.average().roundToInt()

        val lengths = periods.filter { it.end != null }
            .map { ChronoUnit.DAYS.between(it.start, it.end).toInt() + 1 }
            .filter { it in 1..14 }
            .takeLast(MAX_CYCLES_USED)
        val avgPeriod = if (lengths.isEmpty()) defaultPeriod.coerceIn(1, 14) else lengths.average().roundToInt()

        val irregular = gaps.size >= 2 && (gaps.max() - gaps.min()) > 7
        val unusual = gaps.any { it < 21 || it > 35 }
        val confidence = when {
            irregular || gaps.isEmpty() -> Confidence.LOW
            gaps.size < 3 -> Confidence.MEDIUM
            else -> Confidence.HIGH
        }
        return CycleStats(
            avgCycleLength = avgCycle,
            avgPeriodLength = avgPeriod,
            cyclesUsed = gaps.size,
            shortestCycle = gaps.minOrNull() ?: avgCycle,
            longestCycle = gaps.maxOrNull() ?: avgCycle,
            isIrregular = irregular,
            hasUnusualLength = unusual,
            confidence = confidence
        )
    }

    fun predict(
        periods: List<PeriodSpan>,
        today: LocalDate,
        defaultCycle: Int = DEFAULT_CYCLE_LENGTH,
        defaultPeriod: Int = DEFAULT_PERIOD_LENGTH
    ): CyclePrediction {
        val past = periods.filter { !it.start.isAfter(today) }
        val stats = stats(past, defaultCycle, defaultPeriod)
        val last = past.maxByOrNull { it.start }
            ?: return CyclePrediction(stats, null, null, null, false, null, null, null, null, null, null, 0, false)

        val periodLength = stats.avgPeriodLength
        val cycleDay = ChronoUnit.DAYS.between(last.start, today).toInt() + 1
        val lastEnd = last.end
        val inPeriodNow = if (lastEnd != null) !today.isAfter(lastEnd) else cycleDay <= periodLength

        val nextStart = last.start.plusDays(stats.avgCycleLength.toLong())
        val nextEnd = nextStart.plusDays((periodLength - 1).toLong())

        // ovulation sits about 14 days before the next period, but never inside this cycle's period
        val earliestOvulation = last.start.plusDays(periodLength.toLong())
        var ovulation = nextStart.minusDays(LUTEAL_LENGTH.toLong())
        if (ovulation.isBefore(earliestOvulation)) ovulation = earliestOvulation
        val fertileStart = max(ovulation.minusDays(5).toEpochDay(), earliestOvulation.toEpochDay()).let { LocalDate.ofEpochDay(it) }
        val fertileEnd = ovulation.plusDays(1)

        val daysUntil = ChronoUnit.DAYS.between(today, nextStart).toInt()
        val daysLate = if (daysUntil < 0) -daysUntil else 0
        val phase = when {
            inPeriodNow -> CyclePhase.MENSTRUAL
            today.isBefore(fertileStart) -> CyclePhase.FOLLICULAR
            !today.isAfter(fertileEnd) -> CyclePhase.OVULATION
            else -> CyclePhase.LUTEAL
        }
        return CyclePrediction(
            stats = stats,
            lastPeriodStart = last.start,
            cycleDay = cycleDay,
            phase = phase,
            inPeriodNow = inPeriodNow,
            nextPeriodStart = nextStart,
            nextPeriodEnd = nextEnd,
            ovulationDate = ovulation,
            fertileStart = fertileStart,
            fertileEnd = fertileEnd,
            daysUntilNextPeriod = daysUntil,
            daysLate = daysLate,
            isStale = daysLate > STALE_AFTER_LATE_DAYS
        )
    }

    /**
     * What each calendar day between [from] and [to] shows: logged period days, and for the next four cycles the predicted
     * period, fertile window and ovulation day. Predictions are hidden when there is nothing to base them on or they are stale.
     */
    fun marksForRange(
        periods: List<PeriodSpan>,
        prediction: CyclePrediction,
        today: LocalDate,
        from: LocalDate,
        to: LocalDate
    ): Map<LocalDate, DayMarks> {
        val marks = HashMap<LocalDate, DayMarks>()
        fun mark(day: LocalDate, change: (DayMarks) -> DayMarks) {
            if (!day.isBefore(from) && !day.isAfter(to)) marks[day] = change(marks[day] ?: DayMarks())
        }

        val periodLength = prediction.stats.avgPeriodLength
        for (p in periods) {
            val end = p.end ?: minOf(today, p.start.plusDays((periodLength - 1).toLong()))
            var d = p.start
            var guard = 0
            while (!d.isAfter(end) && guard++ < 20) {
                mark(d) { it.copy(period = true) }
                d = d.plusDays(1)
            }
        }

        val nextStart = prediction.nextPeriodStart
        val ovulation = prediction.ovulationDate
        if (!prediction.isStale && nextStart != null && ovulation != null && prediction.fertileStart != null) {
            val cycle = prediction.stats.avgCycleLength.toLong()
            for (k in 0L..3L) {
                val periodStart = nextStart.plusDays(k * cycle)
                for (i in 0 until periodLength) mark(periodStart.plusDays(i.toLong())) { it.copy(predictedPeriod = true) }
                val ov = ovulation.plusDays(k * cycle)
                val fertileFrom = prediction.fertileStart.plusDays(k * cycle)
                var d = fertileFrom
                while (!d.isAfter(ov.plusDays(1))) {
                    mark(d) { it.copy(fertile = true) }
                    d = d.plusDays(1)
                }
                mark(ov) { it.copy(ovulation = true) }
            }
        }
        return marks
    }
}
