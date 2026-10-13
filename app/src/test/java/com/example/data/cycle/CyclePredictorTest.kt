package com.example.data.cycle

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CyclePredictorTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun starts(vararg s: String) = s.map { PeriodSpan(d(it)) }

    @Test
    fun withNothingLoggedThereIsNoPrediction() {
        val p = CyclePredictor.predict(emptyList(), d("2026-10-13"))
        assertNull(p.lastPeriodStart)
        assertNull(p.nextPeriodStart)
        assertNull(p.phase)
        assertEquals(28, p.stats.avgCycleLength)
        assertEquals(5, p.stats.avgPeriodLength)
        assertEquals(0, p.stats.cyclesUsed)
        assertEquals(Confidence.LOW, p.stats.confidence)
    }

    @Test
    fun onePeriodUsesTypicalValues() {
        val p = CyclePredictor.predict(starts("2026-10-01"), d("2026-10-01"))
        assertEquals(1, p.cycleDay)
        assertEquals(d("2026-10-29"), p.nextPeriodStart)
        assertEquals(d("2026-11-02"), p.nextPeriodEnd)
        assertEquals(d("2026-10-15"), p.ovulationDate)
        assertEquals(d("2026-10-10"), p.fertileStart)
        assertEquals(d("2026-10-16"), p.fertileEnd)
        assertEquals(28, p.daysUntilNextPeriod)
        assertEquals(0, p.daysLate)
    }

    @Test
    fun phasesFollowTheDayOfTheCycle() {
        val log = starts("2026-10-01")
        fun phase(day: String) = CyclePredictor.predict(log, d(day)).phase
        assertEquals(CyclePhase.MENSTRUAL, phase("2026-10-01"))
        assertEquals(CyclePhase.MENSTRUAL, phase("2026-10-05"))
        assertEquals(CyclePhase.FOLLICULAR, phase("2026-10-06"))
        assertEquals(CyclePhase.FOLLICULAR, phase("2026-10-09"))
        assertEquals(CyclePhase.OVULATION, phase("2026-10-10"))
        assertEquals(CyclePhase.OVULATION, phase("2026-10-16"))
        assertEquals(CyclePhase.LUTEAL, phase("2026-10-17"))
        assertEquals(CyclePhase.LUTEAL, phase("2026-10-28"))
    }

    @Test
    fun theAverageOfRecentCyclesIsUsed() {
        // starts 30 days apart three times
        val log = starts("2026-06-01", "2026-07-01", "2026-07-31", "2026-08-30")
        val p = CyclePredictor.predict(log, d("2026-09-05"))
        assertEquals(30, p.stats.avgCycleLength)
        assertEquals(3, p.stats.cyclesUsed)
        assertEquals(Confidence.HIGH, p.stats.confidence)
        assertFalse(p.stats.isIrregular)
        assertEquals(d("2026-09-29"), p.nextPeriodStart)
    }

    @Test
    fun onlyTheLastSixCyclesCount() {
        // an old 40 day cycle followed by six 28 day cycles
        val log = starts("2025-01-01", "2025-02-10", "2025-03-10", "2025-04-07", "2025-05-05", "2025-06-02", "2025-06-30", "2025-07-28")
        val p = CyclePredictor.predict(log, d("2025-08-01"))
        assertEquals(6, p.stats.cyclesUsed)
        assertEquals(28, p.stats.avgCycleLength)
    }

    @Test
    fun cyclesThatDifferByMoreThanAWeekAreIrregularAndLowConfidence() {
        val log = starts("2026-05-01", "2026-05-25", "2026-06-29", "2026-07-27") // 24, 35, 28
        val p = CyclePredictor.predict(log, d("2026-08-01"))
        assertTrue(p.stats.isIrregular)
        assertEquals(Confidence.LOW, p.stats.confidence)
        assertEquals(24, p.stats.shortestCycle)
        assertEquals(35, p.stats.longestCycle)
    }

    @Test
    fun unusuallyShortOrLongCyclesAreFlagged() {
        val short = CyclePredictor.stats(starts("2026-05-01", "2026-05-20", "2026-06-08"))
        assertTrue(short.hasUnusualLength)
        val normal = CyclePredictor.stats(starts("2026-05-01", "2026-05-29", "2026-06-26"))
        assertFalse(normal.hasUnusualLength)
        val long = CyclePredictor.stats(starts("2026-05-01", "2026-06-10", "2026-07-20"))
        assertTrue(long.hasUnusualLength)
    }

    @Test
    fun aGapOfThreeMonthsMeansAPeriodWasMissedAndIsIgnored() {
        val p = CyclePredictor.stats(starts("2026-01-01", "2026-05-01"))
        assertEquals(0, p.cyclesUsed)
        assertEquals(28, p.avgCycleLength)
    }

    @Test
    fun periodLengthComesFromPeriodsThatHaveAnEnd() {
        val log = listOf(
            PeriodSpan(d("2026-07-01"), d("2026-07-04")), // 4 days
            PeriodSpan(d("2026-07-29"), d("2026-08-03")), // 6 days
            PeriodSpan(d("2026-08-26"))                   // still going, not counted
        )
        assertEquals(5, CyclePredictor.stats(log).avgPeriodLength)
    }

    @Test
    fun yourOwnTypicalValuesAreUsedUntilThereIsHistory() {
        val p = CyclePredictor.predict(starts("2026-10-01"), d("2026-10-02"), defaultCycle = 32, defaultPeriod = 6)
        assertEquals(32, p.stats.avgCycleLength)
        assertEquals(6, p.stats.avgPeriodLength)
        assertEquals(d("2026-11-02"), p.nextPeriodStart)
    }

    @Test
    fun aLatePeriodIsReportedAsLateNotAsAnythingElse() {
        val p = CyclePredictor.predict(starts("2026-10-01"), d("2026-11-02"))
        assertEquals(4, p.daysLate)
        assertTrue(p.daysUntilNextPeriod!! < 0)
        assertFalse(p.isStale)
        assertEquals(CyclePhase.LUTEAL, p.phase)
    }

    @Test
    fun aVeryLatePredictionBecomesStale() {
        val p = CyclePredictor.predict(starts("2026-10-01"), d("2027-01-15"))
        assertTrue(p.isStale)
    }

    @Test
    fun ovulationNeverFallsInsideThePeriodForShortCycles() {
        val p = CyclePredictor.predict(starts("2026-10-01"), d("2026-10-02"), defaultCycle = 21, defaultPeriod = 7)
        assertEquals(d("2026-10-08"), p.ovulationDate) // not day 8 minus the luteal phase inside the period
        assertFalse(p.fertileStart!!.isBefore(d("2026-10-08")))
    }

    @Test
    fun futureDatedPeriodsAreIgnored() {
        val p = CyclePredictor.predict(starts("2026-09-10", "2026-12-01"), d("2026-10-01"))
        assertEquals(d("2026-09-10"), p.lastPeriodStart)
    }

    @Test
    fun aPeriodWithoutAnEndIsOverOnceItIsLongerThanTheAverage() {
        val log = starts("2026-10-01")
        assertTrue(CyclePredictor.predict(log, d("2026-10-05")).inPeriodNow)
        assertFalse(CyclePredictor.predict(log, d("2026-10-06")).inPeriodNow)
    }

    @Test
    fun aLoggedEndEndsThePeriodEvenIfShorterThanAverage() {
        val log = listOf(PeriodSpan(d("2026-10-01"), d("2026-10-03")))
        assertTrue(CyclePredictor.predict(log, d("2026-10-03")).inPeriodNow)
        assertFalse(CyclePredictor.predict(log, d("2026-10-04")).inPeriodNow)
    }

    // ---- calendar marks
    @Test
    fun calendarShowsLoggedAndPredictedDays() {
        val log = listOf(PeriodSpan(d("2026-10-01"), d("2026-10-05")))
        val today = d("2026-10-13")
        val p = CyclePredictor.predict(log, today)
        val marks = CyclePredictor.marksForRange(log, p, today, d("2026-10-01"), d("2026-11-30"))
        assertTrue(marks[d("2026-10-03")]!!.period)
        assertFalse(marks[d("2026-10-06")]?.period ?: false)
        assertTrue(marks[d("2026-10-10")]!!.fertile)
        assertTrue(marks[d("2026-10-15")]!!.ovulation)
        assertTrue(marks[d("2026-10-29")]!!.predictedPeriod)
        assertTrue(marks[d("2026-11-02")]!!.predictedPeriod)
        assertFalse(marks[d("2026-11-03")]?.predictedPeriod ?: false)
        // the next cycle repeats: fertile window around 2026-11-12
        assertTrue(marks[d("2026-11-12")]!!.ovulation)
    }

    @Test
    fun calendarHidesPredictionsWhenStale() {
        val log = listOf(PeriodSpan(d("2026-10-01"), d("2026-10-05")))
        val today = d("2027-02-01")
        val p = CyclePredictor.predict(log, today)
        val marks = CyclePredictor.marksForRange(log, p, today, d("2026-10-01"), d("2027-03-01"))
        assertTrue(marks.values.none { it.predictedPeriod || it.fertile || it.ovulation })
        assertTrue(marks[d("2026-10-02")]!!.period)
    }

    @Test
    fun anOngoingPeriodIsMarkedUpToToday() {
        val log = listOf(PeriodSpan(d("2026-10-12")))
        val today = d("2026-10-13")
        val p = CyclePredictor.predict(log, today)
        val marks = CyclePredictor.marksForRange(log, p, today, d("2026-10-10"), d("2026-10-20"))
        assertTrue(marks[d("2026-10-12")]!!.period)
        assertTrue(marks[d("2026-10-13")]!!.period)
        assertNotNull(marks[d("2026-10-13")])
    }

    @Test
    fun calendarOnlyReturnsTheRequestedRange() {
        val log = listOf(PeriodSpan(d("2026-10-01"), d("2026-10-05")))
        val today = d("2026-10-13")
        val marks = CyclePredictor.marksForRange(log, CyclePredictor.predict(log, today), today, d("2026-10-02"), d("2026-10-04"))
        assertEquals(setOf(d("2026-10-02"), d("2026-10-03"), d("2026-10-04")), marks.keys)
    }
}
