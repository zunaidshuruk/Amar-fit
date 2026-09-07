package com.example.presentation.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.navigation.NavController
import com.example.presentation.navigation.navigateToTab
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import java.text.NumberFormat

@Composable
fun HealthScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()

    val glucoseAccent = AccentTokens.glucoseAccent(isDark)
    val heartRateAccent = AccentTokens.heartRateAccent(isDark)
    val weightAccent = AccentTokens.weightAccent(isDark)
    val bloodPressureAccent = AccentTokens.bloodPressureAccent(isDark)
    val bmiAccent = AccentTokens.bmiAccent(isDark)
    val caloriesAccent = AccentTokens.caloriesAccent(isDark)
    val stepsAccent = AccentTokens.stepsAccent(isDark)
    val resilienceAccent = AccentTokens.resilienceAccent(isDark)

    val bmiTrackColor = if (isDark) Emerald800 else Emerald200
    val bmiFillColor = if (isDark) Emerald300 else Emerald600
    val resilienceTrackColor = resilienceAccent.onBg.copy(alpha = 0.2f)
    val resilienceFillColor = resilienceAccent.onBg

    val metrics by viewModel.todayMetrics.collectAsState()
    val todayFoodLogs by viewModel.todayFoodLogs.collectAsState()
    val metricsHistory by viewModel.metricsHistory.collectAsState()
    val last7Days = remember(metricsHistory) {
        metricsHistory.take(7).reversed()
    }

    // Resilience calculation
    val recent7Days = remember(metricsHistory) { metricsHistory.take(7) }
    val recentSleepDay = remember(recent7Days) { recent7Days.firstOrNull { it.sleepHours > 0f } }

    val sleepScore = remember(recentSleepDay) {
        recentSleepDay?.let {
            (100f - kotlin.math.abs(it.sleepHours - 8f) * 20f).coerceIn(0f, 100f)
        }
    }

    val hrvScore = remember(recent7Days, metricsHistory) {
        val mostRecentHrvDay = recent7Days.firstOrNull { it.heartRateVariability > 0f }
        if (mostRecentHrvDay != null) {
            val todayHrv = mostRecentHrvDay.heartRateVariability
            val priorHrvDays = metricsHistory.filter { it.date < mostRecentHrvDay.date && it.heartRateVariability > 0f }.take(7)
            if (priorHrvDays.size >= 3) {
                val avgHrv = priorHrvDays.map { it.heartRateVariability }.average().toFloat()
                if (avgHrv > 0f) {
                    (50f + ((todayHrv - avgHrv) / avgHrv) * 200f).coerceIn(0f, 100f)
                } else null
            } else null
        } else null
    }

    val resilienceScore = remember(sleepScore, hrvScore) {
        when {
            sleepScore == null -> null
            hrvScore == null -> kotlin.math.round(sleepScore).toInt().coerceIn(0, 100)
            else -> kotlin.math.round(0.6f * sleepScore + 0.4f * hrvScore).toInt().coerceIn(0, 100)
        }
    }

    val resilienceBucket = remember(resilienceScore) {
        resilienceScore?.let { score ->
            when {
                score >= 80 -> "Great"
                score >= 60 -> "Good"
                score >= 40 -> "Fair"
                else -> "Low"
            }
        }
    }
    
    val glucose = maxOf(metrics?.bloodGlucoseMorning ?: 0f, metrics?.bloodGlucoseNight ?: 0f)
    val heartRate = metrics?.heartRate ?: 0
    val bloodPressure = metrics?.bloodPressure ?: ""
    val weight = profile?.weightKg ?: 70f
    val heightM = (profile?.heightCm ?: 170f) / 100f
    val bmi = if (heightM > 0) weight / (heightM * heightM) else 0f
    val bmiFillFraction = if (bmi > 0f) ((bmi - 15f) / (35f - 15f)).coerceIn(0f, 1f) else 0f
    
    var showBpDialog by remember { mutableStateOf(false) }
    var showBmiDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Health Vitals", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        
        // Row 1: Glucose & Heart Rate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(glucoseAccent.bg)
                    .clickable { navController.navigate("glucoselog") }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "BLOOD GLUCOSE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = glucoseAccent.onBg)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = if(glucose > 0) "$glucose" else "--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = glucoseAccent.onBg)
                    Text(text = "mmol/L (+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = glucoseAccent.onBg)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(heartRateAccent.bg)
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "HEART RATE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = heartRateAccent.onBg)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = if(heartRate > 0) "$heartRate bpm" else "--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = heartRateAccent.onBg)
                    Text(text = "(Synced automatically)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = heartRateAccent.onBg)
                }
            }
        }

        // Row 2: Weight & Blood Pressure
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(weightAccent.bg)
                    .clickable { navController.navigate("weightlog") }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "WEIGHT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = weightAccent.onBg)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "$weight kg", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = weightAccent.onBg)
                    Text(text = "(+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = weightAccent.onBg)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bloodPressureAccent.bg)
                    .clickable { showBpDialog = true }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "BLOOD PRESSURE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = bloodPressureAccent.onBg)
                    Spacer(modifier = Modifier.height(16.dp))
                    val bp = metrics?.bloodPressure?.takeIf { it.isNotBlank() } ?: "--"
                    Text(text = bp, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = bloodPressureAccent.onBg)
                    Text(text = "mmHg (+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = bloodPressureAccent.onBg)
                }
            }
        }

        // BMI Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(bmiAccent.bg)
                .clickable { showBmiDialog = true }
                .padding(16.dp)
        ) {
            Column {
                Text(text = "BMI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = bmiAccent.onBg)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = String.format("%.1f", bmi), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = bmiAccent.onBg)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(bmiTrackColor)
                ) {
                    if (bmiFillFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(bmiFillFraction)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(bmiFillColor)
                        )
                    }
                }
            }
        }

        // Resilience Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(resilienceAccent.bg)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RESILIENCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = resilienceAccent.onBg
                    )
                    if (resilienceBucket != null) {
                        Text(
                            text = resilienceBucket,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = resilienceAccent.onBg
                        )
                    }
                }
                if (resilienceScore == null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Log your sleep to see your Resilience score",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = resilienceAccent.onBg
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "$resilienceScore",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = resilienceAccent.onBg
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(resilienceTrackColor)
                    ) {
                        val fillFraction = (resilienceScore / 100f).coerceIn(0f, 1f)
                        if (fillFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fillFraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(resilienceFillColor)
                            )
                        }
                    }
                    if (hrvScore == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Based on sleep only — sync Health Connect HRV for a fuller score",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = resilienceAccent.onBg.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Protocol Module
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Slate200, RoundedCornerShape(20.dp))
                .clickable { navController.navigate("lifestyle") }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Emerald50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Protocol", tint = Emerald700)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Lifestyle Protocol", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Read the health guidelines", color = Slate500, fontSize = 14.sp)
                    }
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Slate400)
            }
        }

        // Key metrics section
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Key metrics",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        val validWeights = remember(last7Days) { last7Days.filter { it.weightKg > 0f } }
        val latestWeight = validWeights.lastOrNull()?.weightKg
        val weightCallout = if (latestWeight != null) {
            String.format(java.util.Locale.US, "%.1f kg", latestWeight)
        } else {
            "--"
        }

        val caloriesData = remember(last7Days) { last7Days.map { it.activeCaloriesBurned } }
        val hasCalories = caloriesData.any { it > 0 }
        val todayCalories = metrics?.activeCaloriesBurned ?: caloriesData.lastOrNull() ?: 0
        val caloriesCallout = if (hasCalories) {
            "Today: ${NumberFormat.getIntegerInstance().format(todayCalories)} kcal"
        } else {
            "--"
        }

        val stepsData = remember(last7Days) { last7Days.map { it.steps } }
        val hasSteps = stepsData.any { it > 0 }
        val todaySteps = metrics?.steps ?: stepsData.lastOrNull() ?: 0
        val stepsCallout = if (hasSteps) {
            "Today: ${NumberFormat.getIntegerInstance().format(todaySteps)} steps"
        } else {
            "--"
        }

        val hasExercise = last7Days.any { it.exerciseMinutes > 0 }
        val activeDaysCount = last7Days.count { it.exerciseMinutes > 0 }
        val exerciseCallout = if (hasExercise) {
            "$activeDaysCount of ${last7Days.size} days"
        } else {
            "--"
        }

        // Row 1: Weight & Calories Burned
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Weight
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate("weightlog") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Weight",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = weightCallout,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (validWeights.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data available",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            val minW = (validWeights.minOfOrNull { it.weightKg } ?: 50f) - 1f
                            val maxW = (validWeights.maxOfOrNull { it.weightKg } ?: 100f) + 1f
                            val range = (maxW - minW).takeIf { it > 0.001f } ?: 1f
                            val width = size.width
                            val height = size.height
                            val count = validWeights.size

                            val stepX = if (count > 1) width / (count - 1) else 0f
                            val weightPath = Path()

                            validWeights.forEachIndexed { index, metric ->
                                val x = if (count > 1) index * stepX else width / 2f
                                val normY = ((metric.weightKg - minW) / range).coerceIn(0f, 1f)
                                val y = height - (normY * (height - 16.dp.toPx())) - 8.dp.toPx()

                                if (index == 0) weightPath.moveTo(x, y) else weightPath.lineTo(x, y)
                                drawCircle(
                                    color = weightAccent.onBg,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                            if (count > 1) {
                                drawPath(
                                    path = weightPath,
                                    color = weightAccent.onBg,
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }
            }

            // Card 2: Calories Burned
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate("metric_detail/activeCaloriesBurned") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Calories Burned",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = caloriesCallout,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (!hasCalories) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data available",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            val maxCal = (caloriesData.maxOrNull() ?: 500).coerceAtLeast(100).toFloat()
                            val count = caloriesData.size
                            val width = size.width
                            val height = size.height
                            val barWidth = 8.dp.toPx()
                            val spacing = if (count > 1) (width - (count * barWidth)) / (count - 1) else 0f

                            caloriesData.forEachIndexed { index, cal ->
                                val x = if (count > 1) index * (barWidth + spacing) else (width - barWidth) / 2f
                                val ratio = (cal.toFloat() / maxCal).coerceIn(0f, 1f)
                                val barHeight = (ratio * (height - 8.dp.toPx())).coerceAtLeast(4.dp.toPx())
                                val y = height - barHeight

                                drawRoundRect(
                                    color = if (cal > 0) caloriesAccent.onBg else caloriesAccent.onBg.copy(alpha = 0.2f),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }

        // Row 2: Steps & Exercise Days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 3: Steps
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate("metric_detail/steps") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Steps",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stepsCallout,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (!hasSteps) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data available",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            val maxSteps = (stepsData.maxOrNull() ?: 10000).coerceAtLeast(1000).toFloat()
                            val count = stepsData.size
                            val width = size.width
                            val height = size.height
                            val barWidth = 8.dp.toPx()
                            val spacing = if (count > 1) (width - (count * barWidth)) / (count - 1) else 0f

                            stepsData.forEachIndexed { index, st ->
                                val x = if (count > 1) index * (barWidth + spacing) else (width - barWidth) / 2f
                                val ratio = (st.toFloat() / maxSteps).coerceIn(0f, 1f)
                                val barHeight = (ratio * (height - 8.dp.toPx())).coerceAtLeast(4.dp.toPx())
                                val y = height - barHeight

                                drawRoundRect(
                                    color = if (st > 0) stepsAccent.onBg else stepsAccent.onBg.copy(alpha = 0.2f),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            // Card 4: Exercise Days
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate("metric_detail/exerciseDays") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Exercise Days",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = exerciseCallout,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (!hasExercise) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data available",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            val count = last7Days.size
                            val width = size.width
                            val height = size.height
                            val pillWidth = 8.dp.toPx()
                            val pillHeight = 28.dp.toPx()
                            val spacing = if (count > 1) (width - (count * pillWidth)) / (count - 1) else 0f
                            val y = (height - pillHeight) / 2f

                            last7Days.forEachIndexed { index, metric ->
                                val x = if (count > 1) index * (pillWidth + spacing) else (width - pillWidth) / 2f
                                val isMoved = metric.exerciseMinutes > 0

                                drawRoundRect(
                                    color = if (isMoved) stepsAccent.onBg else stepsAccent.onBg.copy(alpha = 0.2f),
                                    topLeft = Offset(x, y),
                                    size = Size(pillWidth, pillHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }

        // Additional Health Vitals section
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "More Health Vitals",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        val additionalMetrics = listOf(
            Triple("Heart Rate", if (heartRate > 0) "$heartRate bpm" else "--", "heartRate"),
            Triple("Blood Oxygen (SpO2)", metrics?.oxygenSaturation?.let { if (it > 0f) "${String.format(java.util.Locale.US, "%.1f", it)}%" else null } ?: "--", "oxygenSaturation"),
            Triple("Heart Rate Variability (HRV)", metrics?.heartRateVariability?.let { if (it > 0f) "${String.format(java.util.Locale.US, "%.1f", it)} ms" else null } ?: "--", "heartRateVariability"),
            Triple("Skin Temperature", metrics?.skinTemperatureCelsius?.let { if (it > 0f) "${String.format(java.util.Locale.US, "%.1f", it)} °C" else null } ?: "--", "skinTemperatureCelsius"),
            Triple("Respiratory Rate", metrics?.respiratoryRate?.let { if (it > 0f) "${String.format(java.util.Locale.US, "%.1f", it)} rpm" else null } ?: "--", "respiratoryRate")
        )

        additionalMetrics.forEach { (label, value, key) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("metric_detail/$key") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = value,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Focus areas section
        Spacer(modifier = Modifier.height(4.dp))
        Text("Focus areas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

        data class FocusAreaItem(
            val title: String,
            val isTracked: Boolean,
            val accent: AccentColors,
            val onClick: () -> Unit
        )

        val focusAreas = listOf(
            FocusAreaItem("Heart", heartRate > 0, AccentTokens.heartRateAccent(isDark)) { navigateToTab(navController, "health") },
            FocusAreaItem("Metabolic", glucose > 0f, AccentTokens.glucoseAccent(isDark)) { navController.navigate("glucoselog") },
            FocusAreaItem("Fitness", (metrics?.steps ?: 0) > 0 || (metrics?.exerciseMinutes ?: 0) > 0, AccentTokens.stepsAccent(isDark)) { navigateToTab(navController, "fitness") },
            FocusAreaItem("Sleep", (metrics?.sleepHours ?: 0f) > 0f, AccentTokens.sleepAccent(isDark)) { navigateToTab(navController, "sleep") },
            FocusAreaItem("Nutrition", todayFoodLogs.isNotEmpty() || (metrics?.caloriesConsumed ?: 0) > 0, AccentTokens.foodLogAccent(isDark)) { navController.navigate("foodlog") },
            FocusAreaItem("Vitals", bloodPressure.isNotBlank(), AccentTokens.bloodPressureAccent(isDark)) { showBpDialog = true },
            FocusAreaItem("Respiratory", false, AccentTokens.waterAccent(isDark)) {},
            FocusAreaItem("Temperature", false, AccentTokens.caloriesAccent(isDark)) {},
            FocusAreaItem("Mental wellbeing", false, AccentTokens.coachAccent(isDark)) {}
        )

        focusAreas.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    val bg = if (item.isTracked) item.accent.bg else MaterialTheme.colorScheme.surfaceVariant
                    val contentColor = if (item.isTracked) item.accent.onBg else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .then(if (item.isTracked) Modifier.clickable { item.onClick() } else Modifier)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(text = item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = contentColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (item.isTracked) "Tracked" else "Not tracked",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = contentColor.copy(alpha = if (item.isTracked) 1f else 0.7f)
                            )
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Health checks section
        Spacer(modifier = Modifier.height(4.dp))
        Text("Health checks", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

        val isHighHeartRate = heartRate > 100
        val isLowHeartRate = heartRate in 1..59
        val hrAccent = AccentTokens.heartRateAccent(isDark)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val highBg = if (isHighHeartRate) hrAccent.bg else MaterialTheme.colorScheme.surfaceVariant
            val highColor = if (isHighHeartRate) hrAccent.onBg else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(highBg)
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "High heart rate", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = highColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHighHeartRate) "$heartRate bpm" else "Not available",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = highColor.copy(alpha = if (isHighHeartRate) 1f else 0.7f)
                    )
                }
            }

            val lowBg = if (isLowHeartRate) hrAccent.bg else MaterialTheme.colorScheme.surfaceVariant
            val lowColor = if (isLowHeartRate) hrAccent.onBg else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(lowBg)
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "Low heart rate", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = lowColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLowHeartRate) "$heartRate bpm" else "Not available",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = lowColor.copy(alpha = if (isLowHeartRate) 1f else 0.7f)
                    )
                }
            }
        }

        // Personal info section
        Spacer(modifier = Modifier.height(4.dp))
        Text("Personal info", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val profileAccent = AccentTokens.pointsAccent(isDark)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(profileAccent.bg)
                    .clickable { navController.navigate("settings") }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "Profile", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = profileAccent.onBg)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Account & Settings", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = profileAccent.onBg.copy(alpha = 0.8f))
                }
            }

            val chatAccent = AccentTokens.dietChartAccent(isDark)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(chatAccent.bg)
                    .clickable { navController.navigate("chat?openSavedChats=true") }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "Chat history", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = chatAccent.onBg)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Saved AI chats", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = chatAccent.onBg.copy(alpha = 0.8f))
                }
            }
        }

        val medicalAccent = AccentTokens.medicalAccent(isDark)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(medicalAccent.bg)
                .clickable { navController.navigate("medical_records") }
                .padding(16.dp)
        ) {
            Column {
                Text(text = "Medical", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = medicalAccent.onBg)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Health records", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = medicalAccent.onBg.copy(alpha = 0.8f))
            }
        }
    }

    if (showBpDialog) {
        var bpInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBpDialog = false },
            title = { Text("Log Blood Pressure") },
            text = {
                OutlinedTextField(
                    value = bpInput,
                    onValueChange = { bpInput = it },
                    label = { Text("Systolic/Diastolic (e.g. 120/80)") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (bpInput.isNotBlank()) {
                        viewModel.setBloodPressure(bpInput)
                    }
                    showBpDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBmiDialog) {
        var weightInput by remember { mutableStateOf(profile?.weightKg?.takeIf { it > 0 }?.toString() ?: "70.0") }
        var heightInput by remember { mutableStateOf(profile?.heightCm?.takeIf { it > 0 }?.toString() ?: "170.0") }
        var showWeightDialog by remember { mutableStateOf(false) }
        var showHeightDialog by remember { mutableStateOf(false) }

        if (showWeightDialog) {
            com.example.ui.components.HeightWeightPickerDialog(
                mode = com.example.ui.components.PickerMode.WEIGHT,
                initialValue = weightInput.toFloatOrNull() ?: 70f,
                onDismiss = { showWeightDialog = false },
                onConfirm = { kg ->
                    weightInput = kg.toString()
                    showWeightDialog = false
                }
            )
        }

        if (showHeightDialog) {
            com.example.ui.components.HeightWeightPickerDialog(
                mode = com.example.ui.components.PickerMode.HEIGHT,
                initialValue = heightInput.toFloatOrNull() ?: 170f,
                onDismiss = { showHeightDialog = false },
                onConfirm = { cm ->
                    heightInput = cm.toString()
                    showHeightDialog = false
                }
            )
        }
        
        val w = weightInput.toFloatOrNull() ?: 0f
        val h = heightInput.toFloatOrNull()?.div(100f) ?: 0f
        val calcBmi = if (h > 0) w / (h * h) else 0f
        
        AlertDialog(
            onDismissRequest = { showBmiDialog = false },
            title = { Text("BMI Calculator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "${weightInput.toFloatOrNull() ?: 70f} kg",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Weight") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showWeightDialog = true })
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "${heightInput.toFloatOrNull() ?: 170f} cm",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Height") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showHeightDialog = true })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "BMI: ${String.format("%.1f", calcBmi)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                    val status = when {
                        calcBmi == 0f -> ""
                        calcBmi < 18.5f -> "Underweight"
                        calcBmi < 25f -> "Normal"
                        calcBmi < 30f -> "Overweight"
                        else -> "Obese"
                    }
                    Text(text = status, fontSize = 16.sp, color = Slate600)
                }
            },
            confirmButton = {
                TextButton(onClick = { showBmiDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
