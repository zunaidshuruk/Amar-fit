package com.example.presentation.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import com.example.data.health.HealthGoalCalculator
import com.example.data.local.DailyMetric
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailScreen(
    viewModel: ShasthoViewModel,
    metricKey: String,
    onNavigateBack: () -> Unit = {}
) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()

    var selectedRange by remember { mutableStateOf("W") }
    var periodOffset by remember(selectedRange) { mutableStateOf(0) }

    val historyFlow = remember(selectedRange, periodOffset) {
        viewModel.getMetricsHistoryFlowForPeriod(selectedRange, periodOffset)
    }
    val history by historyFlow.collectAsState(initial = emptyList())
    val chronologicalData = remember(history) { history.reversed() }

    val selectedDayDate = remember(periodOffset) {
        LocalDate.now().minusDays(periodOffset.toLong())
    }

    val heartRateDaySamples by produceState<List<Pair<Instant, Int>>>(
        initialValue = emptyList(),
        key1 = metricKey,
        key2 = selectedRange,
        key3 = selectedDayDate
    ) {
        if (metricKey == "heartRate" && selectedRange == "D") {
            value = viewModel.getHeartRateSamplesForDate(selectedDayDate)
        } else {
            value = emptyList()
        }
    }

    val periodLabel = remember(selectedRange, periodOffset) {
        val windowDays = when (selectedRange) {
            "D" -> 1
            "W" -> 7
            "M" -> 30
            "3M" -> 90
            "Y" -> 365
            else -> 7
        }
        val today = LocalDate.now()
        val endDate = today.minusDays((periodOffset.toLong()) * windowDays)
        val startDate = endDate.minusDays((windowDays - 1).toLong())

        when (selectedRange) {
            "D" -> {
                if (periodOffset == 0) "Today"
                else if (startDate == today.minusDays(1)) "Yesterday"
                else startDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
            }
            "W", "M" -> {
                if (startDate.year != endDate.year) {
                    "${startDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))} - ${endDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))}"
                } else if (startDate.month == endDate.month) {
                    "${startDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))} - ${endDate.dayOfMonth}"
                } else {
                    "${startDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))} - ${endDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))}"
                }
            }
            "3M", "Y" -> {
                if (startDate.year != endDate.year) {
                    "${startDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))} - ${endDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))}"
                } else {
                    "${startDate.format(DateTimeFormatter.ofPattern("MMM", Locale.US))} - ${endDate.format(DateTimeFormatter.ofPattern("MMM", Locale.US))} ${endDate.year}"
                }
            }
            else -> ""
        }
    }

    val metricTitle = when (metricKey) {
        "steps" -> "Steps"
        "activeCaloriesBurned" -> "Active Calories Burned"
        "caloriesConsumed" -> "Calories Consumed"
        "carbsG" -> "Carbs"
        "proteinG" -> "Protein"
        "fatG" -> "Fat"
        "waterLiters" -> "Water"
        "exerciseDays" -> "Exercise Days"
        "heartRate" -> "Heart Rate"
        "oxygenSaturation" -> "Blood Oxygen (SpO2)"
        "heartRateVariability" -> "Heart Rate Variability (HRV)"
        "skinTemperatureCelsius" -> "Skin Temperature"
        "respiratoryRate" -> "Respiratory Rate"
        else -> "Metric Detail"
    }

    val hasData = remember(chronologicalData, metricKey) {
        chronologicalData.any {
            when (metricKey) {
                "steps" -> it.steps > 0
                "activeCaloriesBurned" -> it.activeCaloriesBurned > 0
                "caloriesConsumed" -> it.caloriesConsumed > 0
                "carbsG" -> it.carbsG > 0f
                "proteinG" -> it.proteinG > 0f
                "fatG" -> it.fatG > 0f
                "waterLiters" -> it.waterLiters > 0f
                "exerciseDays" -> it.exerciseMinutes > 0
                "heartRate" -> it.heartRate > 0
                "oxygenSaturation" -> it.oxygenSaturation > 0
                "heartRateVariability" -> it.heartRateVariability > 0
                "skinTemperatureCelsius" -> it.skinTemperatureCelsius > 0
                "respiratoryRate" -> it.respiratoryRate > 0
                else -> false
            }
        }
    }

    val latestVal = chronologicalData.lastOrNull {
        when (metricKey) {
            "steps" -> it.steps > 0
            "activeCaloriesBurned" -> it.activeCaloriesBurned > 0
            "caloriesConsumed" -> it.caloriesConsumed > 0
            "carbsG" -> it.carbsG > 0f
            "proteinG" -> it.proteinG > 0f
            "fatG" -> it.fatG > 0f
            "waterLiters" -> it.waterLiters > 0f
            "exerciseDays" -> it.exerciseMinutes > 0
            "heartRate" -> it.heartRate > 0
            "oxygenSaturation" -> it.oxygenSaturation > 0
            "heartRateVariability" -> it.heartRateVariability > 0
            "skinTemperatureCelsius" -> it.skinTemperatureCelsius > 0
            "respiratoryRate" -> it.respiratoryRate > 0
            else -> false
        }
    }

    val displayValue = if (latestVal != null) {
        when (metricKey) {
            "steps" -> "${latestVal.steps} steps"
            "activeCaloriesBurned" -> "${latestVal.activeCaloriesBurned} kcal"
            "caloriesConsumed" -> "${latestVal.caloriesConsumed} kcal"
            "carbsG" -> "${String.format(java.util.Locale.US, "%.1f", latestVal.carbsG)} g"
            "proteinG" -> "${String.format(java.util.Locale.US, "%.1f", latestVal.proteinG)} g"
            "fatG" -> "${String.format(java.util.Locale.US, "%.1f", latestVal.fatG)} g"
            "waterLiters" -> "${String.format(java.util.Locale.US, "%.1f", latestVal.waterLiters)} L"
            "exerciseDays" -> "${chronologicalData.count { it.exerciseMinutes > 0 }} active days"
            "heartRate" -> "${latestVal.heartRate} bpm"
            "oxygenSaturation" -> "${String.format(java.util.Locale.US, "%.1f", latestVal.oxygenSaturation)}%"
            "heartRateVariability" -> "${String.format(java.util.Locale.US, "%.1f", latestVal.heartRateVariability)} ms"
            "skinTemperatureCelsius" -> "${String.format(java.util.Locale.US, "%.1f", latestVal.skinTemperatureCelsius)} °C"
            "respiratoryRate" -> "${String.format(java.util.Locale.US, "%.1f", latestVal.respiratoryRate)} rpm"
            else -> "--"
        }
    } else {
        "--"
    }

    val averageVal = remember(chronologicalData, metricKey) {
        val nonZeroValues = chronologicalData.mapNotNull {
            when (metricKey) {
                "steps" -> if (it.steps > 0) it.steps.toFloat() else null
                "activeCaloriesBurned" -> if (it.activeCaloriesBurned > 0) it.activeCaloriesBurned.toFloat() else null
                "caloriesConsumed" -> if (it.caloriesConsumed > 0) it.caloriesConsumed.toFloat() else null
                "carbsG" -> if (it.carbsG > 0f) it.carbsG else null
                "proteinG" -> if (it.proteinG > 0f) it.proteinG else null
                "fatG" -> if (it.fatG > 0f) it.fatG else null
                "waterLiters" -> if (it.waterLiters > 0f) it.waterLiters else null
                "heartRate" -> if (it.heartRate > 0) it.heartRate.toFloat() else null
                "oxygenSaturation" -> if (it.oxygenSaturation > 0f) it.oxygenSaturation else null
                "heartRateVariability" -> if (it.heartRateVariability > 0f) it.heartRateVariability else null
                "skinTemperatureCelsius" -> if (it.skinTemperatureCelsius > 0f) it.skinTemperatureCelsius else null
                "respiratoryRate" -> if (it.respiratoryRate > 0f) it.respiratoryRate else null
                else -> null
            }
        }
        if (nonZeroValues.isNotEmpty()) nonZeroValues.average() else null
    }

    val displayAverage = if (averageVal != null) {
        when (metricKey) {
            "steps" -> "${String.format(java.util.Locale.US, "%,.0f", averageVal)} steps"
            "activeCaloriesBurned" -> "${String.format(java.util.Locale.US, "%.0f", averageVal)} kcal"
            "caloriesConsumed" -> "${String.format(java.util.Locale.US, "%.0f", averageVal)} kcal"
            "carbsG" -> "${String.format(java.util.Locale.US, "%.1f", averageVal)} g"
            "proteinG" -> "${String.format(java.util.Locale.US, "%.1f", averageVal)} g"
            "fatG" -> "${String.format(java.util.Locale.US, "%.1f", averageVal)} g"
            "waterLiters" -> "${String.format(java.util.Locale.US, "%.1f", averageVal)} L"
            "heartRate" -> "${String.format(java.util.Locale.US, "%.1f", averageVal)} bpm"
            "oxygenSaturation" -> "${String.format(java.util.Locale.US, "%.1f", averageVal)}%"
            "heartRateVariability" -> "${String.format(java.util.Locale.US, "%.1f", averageVal)} ms"
            "skinTemperatureCelsius" -> "${String.format(java.util.Locale.US, "%.1f", averageVal)} °C"
            "respiratoryRate" -> "${String.format(java.util.Locale.US, "%.1f", averageVal)} rpm"
            else -> null
        }
    } else {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = metricTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Range Selector Chips
            val ranges = listOf("D", "W", "M", "3M", "Y")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ranges.forEach { range ->
                    val isSelected = selectedRange == range
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                selectedRange = range
                                periodOffset = 0
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = range,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Period Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { periodOffset++ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous period",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(
                        onClick = { if (periodOffset > 0) periodOffset-- },
                        enabled = periodOffset > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next period",
                            tint = if (periodOffset > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }

                if (periodOffset != 0) {
                    IconButton(
                        onClick = { periodOffset = 0 }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset to current period",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Latest Value Callout Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (metricKey == "exerciseDays") "Total Active Days" else "Latest Reading",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayValue,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (displayAverage != null && metricKey != "exerciseDays") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Average: $displayAverage",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chart Title
            Text(
                text = "History Trend",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Chart Render Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!hasData) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No data available for this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    when (metricKey) {
                        "steps", "activeCaloriesBurned", "caloriesConsumed", "carbsG", "proteinG", "fatG", "waterLiters" -> {
                            StepsCaloriesBarChart(
                                data = chronologicalData,
                                metricKey = metricKey,
                                isDark = isDark
                            )
                        }
                        "exerciseDays" -> {
                            ExerciseStreakStrip(
                                data = chronologicalData,
                                isDark = isDark
                            )
                        }
                        "heartRate", "oxygenSaturation" -> {
                            ZoneBarChart(
                                data = chronologicalData,
                                metricKey = metricKey
                            )
                        }
                        "heartRateVariability", "skinTemperatureCelsius", "respiratoryRate" -> {
                            LineChartMetric(
                                data = chronologicalData,
                                metricKey = metricKey
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (metricKey == "heartRate" && selectedRange == "D") {
                Text(
                    text = "Intraday Heart Rate",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                IntradayHeartRateChart(
                    samples = heartRateDaySamples,
                    age = profile?.age ?: 0,
                    selectedDate = selectedDayDate,
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Time in Heart Rate Zones",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                HeartRateZonesCard(
                    samples = heartRateDaySamples,
                    age = profile?.age ?: 0,
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Day-by-Day Entries Section
            Text(
                text = "Entries",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            val entries = chronologicalData.filter { hasMetricValue(it, metricKey) }.reversed()
            if (entries.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No entries recorded for this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        entries.forEachIndexed { index, metric ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatEntryDate(metric.date),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatMetricValueForEntry(metric, metricKey),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (index < entries.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun StepsCaloriesBarChart(
    data: List<DailyMetric>,
    metricKey: String,
    isDark: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(250.dp)) {
        val totalWidth = maxWidth
        val contentWidth = if (data.size > 15) {
            (data.size * 16).dp.coerceAtLeast(totalWidth)
        } else {
            totalWidth
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(modifier = Modifier.width(contentWidth).fillMaxHeight().padding(vertical = 16.dp)) {
                val maxVal = (data.map {
                    when (metricKey) {
                        "steps" -> it.steps.toFloat()
                        "activeCaloriesBurned" -> it.activeCaloriesBurned.toFloat()
                        "caloriesConsumed" -> it.caloriesConsumed.toFloat()
                        "carbsG" -> it.carbsG
                        "proteinG" -> it.proteinG
                        "fatG" -> it.fatG
                        "waterLiters" -> it.waterLiters
                        else -> 0f
                    }
                }.maxOrNull() ?: 100f).coerceAtLeast(1f)

                val barColor = when (metricKey) {
                    "steps" -> Emerald500
                    "activeCaloriesBurned" -> Orange500
                    "caloriesConsumed" -> if (isDark) Color(0xFFFFB27D) else Orange700
                    "carbsG" -> if (isDark) Color(0xFFFCD34D) else Color(0xFFD97706)
                    "proteinG" -> if (isDark) Color(0xFF6EE7B7) else Emerald700
                    "fatG" -> if (isDark) Color(0xFFC4B5FD) else Color(0xFF7C3AED)
                    "waterLiters" -> AccentTokens.waterAccent(isDark).onBg
                    else -> Emerald500
                }

                val count = data.size
                val width = size.width
                val height = size.height
                val barWidth = (width / (count * 1.5f)).coerceIn(4.dp.toPx(), 24.dp.toPx())
                val spacing = if (count > 1) (width - (count * barWidth)) / (count - 1) else 0f

                data.forEachIndexed { index, metric ->
                    val value = when (metricKey) {
                        "steps" -> metric.steps.toFloat()
                        "activeCaloriesBurned" -> metric.activeCaloriesBurned.toFloat()
                        "caloriesConsumed" -> metric.caloriesConsumed.toFloat()
                        "carbsG" -> metric.carbsG
                        "proteinG" -> metric.proteinG
                        "fatG" -> metric.fatG
                        "waterLiters" -> metric.waterLiters
                        else -> 0f
                    }
                    if (value > 0f) {
                        val x = if (count > 1) index * (barWidth + spacing) else (width - barWidth) / 2f
                        val ratio = (value / maxVal).coerceIn(0f, 1f)
                        val barHeight = (ratio * (height - 8.dp.toPx())).coerceAtLeast(4.dp.toPx())
                        val y = height - barHeight

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    } else {
                        val x = if (count > 1) index * (barWidth + spacing) else (width - barWidth) / 2f
                        drawRoundRect(
                            color = Slate500.copy(alpha = 0.35f),
                            topLeft = Offset(x, height - 8.dp.toPx()),
                            size = Size(barWidth, 8.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseStreakStrip(
    data: List<DailyMetric>,
    isDark: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val totalWidth = maxWidth
        val contentWidth = if (data.size > 15) {
            (data.size * 16).dp.coerceAtLeast(totalWidth)
        } else {
            totalWidth
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.width(contentWidth).height(60.dp)) {
                val count = data.size
                val width = size.width
                val height = size.height
                val pillWidth = 8.dp.toPx()
                val pillHeight = 28.dp.toPx()
                val spacing = if (count > 1) (width - (count * pillWidth)) / (count - 1) else 0f
                val y = (height - pillHeight) / 2f

                data.forEachIndexed { index, metric ->
                    val x = if (count > 1) index * (pillWidth + spacing) else (width - pillWidth) / 2f
                    val isMoved = metric.exerciseMinutes > 0

                    drawRoundRect(
                        color = if (isMoved) Emerald500 else Emerald500.copy(alpha = 0.38f),
                        topLeft = Offset(x, y),
                        size = Size(pillWidth, pillHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneBarChart(
    data: List<DailyMetric>,
    metricKey: String
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(250.dp)) {
        val totalWidth = maxWidth
        val contentWidth = if (data.size > 15) {
            (data.size * 16).dp.coerceAtLeast(totalWidth)
        } else {
            totalWidth
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(modifier = Modifier.width(contentWidth).fillMaxHeight().padding(vertical = 16.dp)) {
                val values = data.map {
                    if (metricKey == "heartRate") it.heartRate.toFloat() else it.oxygenSaturation
                }
                val maxVal = (values.maxOfOrNull { it } ?: 100f).coerceAtLeast(1f)
                val count = data.size
                val width = size.width
                val height = size.height
                val barWidth = (width / (count * 1.5f)).coerceIn(4.dp.toPx(), 24.dp.toPx())
                val spacing = if (count > 1) (width - (count * barWidth)) / (count - 1) else 0f

                data.forEachIndexed { index, metric ->
                    val value = if (metricKey == "heartRate") metric.heartRate.toFloat() else metric.oxygenSaturation
                    if (value > 0) {
                        val color = if (metricKey == "heartRate") {
                            when {
                                value < 60f -> Slate500
                                value <= 100f -> Emerald500
                                else -> Red500
                            }
                        } else {
                            when {
                                value < 90f -> Red500
                                value < 95f -> Orange500
                                else -> Emerald500
                            }
                        }

                        val x = if (count > 1) index * (barWidth + spacing) else (width - barWidth) / 2f
                        val ratio = (value / maxVal).coerceIn(0f, 1f)
                        val barHeight = (ratio * (height - 8.dp.toPx())).coerceAtLeast(4.dp.toPx())
                        val y = height - barHeight

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    } else {
                        val x = if (count > 1) index * (barWidth + spacing) else (width - barWidth) / 2f
                        drawRoundRect(
                            color = Slate500.copy(alpha = 0.35f),
                            topLeft = Offset(x, height - 8.dp.toPx()),
                            size = Size(barWidth, 8.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LineChartMetric(
    data: List<DailyMetric>,
    metricKey: String
) {
    val validValues = remember(data, metricKey) {
        data.mapNotNull {
            val value = when (metricKey) {
                "heartRateVariability" -> it.heartRateVariability
                "skinTemperatureCelsius" -> it.skinTemperatureCelsius
                "respiratoryRate" -> it.respiratoryRate
                else -> 0f
            }
            if (value > 0f) value else null
        }
    }

    if (validValues.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available for this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(250.dp)) {
        val totalWidth = maxWidth
        val contentWidth = if (data.size > 15) {
            (data.size * 20).dp.coerceAtLeast(totalWidth)
        } else {
            totalWidth
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(modifier = Modifier.width(contentWidth).fillMaxHeight().padding(vertical = 16.dp)) {
                val minW = (validValues.minOfOrNull { it } ?: 0f) * 0.9f
                val maxW = (validValues.maxOfOrNull { it } ?: 100f) * 1.1f
                val range = (maxW - minW).takeIf { it > 0.001f } ?: 1f
                val width = size.width
                val height = size.height
                val count = data.size

                val stepX = if (count > 1) width / (count - 1) else width
                val path = Path()
                var lastValidIndex: Int? = null

                data.forEachIndexed { index, metric ->
                    val value = when (metricKey) {
                        "heartRateVariability" -> metric.heartRateVariability
                        "skinTemperatureCelsius" -> metric.skinTemperatureCelsius
                        "respiratoryRate" -> metric.respiratoryRate
                        else -> 0f
                    }
                    if (value > 0f) {
                        val x = if (count > 1) index * stepX else width / 2f
                        val y = height - (((value - minW) / range) * height)

                        if (lastValidIndex == null || lastValidIndex != index - 1) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                        lastValidIndex = index
                        drawCircle(color = Emerald500, radius = 6.dp.toPx(), center = Offset(x, y))
                    }
                }
                drawPath(path, color = Emerald500, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

private fun formatEntryDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val today = LocalDate.now()
        when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))
        }
    } catch (e: Exception) {
        dateStr
    }
}

private fun hasMetricValue(metric: DailyMetric, metricKey: String): Boolean {
    return when (metricKey) {
        "steps" -> metric.steps > 0
        "activeCaloriesBurned" -> metric.activeCaloriesBurned > 0
        "caloriesConsumed" -> metric.caloriesConsumed > 0
        "carbsG" -> metric.carbsG > 0f
        "proteinG" -> metric.proteinG > 0f
        "fatG" -> metric.fatG > 0f
        "waterLiters" -> metric.waterLiters > 0f
        "exerciseDays" -> metric.exerciseMinutes > 0
        "heartRate" -> metric.heartRate > 0
        "oxygenSaturation" -> metric.oxygenSaturation > 0
        "heartRateVariability" -> metric.heartRateVariability > 0
        "skinTemperatureCelsius" -> metric.skinTemperatureCelsius > 0
        "respiratoryRate" -> metric.respiratoryRate > 0
        else -> false
    }
}

private fun formatMetricValueForEntry(metric: DailyMetric, metricKey: String): String {
    return when (metricKey) {
        "steps" -> "${metric.steps} steps"
        "activeCaloriesBurned" -> "${metric.activeCaloriesBurned} kcal"
        "caloriesConsumed" -> "${metric.caloriesConsumed} kcal"
        "carbsG" -> "${String.format(Locale.US, "%.1f", metric.carbsG)} g"
        "proteinG" -> "${String.format(Locale.US, "%.1f", metric.proteinG)} g"
        "fatG" -> "${String.format(Locale.US, "%.1f", metric.fatG)} g"
        "waterLiters" -> "${String.format(Locale.US, "%.1f", metric.waterLiters)} L"
        "exerciseDays" -> "${metric.exerciseMinutes} mins"
        "heartRate" -> "${metric.heartRate} bpm"
        "oxygenSaturation" -> "${String.format(Locale.US, "%.1f", metric.oxygenSaturation)}%"
        "heartRateVariability" -> "${String.format(Locale.US, "%.1f", metric.heartRateVariability)} ms"
        "skinTemperatureCelsius" -> "${String.format(Locale.US, "%.1f", metric.skinTemperatureCelsius)} °C"
        "respiratoryRate" -> "${String.format(Locale.US, "%.1f", metric.respiratoryRate)} rpm"
        else -> "--"
    }
}

@Composable
private fun IntradayHeartRateChart(
    samples: List<Pair<Instant, Int>>,
    age: Int,
    selectedDate: LocalDate,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        if (samples.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No heart rate data for this day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                val maxHr = HealthGoalCalculator.maxHeartRate(age)
                val peakBpm = (maxHr * 0.85f).roundToInt()
                val vigorousBpm = (maxHr * 0.70f).roundToInt()
                val moderateBpm = (maxHr * 0.50f).roundToInt()

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val leftPadding = 8.dp.toPx()
                    val rightPadding = 8.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val bottomPadding = 24.dp.toPx()

                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - topPadding - bottomPadding

                    val sampleMin = samples.minOf { it.second }.toFloat()
                    val sampleMax = samples.maxOf { it.second }.toFloat()
                    val minY = (minOf(sampleMin - 10f, moderateBpm - 20f)).coerceAtLeast(30f)
                    val maxY = (maxOf(sampleMax + 10f, maxHr.toFloat(), peakBpm + 10f)).coerceAtLeast(120f)
                    val bpmRange = (maxY - minY).coerceAtLeast(1f)

                    fun getY(bpm: Float): Float {
                        val ratio = ((bpm - minY) / bpmRange).coerceIn(0f, 1f)
                        return topPadding + chartHeight * (1f - ratio)
                    }

                    val zoneId = ZoneId.systemDefault()
                    val startOfDaySec = selectedDate.atStartOfDay(zoneId).toEpochSecond()
                    val totalSecInDay = 86400f

                    fun getX(instant: Instant): Float {
                        val secFromStart = instant.epochSecond - startOfDaySec
                        val ratio = (secFromStart / totalSecInDay).coerceIn(0f, 1f)
                        return leftPadding + chartWidth * ratio
                    }

                    val dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    // Draw 3 horizontal dashed reference lines with zone labels
                    val zones = listOf(
                        Triple(peakBpm.toFloat(), "Peak ($peakBpm)", if (isDark) Color(0xFFFF6B6B) else Red500),
                        Triple(vigorousBpm.toFloat(), "Vigorous ($vigorousBpm)", if (isDark) Color(0xFFFFB074) else Orange500),
                        Triple(moderateBpm.toFloat(), "Moderate ($moderateBpm)", if (isDark) Color(0xFF6EE7B7) else Emerald600)
                    )

                    zones.forEach { (bpm, label, color) ->
                        val y = getY(bpm)
                        drawLine(
                            color = color.copy(alpha = 0.5f),
                            start = Offset(leftPadding, y),
                            end = Offset(width - rightPadding, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )

                        val textPaint = Paint().apply {
                            this.color = color.toArgb()
                            this.textSize = 10.sp.toPx()
                            this.isAntiAlias = true
                            this.textAlign = Paint.Align.RIGHT
                        }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(label, width - rightPadding, y - 4.dp.toPx(), textPaint)
                        }
                    }

                    // Plot heart rate line & points
                    val path = Path()
                    var hasMoved = false

                    samples.forEach { sample ->
                        val x = getX(sample.first)
                        val y = getY(sample.second.toFloat())

                        if (!hasMoved) {
                            path.moveTo(x, y)
                            hasMoved = true
                        } else {
                            path.lineTo(x, y)
                        }

                        val zone = HealthGoalCalculator.heartRateZoneFor(sample.second, age)
                        val dotColor = when (zone) {
                            HealthGoalCalculator.HeartRateZone.PEAK -> if (isDark) Color(0xFFFF6B6B) else Red500
                            HealthGoalCalculator.HeartRateZone.VIGOROUS -> if (isDark) Color(0xFFFFB074) else Orange500
                            HealthGoalCalculator.HeartRateZone.MODERATE -> if (isDark) Color(0xFF6EE7B7) else Emerald600
                            else -> Slate500
                        }

                        drawCircle(
                            color = dotColor,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    drawPath(
                        path = path,
                        color = (if (isDark) Emerald500 else Emerald600).copy(alpha = 0.75f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw time ticks along bottom (12 AM, 6 AM, 12 PM, 6 PM, 12 AM)
                    val timeTicks = listOf(
                        0f to "12 AM",
                        0.25f to "6 AM",
                        0.5f to "12 PM",
                        0.75f to "6 PM",
                        1f to "12 AM"
                    )

                    val timePaint = Paint().apply {
                        this.color = (if (isDark) Slate400 else Slate600).toArgb()
                        this.textSize = 10.sp.toPx()
                        this.isAntiAlias = true
                        this.textAlign = Paint.Align.CENTER
                    }

                    timeTicks.forEach { (ratio, label) ->
                        val x = (leftPadding + chartWidth * ratio).coerceIn(leftPadding + 14.dp.toPx(), width - rightPadding - 14.dp.toPx())
                        val y = height - 4.dp.toPx()
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(label, x, y, timePaint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartRateZonesCard(
    samples: List<Pair<Instant, Int>>,
    age: Int,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        if (samples.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No heart rate data for this day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val totalSamples = samples.size
            val totalSpanMinutes = if (samples.size >= 2) {
                val firstInstant = samples.first().first
                val lastInstant = samples.last().first
                Duration.between(firstInstant, lastInstant).toMinutes().coerceAtLeast(1L)
            } else {
                1L
            }

            val zoneDefs = listOf(
                Triple(HealthGoalCalculator.HeartRateZone.PEAK, "Peak (≥85%)", if (isDark) Color(0xFFFF6B6B) else Red500),
                Triple(HealthGoalCalculator.HeartRateZone.VIGOROUS, "Vigorous (70-84%)", if (isDark) Color(0xFFFFB074) else Orange500),
                Triple(HealthGoalCalculator.HeartRateZone.MODERATE, "Moderate (50-69%)", if (isDark) Color(0xFF6EE7B7) else Emerald600),
                Triple(HealthGoalCalculator.HeartRateZone.LIGHT, "Light (<50%)", Slate500)
            )

            val activeZones = zoneDefs.mapNotNull { (zone, label, color) ->
                val count = samples.count { HealthGoalCalculator.heartRateZoneFor(it.second, age) == zone }
                if (count > 0) {
                    val pct = (count.toFloat() / totalSamples.toFloat()) * 100f
                    val minutes = ((count.toFloat() / totalSamples.toFloat()) * totalSpanMinutes).roundToInt()
                    ZoneStat(zone = zone, label = label, color = color, percentage = pct, minutes = minutes)
                } else {
                    null
                }
            }

            if (activeZones.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active zone data recorded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    activeZones.forEachIndexed { index, stat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(stat.color)
                                )
                                Text(
                                    text = stat.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${String.format(Locale.US, "%.0f", stat.percentage)}% (${stat.minutes} min)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (index < activeZones.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ZoneStat(
    val zone: HealthGoalCalculator.HeartRateZone,
    val label: String,
    val color: Color,
    val percentage: Float,
    val minutes: Int
)
