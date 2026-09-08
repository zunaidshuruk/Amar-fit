package com.example.presentation.today

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.example.data.local.DailyMetric
import com.example.data.local.FoodLog
import com.example.data.local.UserProfile
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.AccentColors
import com.example.ui.theme.AccentTokens
import java.util.Locale

val ALL_LARGE_TILE_IDS = listOf(
    "large_steps",
    "large_weekly_cardio"
)

val ALL_SMALL_TILE_IDS = listOf(
    "steps",
    "sleep",
    "distance",
    "cal_burned",
    "heart_rate",
    "weight",
    "water",
    "blood_glucose",
    "blood_pressure",
    "mindfulness",
    "exercise_days",
    "exercise_minutes",
    "hrv",
    "spo2",
    "skin_temp",
    "breathing_rate",
    "resilience",
    "food_calories"
)

fun parseTodayTileSlots(raw: String?): Pair<List<String>, List<String>> {
    val defaultLarge = listOf("large_steps")
    val defaultSmall = listOf("steps", "sleep", "distance", "cal_burned", "exercise_days", "heart_rate")

    if (raw.isNullOrBlank()) {
        return Pair(defaultLarge, defaultSmall)
    }

    val tokens = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (tokens.isEmpty()) {
        return Pair(defaultLarge, defaultSmall)
    }

    val newTokensSet = setOf(
        "large_steps", "large_weekly_cardio",
        "distance", "cal_burned", "exercise_days", "exercise_minutes",
        "food_calories", "mindfulness", "hrv", "spo2", "skin_temp",
        "breathing_rate", "resilience"
    )

    val hasNewTokens = tokens.any { it.startsWith("large_") || it in newTokensSet }

    if (hasNewTokens) {
        val validLarge = ALL_LARGE_TILE_IDS.toSet()
        val validSmall = ALL_SMALL_TILE_IDS.toSet()
        val largeList = mutableListOf<String>()
        val smallList = mutableListOf<String>()

        tokens.forEach { token ->
            val normalized = if (token == "calories") "food_calories" else token
            if (normalized in validLarge && normalized !in largeList) {
                largeList.add(normalized)
            } else if (normalized in validSmall && normalized !in smallList) {
                smallList.add(normalized)
            }
        }
        return Pair(largeList, smallList)
    } else {
        // Old format (e.g. "steps,water,calories")
        val smallList = mutableListOf<String>()
        tokens.forEach { oldToken ->
            val mapped = when (oldToken) {
                "steps" -> "steps"
                "water" -> "water"
                "calories" -> "food_calories"
                "weight" -> "weight"
                "sleep" -> "sleep"
                "blood_glucose" -> "blood_glucose"
                "blood_pressure" -> "blood_pressure"
                else -> null
            }
            if (mapped != null && mapped in ALL_SMALL_TILE_IDS && mapped !in smallList) {
                smallList.add(mapped)
            }
        }
        val finalSmall = if (smallList.isNotEmpty()) smallList else defaultSmall
        return Pair(defaultLarge, finalSmall)
    }
}

data class ResolvedLargeTile(
    val id: String,
    val title: String,
    val progress: Float,
    val insideValue: String,
    val insideSubtext: String,
    val accent: AccentColors,
    val onClick: () -> Unit
)

data class ResolvedSmallTile(
    val id: String,
    val label: String,
    val value: String,
    val icon: ImageVector,
    val accent: AccentColors?,
    val isMuted: Boolean,
    val onClick: () -> Unit
)

fun resolveLargeTile(
    id: String,
    metrics: DailyMetric?,
    last7Metrics: List<DailyMetric>,
    isDark: Boolean,
    onOpenStepsDialog: () -> Unit,
    onNavigateToFitness: () -> Unit
): ResolvedLargeTile? {
    return when (id) {
        "large_steps" -> {
            val steps = metrics?.steps ?: 0
            val progress = (steps.toFloat() / 10000f).coerceIn(0f, 1f)
            val accent = AccentTokens.stepsAccent(isDark)
            ResolvedLargeTile(
                id = "large_steps",
                title = "Daily Steps",
                progress = progress,
                insideValue = String.format(Locale.US, "%,d", steps),
                insideSubtext = "of 10,000",
                accent = accent,
                onClick = onOpenStepsDialog
            )
        }
        "large_weekly_cardio" -> {
            val weeklyExerciseMinutes = last7Metrics.sumOf { it.exerciseMinutes }
            val cardioGoal = 150
            val progress = (weeklyExerciseMinutes.toFloat() / cardioGoal.toFloat()).coerceIn(0f, 1f)
            val accent = AccentTokens.pointsAccent(isDark)
            ResolvedLargeTile(
                id = "large_weekly_cardio",
                title = "Weekly Cardio",
                progress = progress,
                insideValue = "$weeklyExerciseMinutes",
                insideSubtext = "of $cardioGoal min",
                accent = accent,
                onClick = onNavigateToFitness
            )
        }
        else -> null
    }
}

