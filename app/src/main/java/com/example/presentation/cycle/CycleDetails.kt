package com.example.presentation.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cycle.CyclePrediction
import com.example.data.cycle.CycleTexts
import com.example.data.cycle.FastingLedger
import com.example.data.cycle.FastingType
import com.example.data.cycle.Ramadan
import com.example.data.cycle.toSpan
import com.example.data.local.CycleDayLog
import com.example.data.local.PeriodLog
import com.example.presentation.viewmodel.ShasthoViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** The details view: predictions in words, averages, fasting make-up, every cycle and the recent daily logs. */
@Composable
internal fun DetailsView(
    viewModel: ShasthoViewModel,
    prediction: CyclePrediction?,
    periods: List<PeriodLog>,
    loggedDays: List<CycleDayLog>,
    ledger: FastingLedger?,
    today: LocalDate
) {
    val predictionLines = remember(prediction) { prediction?.let { CycleTexts.predictionLines(it) } ?: emptyList() }
    SettingsCard("Predictions") {
        if (predictionLines.isEmpty()) {
            Text(
                "Log your last period to see predictions here.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            predictionLines.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LabelValueRow(label, value)
            }
        }
    }

    val stats = prediction?.stats
    if (stats != null) {
        SettingsCard("Your averages") {
            CycleTexts.statsLines(stats).forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LabelValueRow(label, value)
            }
            Text(
                CycleTexts.basis(stats),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            CycleTexts.regularityNote(stats)?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

    if (ledger != null) {
        RamadanCard(viewModel = viewModel, ledger = ledger, today = today)
    }

    val sorted = remember(periods) { periods.mapNotNull { p -> p.toSpan()?.let { it } }.sortedByDescending { it.start } }
    SettingsCard("Cycle history") {
        if (sorted.isEmpty()) {
            Text("No periods logged yet.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            sorted.forEachIndexed { index, span ->
                // the cycle starting at this period ends where the next (newer) one starts
                val nextStart = sorted.getOrNull(index - 1)?.start
                val cycleLength = nextStart?.let { java.time.temporal.ChronoUnit.DAYS.between(span.start, it).toInt() }
                val periodLength = span.end?.let { java.time.temporal.ChronoUnit.DAYS.between(span.start, it).toInt() + 1 }
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(CycleTexts.period(span.start, span.end), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    val parts = listOfNotNull(periodLength?.let { "$it-day period" }, cycleLength?.let { "cycle $it days" })
                    if (parts.isNotEmpty()) {
                        Text(parts.joinToString(" \u00B7 "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    val recent = remember(loggedDays) {
        loggedDays.sortedByDescending { it.date }.mapNotNull { day ->
            val date = try { LocalDate.parse(day.date) } catch (e: Exception) { null }
            if (date == null) null else CycleTexts.dayLine(date, day.flow, day.symptoms, day.mood)
        }.take(14)
    }
    SettingsCard("Recent days") {
        if (recent.isEmpty()) {
            Text(
                "Nothing logged yet. Tap a day in the Calendar to log flow, symptoms or mood.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            recent.forEachIndexed { index, line ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(line, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }

    Text(
        CycleTexts.DISCLAIMER,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
    }
}

/** Optional Ramadan fasting make-up: what was missed (for example on period days), what was made up, and what is left. */
@Composable
private fun RamadanCard(viewModel: ShasthoViewModel, ledger: FastingLedger, today: LocalDate) {
    val fastingLogs by viewModel.fastingLogs.collectAsState()
    val madeUpToday = fastingLogs.any { it.date == today.toString() && it.type == FastingType.MADE_UP.code }

    SettingsCard("Ramadan fasting make-up") {
        val next = ledger.nextRamadan
        val nextLabel = "Ramadan ${Ramadan.hijriYearOf(next.start)}: ${next.start.format(shortDate)} \u2013 ${next.end.format(shortDate)}, ${next.end.year}"
        Text(
            if (ledger.daysUntilNextRamadan > 0) "$nextLabel (in ${ledger.daysUntilNextRamadan} days)" else "$nextLabel (now)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val left = ledger.totalOutstanding
        Text(
            if (left == 0) "Nothing left to make up" else "$left fast${if (left == 1) "" else "s"} left to make up",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (left == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        ledger.years.sortedByDescending { it.hijriYear }.forEach { year ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 8.dp))
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    "Ramadan ${year.hijriYear} (${year.range.start.format(shortDate)} \u2013 ${year.range.end.format(shortDate)}, ${year.range.end.year})",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Missed ${year.missed} \u00B7 made up ${year.madeUp} \u00B7 ${year.outstanding} left",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (year.suggestedPeriodDays.isNotEmpty()) {
                    val n = year.suggestedPeriodDays.size
                    Text(
                        "$n period day${if (n == 1) "" else "s"} in this Ramadan ${if (n == 1) "is" else "are"} not counted yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    OutlinedButton(onClick = { viewModel.countAsMissedFasts(year.suggestedPeriodDays) }) {
                        Text("Count them as missed fasts")
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        if (madeUpToday) {
            Text("Today is counted as a made-up fast.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = { viewModel.setFasting(today, FastingType.NONE) }) { Text("Undo") }
        } else {
            Button(onClick = { viewModel.setFasting(today, FastingType.MADE_UP) }) { Text("I made up a fast today") }
        }
        Text(
            "To mark a missed or made-up fast on another day, tap that day in the Calendar.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
