package com.example.data.cycle

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleTextsTest {
    private fun d(s: String) = LocalDate.parse(s)
    private val log = listOf(PeriodSpan(d("2026-10-01"), d("2026-10-05")))
    private fun on(day: String) = CyclePredictor.predict(log, d(day))

    @Test
    fun withoutALoggedPeriodTheCardAsksForOne() {
        val p = CyclePredictor.predict(emptyList(), d("2026-10-13"))
        assertEquals("Log your last period", CycleTexts.headline(p))
        assertEquals("Log your last period", CycleTexts.headline(null))
        assertTrue(CycleTexts.subline(p).contains("after you log a period"))
    }

    @Test
    fun theHeadlineShowsTheDayAndPhase() {
        assertEquals("Day 8 \u00B7 Follicular phase", CycleTexts.headline(on("2026-10-08")))
        assertEquals("Day 13 \u00B7 Fertile window", CycleTexts.headline(on("2026-10-13")))
        assertEquals("Day 3 \u00B7 Period", CycleTexts.headline(on("2026-10-03")))
    }

    @Test
    fun theSublineCountsDownThenReportsALatePeriod() {
        assertEquals("Next period in 16 days (Oct 29).", CycleTexts.subline(on("2026-10-13")))
        assertEquals("Your period is expected tomorrow.", CycleTexts.subline(on("2026-10-28")))
        assertEquals("Your period is expected today.", CycleTexts.subline(on("2026-10-29")))
        assertEquals("Your period is 1 day later than expected.", CycleTexts.subline(on("2026-10-30")))
        assertEquals("Your period is 4 days later than expected.", CycleTexts.subline(on("2026-11-02")))
        assertTrue(CycleTexts.subline(on("2027-02-01")).contains("Log your latest period"))
    }

    @Test
    fun theBasisSaysHowMuchHistoryTheNumbersRestOn() {
        assertTrue(CycleTexts.basis(CyclePredictor.stats(emptyList())).contains("typical values"))
        val three = CyclePredictor.stats(listOf("2026-05-01", "2026-05-29", "2026-06-26", "2026-07-24").map { PeriodSpan(d(it)) })
        assertEquals("Based on your last 3 logged cycles.", CycleTexts.basis(three))
        val one = CyclePredictor.stats(listOf("2026-05-01", "2026-05-29").map { PeriodSpan(d(it)) })
        assertEquals("Based on 1 logged cycle.", CycleTexts.basis(one))
    }

    @Test
    fun regularityNotesAreGentleAndOnlyWhenNeeded() {
        val regular = CyclePredictor.stats(listOf("2026-05-01", "2026-05-29", "2026-06-26").map { PeriodSpan(d(it)) })
        assertNull(CycleTexts.regularityNote(regular))
        val irregular = CyclePredictor.stats(listOf("2026-05-01", "2026-05-25", "2026-06-29", "2026-07-27").map { PeriodSpan(d(it)) })
        val note = CycleTexts.regularityNote(irregular)!!
        assertTrue(note.contains("consider talking to a doctor"))
        assertFalse(note.lowercase().contains("pregnan"))
        val short = CyclePredictor.stats(listOf("2026-05-01", "2026-05-20", "2026-06-08").map { PeriodSpan(d(it)) })
        assertNotNull(CycleTexts.regularityNote(short))
    }

    @Test
    fun theDisclaimerRulesOutMedicalAndContraceptiveUse() {
        assertTrue(CycleTexts.DISCLAIMER.contains("not a medical device"))
        assertTrue(CycleTexts.DISCLAIMER.contains("contraception"))
    }

    @Test
    fun periodsAreListedAsDateRanges() {
        assertEquals("Oct 1 \u2013 Oct 5", CycleTexts.period(d("2026-10-01"), d("2026-10-05")))
        assertTrue(CycleTexts.period(d("2026-10-01"), null).contains("ongoing"))
    }
}
