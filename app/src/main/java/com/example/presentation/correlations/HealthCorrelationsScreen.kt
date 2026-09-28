package com.example.presentation.correlations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyMetric
import com.example.presentation.viewmodel.ShasthoViewModel
import kotlin.math.roundToInt

private data class MetricSeries(
    val label: String,
    val unit: String,
    val extractor: (DailyMetric) -> Float
)

private val HISTOGRAM_METRICS = listOf(
    MetricSeries("Steps", "steps") { it.steps.toFloat() },
    MetricSeries("Sleep", "hrs") { it.sleepHours },
    MetricSeries("Water", "L") { it.waterLiters },
    MetricSeries("Calories", "kcal") { it.caloriesConsumed.toFloat() }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthCorrelationsScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {
    val historyFlow = remember { viewModel.getMetricsHistoryFlow(90) }
    val history by historyFlow.collectAsState(initial = emptyList())

    var selectedHistogramMetric by remember { mutableStateOf(HISTOGRAM_METRICS[0]) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Correlations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "See patterns in your health data over the last 90 days -- how consistent you are, and how your metrics relate to each other.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ---------------- Distribution (Histogram) ----------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Column {
                    Text("Your Typical Range", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "How often your ${selectedHistogramMetric.label.lowercase()} falls in each range",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HISTOGRAM_METRICS.forEach { metric ->
                            val selected = metric.label == selectedHistogramMetric.label
                            FilterChip(
                                selected = selected,
                                onClick = { selectedHistogramMetric = metric },
                                label = { Text(metric.label, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val values = history.map { selectedHistogramMetric.extractor(it) }.filter { it > 0f }
                    if (values.size < 3) {
                        Text(
                            "Not enough data yet -- keep logging to see your distribution.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        HistogramChart(values = values, unit = selectedHistogramMetric.unit)
                    }
                }
            }

            // ---------------- Correlations (Scatter Plots) ----------------
            Text("Correlations", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            CorrelationCard(
                title = "Steps vs. Sleep",
                subtitle = "Does more activity relate to more sleep?",
                xLabel = "Steps",
                yLabel = "Sleep (hrs)",
                points = history.filter { it.steps > 0 && it.sleepHours > 0f }
                    .map { it.steps.toFloat() to it.sleepHours }
            )

            CorrelationCard(
                title = "Sleep vs. Resting Heart Rate",
                subtitle = "Does more sleep relate to a lower resting heart rate?",
                xLabel = "Sleep (hrs)",
                yLabel = "Resting HR (bpm)",
                points = history.filter { it.sleepHours > 0f && it.restingHeartRate > 0 }
                    .map { it.sleepHours to it.restingHeartRate.toFloat() }
            )

            CorrelationCard(
                title = "Water Intake vs. Steps",
                subtitle = "Does staying hydrated relate to being more active?",
                xLabel = "Water (L)",
                yLabel = "Steps",
                points = history.filter { it.waterLiters > 0f && it.steps > 0 }
                    .map { it.waterLiters to it.steps.toFloat() }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CorrelationCard(title: String, subtitle: String, xLabel: String, yLabel: String, points: List<Pair<Float, Float>>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            if (points.size < 3) {
                Text(
                    "Not enough overlapping data yet for this comparison.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ScatterPlotChart(points = points, xLabel = xLabel, yLabel = yLabel)
            }
        }
    }
}

@Composable
private fun HistogramChart(values: List<Float>, unit: String) {
    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val minVal = values.min()
    val maxVal = values.max()
    val bucketCount = 6
    val range = (maxVal - minVal).coerceAtLeast(0.01f)
    val bucketSize = range / bucketCount
    val bucketCounts = IntArray(bucketCount)
    for (v in values) {
        val idx = (((v - minVal) / bucketSize).toInt()).coerceIn(0, bucketCount - 1)
        bucketCounts[idx]++
    }
    val maxCount = bucketCounts.max().coerceAtLeast(1)

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val barSlotWidth = size.width / bucketCount
            val barWidth = barSlotWidth * 0.65f
            val chartHeight = size.height - 4.dp.toPx()

            for (i in 0 until bucketCount) {
                val ratio = bucketCounts[i].toFloat() / maxCount
                val barHeight = (ratio * chartHeight).coerceAtLeast(if (bucketCounts[i] > 0) 4.dp.toPx() else 0f)
                val x = i * barSlotWidth + (barSlotWidth - barWidth) / 2f
                val y = size.height - barHeight
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
            drawLine(
                color = axisColor.copy(alpha = 0.3f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${formatBucketValue(minVal)} $unit",
                fontSize = 10.sp,
                color = labelColor
            )
            Text(
                text = "${formatBucketValue(maxVal)} $unit",
                fontSize = 10.sp,
                color = labelColor
            )
        }
    }
}

private fun formatBucketValue(value: Float): String {
    return if (value >= 100f) value.roundToInt().toString() else String.format(java.util.Locale.US, "%.1f", value)
}

@Composable
private fun ScatterPlotChart(points: List<Pair<Float, Float>>, xLabel: String, yLabel: String) {
    val dotColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val xMin = points.minOf { it.first }
    val xMax = points.maxOf { it.first }
    val yMin = points.minOf { it.second }
    val yMax = points.maxOf { it.second }
    val xRange = (xMax - xMin).coerceAtLeast(0.01f)
    val yRange = (yMax - yMin).coerceAtLeast(0.01f)

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val padding = 8.dp.toPx()
            val plotWidth = size.width - padding * 2
            val plotHeight = size.height - padding * 2

            // Axis lines
            drawLine(
                color = axisColor.copy(alpha = 0.3f),
                start = Offset(padding, size.height - padding),
                end = Offset(size.width - padding, size.height - padding),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = axisColor.copy(alpha = 0.3f),
                start = Offset(padding, padding),
                end = Offset(padding, size.height - padding),
                strokeWidth = 1.dp.toPx()
            )

            for ((x, y) in points) {
                val normX = padding + ((x - xMin) / xRange) * plotWidth
                val normY = (size.height - padding) - ((y - yMin) / yRange) * plotHeight
                drawCircle(
                    color = dotColor.copy(alpha = 0.75f),
                    radius = 5.dp.toPx(),
                    center = Offset(normX, normY)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "$xLabel: ${formatBucketValue(xMin)}\u2013${formatBucketValue(xMax)}", fontSize = 10.sp, color = labelColor)
            Text(text = "$yLabel: ${formatBucketValue(yMin)}\u2013${formatBucketValue(yMax)}", fontSize = 10.sp, color = labelColor)
        }
    }
}
