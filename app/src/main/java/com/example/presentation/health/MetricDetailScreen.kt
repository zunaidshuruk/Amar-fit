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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.PathEffect
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
import com.example.data.local.UserProfile
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.flowOf

private data class EntryRow(val label: String, val value: String, val metGoal: Boolean = false)

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
    var selectedHrTab by remember(metricKey, selectedRange) { mutableStateOf("Zones") }

    val historyFlow = remember(selectedRange, periodOffset) {
        viewModel.getMetricsHistoryFlowForPeriod(selectedRange, periodOffset)
    }
    val history by historyFlow.collectAsState(initial = emptyList())
    val chronologicalData = remember(history) { history.reversed() }

    val previousHistoryFlow = remember(selectedRange, periodOffset, metricKey) {
        if (metricKey == "heartRate") {
            viewModel.getMetricsHistoryFlowForPeriod(selectedRange, periodOffset + 1)
        } else {
            null
        }
    }
    val previousHistory by (previousHistoryFlow ?: remember { flowOf(emptyList()) }).collectAsState(initial = emptyList())
    val previousChronologicalData = remember(previousHistory) { previousHistory.reversed() }

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

    val stepsHourlyBuckets by produceState<List<Pair<Instant, Int>>>(
        initialValue = emptyList(),
        key1 = metricKey,
        key2 = selectedRange,
        key3 = selectedDayDate
    ) {
        if (metricKey == "steps" && selectedRange == "D") {
            value = viewModel.getHourlyStepsForDate(selectedDayDate)
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

    val heartRateDayRange = if (metricKey == "heartRate" && selectedRange == "D" && heartRateDaySamples.isNotEmpty()) {
        val lo = heartRateDaySamples.minOf { it.second }
        val hi = heartRateDaySamples.maxOf { it.second }
        "$lo-$hi bpm"
    } else null

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

    val previousAverageVal = remember(previousChronologicalData, metricKey) {
        if (metricKey == "heartRate") {
            val values = previousChronologicalData.map { it.heartRate }.filter { it > 0 }
            if (values.isNotEmpty()) values.average() else null
        } else {
            null
        }
    }

    val heartRateDelta = if (metricKey == "heartRate" && averageVal != null && previousAverageVal != null) {
        averageVal - previousAverageVal
    } else {
        null
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
                        text = when {
                            metricKey == "exerciseDays" -> "Total Active Days"
                            heartRateDayRange != null -> "Range"
                            else -> "Latest Reading"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = heartRateDayRange ?: displayValue,
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
                        if (metricKey == "heartRate" && heartRateDelta != null) {
                            val sign = if (heartRateDelta >= 0) "+" else ""
                            Text(
                                text = "$sign${String.format(Locale.US, "%.1f", heartRateDelta)} bpm vs last period",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (metricKey == "steps" && selectedRange == "D") {
                Text(
                    text = "Hourly Steps",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                IntradayStepsChart(buckets = stepsHourlyBuckets, isDark = isDark)

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Trend / Zones toggle for Heart Rate Day view
            if (metricKey == "heartRate" && selectedRange == "D") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Trend", "Zones").forEach { tab ->
                        val isSelected = selectedHrTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surface
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                                .clickable { selectedHrTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val showHistoryTrend = if (metricKey == "heartRate" && selectedRange == "D") {
                selectedHrTab == "Trend"
            } else {
                true
            }

            if (showHistoryTrend) {
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
                                    profile = profile,
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
                                if (metricKey == "heartRate" && selectedRange == "D") {
                                    IntradayRangeBarChart(
                                        samples = heartRateDaySamples,
                                        isDark = isDark
                                    )
                                } else {
                                    ZoneBarChart(
                                        data = chronologicalData,
                                        metricKey = metricKey,
                                        age = profile?.age ?: 0
                                    )
                                }
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
            }

            if (metricKey == "heartRate" && selectedRange == "D" && selectedHrTab == "Zones") {
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

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Zones are estimated from your age and are not a medical measurement. Consult a healthcare professional for clinical heart rate guidance.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

            val displayEntries: List<EntryRow> = if (metricKey == "heartRate" && (selectedRange == "M" || selectedRange == "3M")) {
                chronologicalData
                    .filter { hasMetricValue(it, "heartRate") }
                    .groupBy { LocalDate.parse(it.date).with(DayOfWeek.MONDAY) }
                    .toSortedMap()
                    .map { (weekStartDate, group) ->
                        val avgBpm = group.map { it.heartRate }.average().roundToInt()
                        val label = "Week of " + weekStartDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
                        val value = "$avgBpm bpm"
                        EntryRow(label = label, value = value)
                    }
                    .reversed()
            } else {
                val goalForRow: Float? = when (metricKey) {
                    "steps" -> profile?.stepGoal?.toFloat()
                    "caloriesConsumed" -> profile?.dailyCalorieLimit?.toFloat()
                    "waterLiters" -> profile?.dailyWaterLimitLiters
                    else -> null
                }?.takeIf { it > 0f }

                chronologicalData
                    .filter { hasMetricValue(it, metricKey) }
                    .reversed()
                    .map { metric ->
                        val actualValue = when (metricKey) {
                            "steps" -> metric.steps.toFloat()
                            "caloriesConsumed" -> metric.caloriesConsumed.toFloat()
                            "waterLiters" -> metric.waterLiters
                            else -> null
                        }
                        val metGoal = goalForRow != null && actualValue != null && actualValue >= goalForRow
                        EntryRow(
                            label = formatEntryDate(metric.date),
                            value = formatMetricValueForEntry(metric, metricKey),
                            metGoal = metGoal
                        )
                    }
            }

            if (displayEntries.isEmpty()) {
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
                        displayEntries.forEachIndexed { index, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (row.metGoal) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Goal met",
                                            tint = Emerald500,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = row.value,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (index < displayEntries.size - 1) {
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
    profile: UserProfile?,
    isDark: Boolean
) {
    val goalLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val rawGoal = when (metricKey) {
        "steps" -> profile?.stepGoal?.toFloat()
        "caloriesConsumed" -> profile?.dailyCalorieLimit?.toFloat()
        "waterLiters" -> profile?.dailyWaterLimitLiters
        else -> null
    }
    val goalValue = if (rawGoal != null && rawGoal > 0f) rawGoal else null

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
                val dataMax = (data.map {
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

                val maxVal = if (goalValue != null) maxOf(dataMax, goalValue) else dataMax

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

                if (goalValue != null && goalValue > 0f) {
                    val rawY = height - (goalValue / maxVal) * (height - 8.dp.toPx())
                    val lineY = rawY.coerceAtLeast(14.dp.toPx())

                    drawLine(
                        color = goalLineColor,
                        start = Offset(0f, lineY),
                        end = Offset(width, lineY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )

                    val goalLabel = when (metricKey) {
                        "steps" -> "Goal: ${String.format(Locale.US, "%,d", goalValue.toInt())}"
                        "caloriesConsumed" -> "Goal: ${goalValue.toInt()}"
                        "waterLiters" -> "Goal: ${String.format(Locale.US, "%.1f", goalValue)}L"
                        else -> "Goal: $goalValue"
                    }

                    val textPaint = Paint().apply {
                        this.color = goalLineColor.toArgb()
                        this.textSize = 10.sp.toPx()
                        this.isAntiAlias = true
                        this.textAlign = Paint.Align.LEFT
                    }

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(goalLabel, 4.dp.toPx(), lineY - 4.dp.toPx(), textPaint)
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
    metricKey: String,
    age: Int = 0
) {
    val goalLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val goalValue = if (metricKey == "heartRate") {
        (HealthGoalCalculator.maxHeartRate(age) * 0.5f)
    } else {
        null
    }

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
                val dataMax = (values.maxOfOrNull { it } ?: 100f).coerceAtLeast(1f)
                val effectiveMax = if (goalValue != null && goalValue > 0f) maxOf(dataMax, goalValue) else dataMax
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
                        val ratio = (value / effectiveMax).coerceIn(0f, 1f)
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

                if (goalValue != null && goalValue > 0f) {
                    val rawY = height - (goalValue / effectiveMax) * (height - 8.dp.toPx())
                    val lineY = rawY.coerceAtLeast(14.dp.toPx())

                    drawLine(
                        color = goalLineColor,
                        start = Offset(0f, lineY),
                        end = Offset(width, lineY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )

                    val goalLabel = "Target: ${goalValue.toInt()} bpm"

                    val textPaint = Paint().apply {
                        this.color = goalLineColor.toArgb()
                        this.textSize = 10.sp.toPx()
                        this.isAntiAlias = true
                        this.textAlign = Paint.Align.LEFT
                    }

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(goalLabel, 4.dp.toPx(), lineY - 4.dp.toPx(), textPaint)
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
private fun IntradayRangeBarChart(
    samples: List<Pair<Instant, Int>>,
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
                modifier = Modifier.fillMaxWidth().height(220.dp),
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
                val zoneId = ZoneId.systemDefault()
                // Bucket samples into 24 hourly slots, each holding min/max bpm for that hour
                val hourlyBuckets = remember(samples) {
                    val buckets = Array(24) { mutableListOf<Int>() }
                    samples.forEach { (instant, bpm) ->
                        val hour = instant.atZone(zoneId).hour
                        if (hour in 0..23 && bpm > 0) buckets[hour].add(bpm)
                    }
                    buckets.map { if (it.isNotEmpty()) it.min() to it.max() else null }
                }

                val overallMin = samples.minOf { it.second }
                val overallMax = samples.maxOf { it.second }
                // Round the axis bounds to clean multiples of 10, with headroom
                val axisMin = ((overallMin - 10) / 10 * 10).coerceAtMost(overallMin - 5).coerceAtLeast(0)
                val axisMax = (((overallMax + 15) / 10) + 1) * 10

                Canvas(
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val leftPadding = 8.dp.toPx()
                    val rightPadding = 32.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val bottomPadding = 24.dp.toPx()
                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - topPadding - bottomPadding

                    val range = (axisMax - axisMin).coerceAtLeast(1)
                    fun getY(bpm: Int): Float {
                        val ratio = ((bpm - axisMin).toFloat() / range).coerceIn(0f, 1f)
                        return topPadding + chartHeight * (1f - ratio)
                    }

                    val barColor = if (isDark) Color(0xFFFF6B6B) else Red500
                    val slotWidth = chartWidth / 24
                    val barWidth = (slotWidth * 0.4f).coerceAtLeast(3.dp.toPx())

                    hourlyBuckets.forEachIndexed { hour, minMax ->
                        if (minMax != null) {
                            val (lo, hi) = minMax
                            val x = leftPadding + hour * slotWidth + slotWidth / 2f
                            drawLine(
                                color = barColor,
                                start = Offset(x, getY(lo)),
                                end = Offset(x, getY(hi)),
                                strokeWidth = barWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }

                    // Dashed gridlines + labels at axisMin, midpoint, axisMax (matching the
                    // reference's 3-line y-axis)
                    val dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val gridColor = (if (isDark) Slate400 else Slate600).copy(alpha = 0.3f)
                    val textPaint = Paint().apply {
                        this.color = (if (isDark) Slate400 else Slate600).toArgb()
                        this.textSize = 10.sp.toPx()
                        this.isAntiAlias = true
                    }
                    val midVal = (axisMin + axisMax) / 2
                    listOf(axisMin, midVal, axisMax).forEach { value ->
                        val y = getY(value)
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(width - rightPadding, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(value.toString(), width - rightPadding + 6.dp.toPx(), y + 4.dp.toPx(), textPaint)
                        }
                    }

                    // X-axis labels: 0, 6, 12, 18, 24
                    listOf(0, 6, 12, 18, 24).forEach { hourMark ->
                        val x = leftPadding + (hourMark.coerceAtMost(23)) * slotWidth + (if (hourMark == 24) slotWidth else slotWidth / 2f)
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(hourMark.toString(), x, height - 4.dp.toPx(), textPaint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntradayStepsChart(
    buckets: List<Pair<Instant, Int>>,
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
        if (buckets.isEmpty()) {
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
            Column(modifier = Modifier.padding(16.dp)) {
                val zoneId = ZoneId.systemDefault()
                // Construct a 24-slot map or list for hours 0..23
                val hourlyStepCounts = remember(buckets) {
                    val counts = IntArray(24) { 0 }
                    buckets.forEach { (instant, count) ->
                        val hour = instant.atZone(zoneId).hour
                        if (hour in 0..23) {
                            counts[hour] += count
                        }
                    }
                    counts
                }

                val maxSteps = remember(hourlyStepCounts) {
                    (hourlyStepCounts.maxOrNull() ?: 0).coerceAtLeast(100)
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val leftPadding = 8.dp.toPx()
                    val rightPadding = 8.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val bottomPadding = 24.dp.toPx()

                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - topPadding - bottomPadding

                    val numBars = 24
                    val totalSpacingRatio = 0.3f
                    val slotWidth = chartWidth / numBars
                    val barWidth = slotWidth * (1f - totalSpacingRatio)
                    val spacing = slotWidth * totalSpacingRatio

                    val barColor = if (isDark) Emerald500 else Emerald600

                    for (hour in 0 until 24) {
                        val steps = hourlyStepCounts[hour]
                        val x = leftPadding + hour * slotWidth + (spacing / 2f)
                        val ratio = (steps.toFloat() / maxSteps.toFloat()).coerceIn(0f, 1f)
                        val barHeight = if (steps > 0) {
                            (ratio * chartHeight).coerceAtLeast(3.dp.toPx())
                        } else {
                            0f
                        }
                        val y = topPadding + chartHeight - barHeight

                        if (barHeight > 0f) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }

                    // Time labels along bottom (every 4 hours: 12 AM, 4 AM, 8 AM, 12 PM, 4 PM, 8 PM)
                    val hourLabels = listOf(
                        0 to "12 AM",
                        4 to "4 AM",
                        8 to "8 AM",
                        12 to "12 PM",
                        16 to "4 PM",
                        20 to "8 PM"
                    )

                    val textPaint = Paint().apply {
                        this.color = (if (isDark) Slate400 else Slate600).toArgb()
                        this.textSize = 10.sp.toPx()
                        this.isAntiAlias = true
                        this.textAlign = Paint.Align.CENTER
                    }

                    hourLabels.forEach { (hour, label) ->
                        val x = leftPadding + (hour + 0.5f) * slotWidth
                        val labelX = x.coerceIn(leftPadding + 14.dp.toPx(), width - rightPadding - 14.dp.toPx())
                        val y = height - 4.dp.toPx()
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(label, labelX, y, textPaint)
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