fun resolveSmallTile(
    id: String,
    metrics: DailyMetric?,
    profile: UserProfile?,
    last7Metrics: List<DailyMetric>,
    todayFoodLogs: List<FoodLog>,
    isDark: Boolean,
    navController: NavController,
    onOpenStepsDialog: () -> Unit,
    onOpenWaterDialog: () -> Unit,
    onNavigateToTab: (String) -> Unit
): ResolvedSmallTile? {
    val stepsAccent = AccentTokens.stepsAccent(isDark)
    val caloriesAccent = AccentTokens.caloriesAccent(isDark)
    val waterAccent = AccentTokens.waterAccent(isDark)
    val weightAccent = AccentTokens.weightAccent(isDark)
    val glucoseAccent = AccentTokens.glucoseAccent(isDark)
    val bloodPressureAccent = AccentTokens.bloodPressureAccent(isDark)
    val sleepAccent = AccentTokens.sleepAccent(isDark)
    val pointsAccent = AccentTokens.pointsAccent(isDark)
    val heartRateAccent = AccentTokens.heartRateAccent(isDark)
    val mindfulnessAccent = AccentTokens.mindfulnessAccent(isDark)
    val resilienceAccent = AccentTokens.resilienceAccent(isDark)

    return when (id) {
        "steps" -> {
            val steps = metrics?.steps ?: 0
            ResolvedSmallTile(
                id = "steps",
                label = "Steps",
                value = String.format(Locale.US, "%,d", steps),
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                accent = stepsAccent,
                isMuted = false,
                onClick = onOpenStepsDialog
            )
        }
        "sleep" -> {
            val sleepHrs = metrics?.sleepHours ?: 0f
            ResolvedSmallTile(
                id = "sleep",
                label = "Sleep",
                value = if (sleepHrs > 0f) "${String.format(Locale.US, "%.1f", sleepHrs)} hrs" else "No data",
                icon = Icons.Default.Bedtime,
                accent = if (sleepHrs > 0f) sleepAccent else null,
                isMuted = sleepHrs <= 0f,
                onClick = { onNavigateToTab("sleep") }
            )
        }
        "distance" -> {
            val distanceKm = (metrics?.distanceMeters ?: 0f) / 1000f
            ResolvedSmallTile(
                id = "distance",
                label = "Distance",
                value = "${String.format(Locale.US, "%.1f", distanceKm)} km",
                icon = Icons.Default.Straighten,
                accent = stepsAccent,
                isMuted = false,
                // MetricDetailScreen has no "distance" drill-down (its metricKey switch has no
                // matching branch — see hasData/latestVal/averageVal in MetricDetailScreen.kt),
                // so this tile is display-only for now rather than opening a screen that would
                // always show "Metric Detail" / "--" with no chart.
                onClick = {}
            )
        }
        "cal_burned" -> {
            val calBurned = metrics?.activeCaloriesBurned ?: 0
            ResolvedSmallTile(
                id = "cal_burned",
                label = "Cal burned",
                value = "$calBurned",
                icon = Icons.Default.LocalFireDepartment,
                accent = caloriesAccent,
                isMuted = false,
                onClick = { navController.navigate("metric_detail/activeCaloriesBurned") }
            )
        }
        "heart_rate" -> {
            val heartRate = metrics?.heartRate ?: 0
            ResolvedSmallTile(
                id = "heart_rate",
                label = "Heart rate",
                value = if (heartRate > 0) "$heartRate bpm" else "--",
                icon = Icons.Default.Favorite,
                accent = if (heartRate > 0) heartRateAccent else null,
                isMuted = heartRate <= 0,
                onClick = { navController.navigate("metric_detail/heartRate") }
            )
        }
        "weight" -> {
            val currentWeight = if ((metrics?.weightKg ?: 0f) > 0f) metrics?.weightKg ?: 0f else profile?.weightKg ?: 0f
            ResolvedSmallTile(
                id = "weight",
                label = "Weight",
                value = if (currentWeight > 0f) "${String.format(Locale.US, "%.1f", currentWeight)} kg" else "--",
                icon = Icons.Default.MonitorWeight,
                accent = if (currentWeight > 0f) weightAccent else null,
                isMuted = currentWeight <= 0f,
                onClick = { navController.navigate("weightlog") }
            )
        }
        "water" -> {
            val waterConsumed = metrics?.waterLiters ?: 0f
            ResolvedSmallTile(
                id = "water",
                label = "Water",
                value = "${String.format(Locale.US, "%.1f", waterConsumed)} L",
                icon = Icons.Default.LocalDrink,
                accent = waterAccent,
                isMuted = false,
                onClick = onOpenWaterDialog
            )
        }
        "blood_glucose" -> {
            val glucose = maxOf(metrics?.bloodGlucoseMorning ?: 0f, metrics?.bloodGlucoseNight ?: 0f)
            ResolvedSmallTile(
                id = "blood_glucose",
                label = "Blood Glucose",
                value = if (glucose > 0f) "${String.format(Locale.US, "%.1f", glucose)} mg/dL" else "--",
                icon = Icons.Default.Favorite,
                accent = if (glucose > 0f) glucoseAccent else null,
                isMuted = glucose <= 0f,
                onClick = { navController.navigate("glucoselog") }
            )
        }
        "blood_pressure" -> {
            val bp = metrics?.bloodPressure?.takeIf { it.isNotBlank() } ?: ""
            ResolvedSmallTile(
                id = "blood_pressure",
                label = "Blood Pressure",
                value = if (bp.isNotBlank()) "$bp mmHg" else "--",
                icon = Icons.Default.MonitorHeart,
                accent = if (bp.isNotBlank()) bloodPressureAccent else null,
                isMuted = bp.isBlank(),
                onClick = { onNavigateToTab("health") }
            )
        }
        "mindfulness" -> {
            val minutes = metrics?.mindfulnessMinutes ?: 0
            ResolvedSmallTile(
                id = "mindfulness",
                label = "Mindfulness",
                value = "$minutes min",
                icon = Icons.Default.SelfImprovement,
                accent = mindfulnessAccent,
                isMuted = false,
                onClick = { navController.navigate("mindfulness_timer") }
            )
        }
        "exercise_days" -> {
            val exerciseDays = last7Metrics.count { it.exerciseMinutes > 0 }
            ResolvedSmallTile(
                id = "exercise_days",
                label = "Exercise days",
                value = "$exerciseDays/7",
                icon = Icons.Default.FitnessCenter,
                accent = pointsAccent,
                isMuted = false,
                onClick = { onNavigateToTab("fitness") }
            )
        }
        "exercise_minutes" -> {
            val minutes = metrics?.exerciseMinutes ?: 0
            ResolvedSmallTile(
                id = "exercise_minutes",
                label = "Exercise",
                value = "$minutes min",
                icon = Icons.Default.Timer,
                accent = pointsAccent,
                isMuted = false,
                onClick = { onNavigateToTab("fitness") }
            )
        }
        "hrv" -> {
            val hrv = metrics?.heartRateVariability ?: 0f
            ResolvedSmallTile(
                id = "hrv",
                label = "HRV",
                value = if (hrv > 0f) "${hrv.toInt()} ms" else "--",
                icon = Icons.Default.Timeline,
                accent = if (hrv > 0f) waterAccent else null,
                isMuted = hrv <= 0f,
                onClick = { navController.navigate("metric_detail/heartRateVariability") }
            )
        }
        "spo2" -> {
            val spo2 = metrics?.oxygenSaturation ?: 0f
            ResolvedSmallTile(
                id = "spo2",
                label = "SpO2",
                value = if (spo2 > 0f) "${spo2.toInt()}%" else "--",
                icon = Icons.Default.Air,
                accent = if (spo2 > 0f) heartRateAccent else null,
                isMuted = spo2 <= 0f,
                onClick = { navController.navigate("metric_detail/oxygenSaturation") }
            )
        }
        "skin_temp" -> {
            val skinTemp = metrics?.skinTemperatureCelsius ?: 0f
            ResolvedSmallTile(
                id = "skin_temp",
                label = "Skin Temp",
                value = if (skinTemp > 0f) "${String.format(Locale.US, "%.1f", skinTemp)} °C" else "--",
                icon = Icons.Default.Thermostat,
                accent = if (skinTemp > 0f) caloriesAccent else null,
                isMuted = skinTemp <= 0f,
                onClick = { navController.navigate("metric_detail/skinTemperatureCelsius") }
            )
        }
        "breathing_rate" -> {
            val rate = metrics?.respiratoryRate ?: 0f
            ResolvedSmallTile(
                id = "breathing_rate",
                label = "Breathing Rate",
                value = if (rate > 0f) "${rate.toInt()} breaths/min" else "--",
                icon = Icons.Default.Waves,
                accent = if (rate > 0f) sleepAccent else null,
                isMuted = rate <= 0f,
                onClick = { navController.navigate("metric_detail/respiratoryRate") }
            )
        }
        "resilience" -> {
            val resResult = ShasthoViewModel.calculateResilienceScore(last7Metrics)
            val score = resResult.score
            val bucket = resResult.bucket
            val valText = if (score != null) "$score ($bucket)" else "No data"
            ResolvedSmallTile(
                id = "resilience",
                label = "Resilience",
                value = valText,
                icon = Icons.Default.Shield,
                accent = if (score != null) resilienceAccent else null,
                isMuted = score == null,
                onClick = { onNavigateToTab("health") }
            )
        }
        "food_calories" -> {
            val totalCalories = todayFoodLogs.sumOf { it.calories }
            ResolvedSmallTile(
                id = "food_calories",
                label = "Food Calories",
                value = "$totalCalories kcal",
                icon = Icons.Default.Restaurant,
                accent = caloriesAccent,
                isMuted = false,
                onClick = { onNavigateToTab("nutrition") }
            )
        }
        else -> null
    }
}
