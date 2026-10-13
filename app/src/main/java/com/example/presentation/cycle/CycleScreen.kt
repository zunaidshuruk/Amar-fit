package com.example.presentation.cycle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cycle.DayMarks
import com.example.data.cycle.CyclePredictor
import com.example.data.cycle.CycleTexts
import com.example.data.cycle.FlowLevel
import com.example.data.cycle.Mood
import com.example.data.cycle.Symptoms
import com.example.data.cycle.toSpan
import com.example.data.local.CycleDayLog
import com.example.data.local.PeriodLog
import com.example.presentation.viewmodel.ShasthoViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CycleScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val available by viewModel.cycleAvailable.collectAsState()
    val active by viewModel.cycleActive.collectAsState()
    val prediction by viewModel.cyclePrediction.collectAsState()
    val periods by viewModel.periodLogs.collectAsState()
    val loggedDays by viewModel.cycleDays.collectAsState()
    val todayStr by viewModel.todayDate.collectAsState()
    val today = remember(todayStr) { try { LocalDate.parse(todayStr) } catch (e: Exception) { LocalDate.now() } }

    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showConsent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Cycle tracker",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (active) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Cycle settings", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        if (!available) {
            InfoCard("The cycle tracker is available for profiles set to Female. You can change this in your profile.")
        } else if (!active) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Track your cycle", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Log your period and how you feel, see your cycle day and an estimate of your next period and fertile window, and get reminders you choose. It is private: only you can see it, and it is never shown to friends.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                    Button(onClick = { showConsent = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Turn on cycle tracking")
                    }
                }
            }
        } else {
            // ---- summary
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(CycleTexts.headline(prediction), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        CycleTexts.subline(prediction),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    val p = prediction
                    if (p != null && p.lastPeriodStart != null && !p.isStale && p.fertileStart != null && p.fertileEnd != null) {
                        Text(
                            "Estimated fertile window: ${p.fertileStart.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${p.fertileStart.dayOfMonth} to ${p.fertileEnd.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${p.fertileEnd.dayOfMonth}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (prediction?.inPeriodNow == true) {
                    Button(onClick = { viewModel.endPeriodOn(today) }, modifier = Modifier.weight(1f)) { Text("Period ended today") }
                } else {
                    Button(onClick = { viewModel.startPeriodOn(today) }, modifier = Modifier.weight(1f)) { Text("Period started today") }
                }
                OutlinedButton(onClick = { selectedDate = today }, modifier = Modifier.weight(1f)) { Text("Log today") }
            }

            Spacer(Modifier.height(16.dp))

            // ---- calendar
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { month = month.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                        }
                        Text(
                            "${month.month.getDisplayName(TextStyle.FULL, Locale.US)} ${month.year}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { month = month.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                            Text(
                                it,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    val spans = remember(periods) { periods.mapNotNull { it.toSpan() } }
                    val marks: Map<LocalDate, DayMarks> = remember(spans, prediction, month, today) {
                        val p = prediction
                        if (p == null) emptyMap() else CyclePredictor.marksForRange(spans, p, today, month.atDay(1), month.atEndOfMonth())
                    }
                    val loggedDates = remember(loggedDays) {
                        loggedDays.filter { it.flow > 0 || it.symptoms != 0 || it.mood != 0 }.map { it.date }.toSet()
                    }
                    val offset = month.atDay(1).dayOfWeek.value - 1
                    val rows = (offset + month.lengthOfMonth() + 6) / 7
                    for (r in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (c in 0..6) {
                                val index = r * 7 + c
                                val dayNumber = index - offset + 1
                                if (dayNumber < 1 || dayNumber > month.lengthOfMonth()) {
                                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val date = month.atDay(dayNumber)
                                    DayCell(
                                        date = date,
                                        marks = marks[date],
                                        hasLog = date.toString() in loggedDates,
                                        isToday = date == today,
                                        onClick = { selectedDate = date },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LegendDot(MaterialTheme.colorScheme.error, filled = true, label = "Period")
                        LegendDot(MaterialTheme.colorScheme.error, filled = false, label = "Expected period")
                        LegendDot(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), filled = true, label = "Fertile window")
                        LegendDot(MaterialTheme.colorScheme.primary, filled = false, label = "Ovulation (estimate)")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- basis, regularity and disclaimer
            val stats = prediction?.stats
            if (stats != null) {
                InfoCard(CycleTexts.basis(stats))
                CycleTexts.regularityNote(stats)?.let {
                    Spacer(Modifier.height(8.dp))
                    InfoCard(it)
                }
                Spacer(Modifier.height(8.dp))
            }

            // ---- history
            val sorted = remember(periods) { periods.mapNotNull { p -> p.toSpan()?.let { p to it } }.sortedBy { it.second.start } }
            if (sorted.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        val recent = sorted.reversed().take(6)
                        recent.forEachIndexed { index, (_, span) ->
                            val next = sorted.getOrNull(sorted.indexOfFirst { it.second.start == span.start } + 1)?.second?.start
                            val cycleLength = next?.let { java.time.temporal.ChronoUnit.DAYS.between(span.start, it).toInt() }
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(CycleTexts.period(span.start, span.end), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                val lengthText = span.end?.let { "${java.time.temporal.ChronoUnit.DAYS.between(span.start, it).toInt() + 1}-day period" }
                                val parts = listOfNotNull(lengthText, cycleLength?.let { "cycle $it days" })
                                if (parts.isNotEmpty()) {
                                    Text(parts.joinToString(" \u00B7 "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(
                CycleTexts.DISCLAIMER,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        Spacer(Modifier.height(80.dp))
    }

    selectedDate?.let { date ->
        DayLogDialog(
            viewModel = viewModel,
            date = date,
            periods = periods,
            onDismiss = { selectedDate = null }
        )
    }
    if (showConsent) {
        CycleConsentDialog(
            onDismiss = { showConsent = false },
            onConfirm = { ai, discreet ->
                showConsent = false
                viewModel.enableCycleTracking(ai, discreet, 28, 5)
            }
        )
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun LegendDot(color: Color, filled: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .then(if (filled) Modifier.background(color) else Modifier.border(BorderStroke(1.5.dp, color), CircleShape))
        )
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    marks: DayMarks?,
    hasLog: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val fill = when {
        marks?.period == true -> scheme.error
        marks?.fertile == true -> scheme.primary.copy(alpha = 0.25f)
        else -> Color.Transparent
    }
    val textColor = if (marks?.period == true) scheme.onError else scheme.onSurface
    val border = when {
        isToday -> BorderStroke(1.5.dp, scheme.onSurface)
        marks?.ovulation == true -> BorderStroke(2.dp, scheme.primary)
        marks?.predictedPeriod == true -> BorderStroke(1.5.dp, scheme.error)
        else -> null
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(fill)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(date.dayOfMonth.toString(), fontSize = 13.sp, color = textColor)
        if (hasLog) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
        }
    }
}

/** Log one day: flow, symptoms and mood, and mark the day as the start or end of a period. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DayLogDialog(
    viewModel: ShasthoViewModel,
    date: LocalDate,
    periods: List<PeriodLog>,
    onDismiss: () -> Unit
) {
    var flow by remember(date) { mutableIntStateOf(0) }
    var symptoms by remember(date) { mutableIntStateOf(0) }
    var mood by remember(date) { mutableIntStateOf(0) }
    var loaded by remember(date) { mutableStateOf(false) }
    var confirmRemove by remember(date) { mutableStateOf<PeriodLog?>(null) }

    LaunchedEffect(date) {
        viewModel.getCycleDay(date)?.let {
            flow = it.flow
            symptoms = it.symptoms
            mood = it.mood
        }
        loaded = true
    }

    val covering = periods.firstOrNull { p ->
        val span = p.toSpan() ?: return@firstOrNull false
        !date.isBefore(span.start) && (if (span.end == null) date == span.start else !date.isAfter(span.end))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${date.month.getDisplayName(TextStyle.FULL, Locale.US)} ${date.dayOfMonth}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Flow", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowLevel.values().forEach { level ->
                        FilterChip(selected = flow == level.code, onClick = { flow = level.code }, label = { Text(level.label) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Symptoms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Symptoms.ALL.forEach { s ->
                        FilterChip(
                            selected = Symptoms.has(symptoms, s.bit),
                            onClick = { symptoms = Symptoms.toggle(symptoms, s.bit) },
                            label = { Text(s.label) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Mood", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mood.values().filter { it != Mood.NOT_SET }.forEach { m ->
                        FilterChip(
                            selected = mood == m.code,
                            onClick = { mood = if (mood == m.code) Mood.NOT_SET.code else m.code },
                            label = { Text(m.label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(onClick = { viewModel.startPeriodOn(date); onDismiss() }) { Text("Period started on this day") }
                TextButton(onClick = { viewModel.endPeriodOn(date); onDismiss() }) { Text("Period ended on this day") }
                if (covering != null) {
                    TextButton(onClick = { confirmRemove = covering }) {
                        Text("Remove this period", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = loaded,
                onClick = {
                    viewModel.saveCycleDay(date, flow, symptoms, mood)
                    onDismiss()
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    confirmRemove?.let { period ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove this period?") },
            text = { Text("The period starting ${period.startDate} will be removed from your log and your predictions will change.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removePeriod(period.cloudId)
                    confirmRemove = null
                    onDismiss()
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text("Keep") } }
        )
    }
}

/** The consent screen: says exactly what is stored and where, with the AI switch visible and ON by default. */
@Composable
fun CycleConsentDialog(
    onDismiss: () -> Unit,
    onConfirm: (aiEnabled: Boolean, discreet: Boolean) -> Unit
) {
    var aiOn by remember { mutableStateOf(true) }
    var discreet by remember { mutableStateOf(true) }
    var agreed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turn on cycle tracking") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Cycle data is very personal, so here is exactly how it works.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "\u2022 What is stored: the dates of your periods and what you log each day (flow, symptoms, mood).\n" +
                        "\u2022 Where: on this phone and in your private area of your KardIQ account, so it follows you to your other phones. Only your account can read it. It is never shown to friends, the leaderboard or challenges.\n" +
                        "\u2022 Predictions are estimates. KardIQ is not a medical device and not contraception.\n" +
                        "\u2022 You can turn tracking off or delete all cycle data in Cycle settings at any time. Deleting your account deletes it too.\n" +
                        "\u2022 Details: Privacy Policy, section 1.6.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                ConsentSwitch(
                    title = "AI Assistant may use a short cycle summary",
                    subtitle = "Sends only your cycle day, phase and next period date to Google's Gemini when you ask health questions. Never your daily logs, symptoms or mood.",
                    checked = aiOn,
                    onChange = { aiOn = it }
                )
                ConsentSwitch(
                    title = "Hide details on the lock screen",
                    subtitle = "Cycle notifications show only \"You have a reminder\" until the phone is unlocked.",
                    checked = discreet,
                    onChange = { discreet = it }
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { agreed = !agreed }) {
                    Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                    Text("I understand and agree", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = agreed, onClick = { onConfirm(aiOn, discreet) }) { Text("Turn on") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }
    )
}

@Composable
private fun ConsentSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
