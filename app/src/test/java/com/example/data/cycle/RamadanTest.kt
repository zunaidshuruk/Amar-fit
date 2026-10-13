package com.example.data.cycle

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RamadanTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun entry(date: String, type: FastingType) = FastingEntry(d(date), type)

    // ---- dates
    @Test
    fun ramadanDatesComeFromTheUmmAlQuraCalendar() {
        assertEquals(DateRange(d("2026-02-18"), d("2026-03-19")), Ramadan.range(1447))
        assertEquals(DateRange(d("2027-02-08"), d("2027-03-08")), Ramadan.range(1448))
        assertEquals(30, Ramadan.range(1447)!!.days().size)
        assertEquals(29, Ramadan.range(1446)!!.days().size)
    }

    @Test
    fun theMoonSightingAdjustmentShiftsTheWholeMonth() {
        assertEquals(DateRange(d("2026-02-19"), d("2026-03-20")), Ramadan.range(1447, 1))
        assertEquals(DateRange(d("2026-02-17"), d("2026-03-18")), Ramadan.range(1447, -1))
    }

    @Test
    fun theCurrentOrNextRamadanFollowsTheDate() {
        assertEquals(1447 to DateRange(d("2026-02-18"), d("2026-03-19")), Ramadan.currentOrNext(d("2026-01-05")))
        assertEquals(1447 to DateRange(d("2026-02-18"), d("2026-03-19")), Ramadan.currentOrNext(d("2026-03-01")))
        assertEquals(1447 to DateRange(d("2026-02-18"), d("2026-03-19")), Ramadan.currentOrNext(d("2026-03-19")))
        assertEquals(1448 to DateRange(d("2027-02-08"), d("2027-03-08")), Ramadan.currentOrNext(d("2026-03-20")))
        assertEquals(1448 to DateRange(d("2027-02-08"), d("2027-03-08")), Ramadan.currentOrNext(d("2026-10-13")))
    }

    @Test
    fun daysUntilNextRamadanNeverGoesBelowZero() {
        assertEquals(118, Ramadan.daysUntil(Ramadan.range(1448)!!, d("2026-10-13")))
        assertEquals(0, Ramadan.daysUntil(Ramadan.range(1447)!!, d("2026-03-01")))
    }

    @Test
    fun dateRangeContainsItsEdges() {
        val r = DateRange(d("2026-02-18"), d("2026-02-20"))
        assertTrue(d("2026-02-18") in r)
        assertTrue(d("2026-02-20") in r)
        assertFalse(d("2026-02-21") in r)
        assertFalse(d("2026-02-17") in r)
        assertEquals(3, r.days().size)
    }

    // ---- make-up ledger
    @Test
    fun missedAndMadeUpFastsGiveWhatIsLeft() {
        val entries = listOf(
            entry("2026-02-20", FastingType.MISSED_PERIOD), entry("2026-02-21", FastingType.MISSED_PERIOD),
            entry("2026-02-22", FastingType.MISSED_OTHER), entry("2026-04-05", FastingType.MADE_UP)
        )
        val ledger = FastingLedgerBuilder.build(entries, emptyList(), d("2026-04-10"))
        assertEquals(2, ledger.totalOutstanding)
        val year = ledger.years.single()
        assertEquals(1447, year.hijriYear)
        assertEquals(3, year.missed)
        assertEquals(1, year.madeUp)
        assertEquals(2, year.outstanding)
    }

    @Test
    fun theOldestRamadanIsMadeUpFirst() {
        val entries = listOf(
            entry("2025-03-05", FastingType.MISSED_PERIOD), entry("2025-03-06", FastingType.MISSED_PERIOD),
            entry("2026-02-20", FastingType.MISSED_PERIOD),
            entry("2025-04-20", FastingType.MADE_UP), entry("2025-05-01", FastingType.MADE_UP)
        )
        val ledger = FastingLedgerBuilder.build(entries, emptyList(), d("2026-04-10"))
        val y1446 = ledger.years.first { it.hijriYear == 1446 }
        val y1447 = ledger.years.first { it.hijriYear == 1447 }
        assertEquals(0, y1446.outstanding)
        assertEquals(1, y1447.outstanding)
        assertEquals(1, ledger.totalOutstanding)
    }

    @Test
    fun moreMadeUpFastsThanMissedLeavesNothing() {
        val entries = listOf(
            entry("2026-02-20", FastingType.MISSED_PERIOD), entry("2026-02-21", FastingType.MISSED_PERIOD),
            entry("2026-04-01", FastingType.MADE_UP), entry("2026-04-02", FastingType.MADE_UP), entry("2026-04-03", FastingType.MADE_UP),
            entry("2026-04-04", FastingType.MADE_UP), entry("2026-04-05", FastingType.MADE_UP)
        )
        assertEquals(0, FastingLedgerBuilder.build(entries, emptyList(), d("2026-04-10")).totalOutstanding)
    }

    @Test
    fun periodDaysInsideRamadanAreSuggestedButNotCountedUntilConfirmed() {
        val periods = listOf(PeriodSpan(d("2026-02-17"), d("2026-02-22")))
        val ledger = FastingLedgerBuilder.build(emptyList(), periods, d("2026-04-10"))
        val year = ledger.years.single()
        assertEquals(0, year.missed)
        assertEquals(5, year.suggestedPeriodDays.size) // Feb 18 to Feb 22; Feb 17 is before Ramadan
        assertEquals(d("2026-02-18"), year.suggestedPeriodDays.first())
        assertEquals(0, ledger.totalOutstanding)
    }

    @Test
    fun confirmedDaysAreNoLongerSuggested() {
        val periods = listOf(PeriodSpan(d("2026-02-17"), d("2026-02-22")))
        val entries = listOf(entry("2026-02-19", FastingType.MISSED_PERIOD))
        val year = FastingLedgerBuilder.build(entries, periods, d("2026-04-10")).years.single()
        assertEquals(1, year.missed)
        assertEquals(4, year.suggestedPeriodDays.size)
        assertFalse(d("2026-02-19") in year.suggestedPeriodDays)
    }

    @Test
    fun duringRamadanAnOngoingPeriodIsSuggestedUpToToday() {
        val periods = listOf(PeriodSpan(d("2026-02-18")))
        val year = FastingLedgerBuilder.build(emptyList(), periods, d("2026-02-20")).years.single()
        assertEquals(listOf(d("2026-02-18"), d("2026-02-19"), d("2026-02-20")), year.suggestedPeriodDays)
    }

    @Test
    fun beforeRamadanThereIsNothingToMakeUp() {
        val periods = listOf(PeriodSpan(d("2026-02-18"), d("2026-02-22")))
        val ledger = FastingLedgerBuilder.build(emptyList(), periods, d("2026-01-05"))
        assertTrue(ledger.years.isEmpty())
        assertEquals(0, ledger.totalOutstanding)
        assertEquals(d("2026-02-18"), ledger.nextRamadan.start)
        assertEquals(44, ledger.daysUntilNextRamadan)
    }

    @Test
    fun theLedgerFollowsTheMoonSightingAdjustment() {
        val entries = listOf(entry("2026-03-20", FastingType.MISSED_OTHER)) // the last day only with +1
        assertEquals(0, FastingLedgerBuilder.build(entries, emptyList(), d("2026-04-10"), 0).totalOutstanding)
        assertEquals(1, FastingLedgerBuilder.build(entries, emptyList(), d("2026-04-10"), 1).totalOutstanding)
    }

    @Test
    fun theMakeUpReminderCountsWhatIsLeft() {
        val one = FastingLedgerBuilder.build(listOf(entry("2026-02-20", FastingType.MISSED_PERIOD)), emptyList(), d("2026-04-10"))
        val notice = FastingNotices.makeUp(one)!!
        assertEquals(CycleNoticeKind.FASTING_MAKEUP, notice.kind)
        assertTrue(notice.message, notice.message.contains("1 fast left"))
        val three = FastingLedgerBuilder.build(
            listOf("2026-02-20", "2026-02-21", "2026-02-22").map { entry(it, FastingType.MISSED_PERIOD) }, emptyList(), d("2026-04-10")
        )
        assertTrue(FastingNotices.makeUp(three)!!.message.contains("3 fasts left"))
        assertNull(FastingNotices.makeUp(FastingLedgerBuilder.build(emptyList(), emptyList(), d("2026-04-10"))))
    }

    @Test
    fun fastingCodesRoundTrip() {
        FastingType.values().forEach { assertEquals(it, FastingType.fromCode(it.code)) }
        assertEquals(FastingType.NONE, FastingType.fromCode(99))
        assertNotNull(FastingType.MADE_UP.label)
    }
}
