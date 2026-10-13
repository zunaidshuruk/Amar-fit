package com.example.data.cycle

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

data class DateRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
    fun days(): List<LocalDate> {
        val list = ArrayList<LocalDate>()
        var d = start
        while (!d.isAfter(end)) {
            list.add(d)
            d = d.plusDays(1)
        }
        return list
    }
}

/**
 * Ramadan dates from the Umm al-Qura calendar built into Java. Local moon sighting can start Ramadan a day later (or earlier),
 * so every date can be shifted by [offsetDays], which the person sets in Cycle settings.
 */
object Ramadan {
    fun range(hijriYear: Int, offsetDays: Int = 0): DateRange? = try {
        val first = HijrahDate.of(hijriYear, 9, 1)
        val start = LocalDate.from(first).plusDays(offsetDays.toLong())
        DateRange(start, start.plusDays((first.lengthOfMonth() - 1).toLong()))
    } catch (e: Exception) {
        null
    }

    fun hijriYearOf(date: LocalDate): Int = HijrahDate.from(date).get(ChronoField.YEAR)

    /** The Ramadan that is under way or the next one that has not ended yet, with its Hijri year. */
    fun currentOrNext(today: LocalDate, offsetDays: Int = 0): Pair<Int, DateRange> {
        var year = hijriYearOf(today)
        var range = range(year, offsetDays)
        if (range == null || today.isAfter(range.end)) {
            year += 1
            range = range(year, offsetDays)
        }
        return year to (range ?: DateRange(today, today))
    }

    fun daysUntil(range: DateRange, today: LocalDate): Int = ChronoUnit.DAYS.between(today, range.start).toInt().coerceAtLeast(0)
}

enum class FastingType(val code: Int, val label: String) {
    NONE(0, "None"),
    MISSED_PERIOD(1, "Missed fast (period)"),
    MISSED_OTHER(2, "Missed fast (other reason)"),
    MADE_UP(3, "Made up a fast");

    companion object {
        fun fromCode(code: Int): FastingType = values().firstOrNull { it.code == code } ?: NONE
    }
}

data class FastingEntry(val date: LocalDate, val type: FastingType)

data class RamadanYearLedger(
    val hijriYear: Int,
    val range: DateRange,
    val missed: Int,
    /** Made-up fasts counted against this Ramadan (the oldest Ramadan is made up first). */
    val madeUp: Int,
    val outstanding: Int,
    /** Days of a logged period that fall inside this Ramadan and are not yet counted as missed fasts. */
    val suggestedPeriodDays: List<LocalDate>
)

data class FastingLedger(
    val years: List<RamadanYearLedger>,
    val totalOutstanding: Int,
    val nextRamadan: DateRange,
    val daysUntilNextRamadan: Int
)

object FastingLedgerBuilder {
    private const val YEARS_BACK = 3

    fun build(entries: List<FastingEntry>, periods: List<PeriodSpan>, today: LocalDate, offsetDays: Int = 0): FastingLedger {
        val (currentYear, nextRange) = Ramadan.currentOrNext(today, offsetDays)
        val entryByDate = entries.associateBy { it.date }
        val missedDates = entries.filter { it.type == FastingType.MISSED_PERIOD || it.type == FastingType.MISSED_OTHER }.map { it.date }.toSet()
        var madeUpLeft = entries.count { it.type == FastingType.MADE_UP }

        val periodDays = HashSet<LocalDate>()
        for (p in periods) {
            val end = p.end ?: minOf(today, p.start.plusDays(9))
            var d = p.start
            var guard = 0
            while (!d.isAfter(end) && !d.isAfter(today) && guard++ < 20) {
                periodDays.add(d)
                d = d.plusDays(1)
            }
        }

        val years = ArrayList<RamadanYearLedger>()
        for (year in (currentYear - YEARS_BACK)..currentYear) {
            val range = Ramadan.range(year, offsetDays) ?: continue
            if (range.start.isAfter(today)) continue // not started yet: nothing can be missed
            val days = range.days()
            val missed = days.count { it in missedDates }
            val suggested = days.filter { it in periodDays && it !in entryByDate && !it.isAfter(today) }
            if (missed == 0 && suggested.isEmpty()) continue
            val take = minOf(missed, madeUpLeft)
            madeUpLeft -= take
            years.add(RamadanYearLedger(year, range, missed, take, missed - take, suggested))
        }
        return FastingLedger(
            years = years,
            totalOutstanding = years.sumOf { it.outstanding },
            nextRamadan = nextRange,
            daysUntilNextRamadan = Ramadan.daysUntil(nextRange, today)
        )
    }
}

object FastingNotices {
    /** The make-up reminder text, or null when nothing is left to make up. */
    fun makeUp(ledger: FastingLedger): CycleNotice? {
        val n = ledger.totalOutstanding
        if (n <= 0) return null
        return CycleNotice(
            CycleNoticeKind.FASTING_MAKEUP,
            "Fasting make-up",
            "You have $n fast${if (n == 1) "" else "s"} left to make up. Tomorrow could be a good day to fast."
        )
    }
}
