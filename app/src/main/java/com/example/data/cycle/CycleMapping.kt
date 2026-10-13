package com.example.data.cycle

import com.example.data.local.PeriodLog
import java.time.LocalDate

/** The logged period as a date span; null when the stored start date cannot be read. */
fun PeriodLog.toSpan(): PeriodSpan? {
    val start = try { LocalDate.parse(startDate) } catch (e: Exception) { return null }
    val end = if (endDate.isBlank()) null else try { LocalDate.parse(endDate) } catch (e: Exception) { null }
    return PeriodSpan(start, end)
}
