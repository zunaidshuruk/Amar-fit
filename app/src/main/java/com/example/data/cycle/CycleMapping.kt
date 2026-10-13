package com.example.data.cycle

import com.example.data.local.FastingLog
import com.example.data.local.PeriodLog
import java.time.LocalDate

/** The logged period as a date span; null when the stored start date cannot be read. */
fun PeriodLog.toSpan(): PeriodSpan? {
    val start = try { LocalDate.parse(startDate) } catch (e: Exception) { return null }
    val end = if (endDate.isBlank()) null else try { LocalDate.parse(endDate) } catch (e: Exception) { null }
    return PeriodSpan(start, end)
}

fun FastingLog.toEntry(): FastingEntry? {
    val day = try { LocalDate.parse(date) } catch (e: Exception) { return null }
    val kind = FastingType.fromCode(type)
    return if (kind == FastingType.NONE) null else FastingEntry(day, kind)
}
