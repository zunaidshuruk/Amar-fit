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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyMetric
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*

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
    val days = when (selectedRange) {
        "D" -> 1
        "W" -> 7
        "M" -> 30
        "3M" -> 90
        "Y" -> 365
        else -> 7
    }

    val historyFlow = remember(days) { viewModel.getMetricsHistoryFlow(days) }
    val history by historyFlow.collectAsState(initial = emptyList())
    val chronologicalData = remember(history) { history.reversed() }

    val metricTitle = when (metricKey) {
        "steps" -> "Steps"
        "activeCaloriesBurned" -> "Active Calories Burned"
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
                            .clickable { selectedRange = range }
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

            Spacer(modifier = Modifier.height(16.dp))

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
                        "steps", "activeCaloriesBurned" -> {
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
                    if (metricKey == "steps") it.steps.toFloat() else it.activeCaloriesBurned.toFloat()
                }.maxOrNull() ?: 100f).coerceAtLeast(1f)

                val count = data.size
                val width = size.width
                val height = size.height
                val barWidth = (width / (count * 1.5f)).coerceIn(4.dp.toPx(), 24.dp.toPx())
                val spacing = if (count > 1) (width - (count * barWidth)) / (count - 1) else 0f

                data.forEachIndexed { index, metric ->
                    val value = if (metricKey == "steps") metric.steps else metric.activeCaloriesBurned
                    if (value > 0) {
                        val x = if (count > 1) index * (barWidth + spacing) else (width - barWidth) / 2f
                        val ratio = (value.toFloat() / maxVal).coerceIn(0f, 1f)
                        val barHeight = (ratio * (height - 8.dp.toPx())).coerceAtLeast(4.dp.toPx())
                        val y = height - barHeight

                        drawRoundRect(
                            color = if (metricKey == "steps") Emerald500 else Orange500,
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
