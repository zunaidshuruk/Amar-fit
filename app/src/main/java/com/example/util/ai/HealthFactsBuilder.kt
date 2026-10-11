package com.example.util.ai

import com.example.data.local.DailyMetric
import com.example.data.local.UserProfile
import com.example.util.WalkingMetrics
import com.example.util.formatGlucose
import com.example.util.formatWeight
import java.util.Locale
import kotlin.math.roundToInt

enum class HealthTopic(val label: String) {
    SLEEP("sleep"),
    STEPS("steps"),
    WEIGHT("weight"),
    GLUCOSE("glucose"),
    BLOOD_PRESSURE("blood pressure"),
    HEART("heart"),
    WATER("water"),
    CALORIES("food and calories"),
    ACTIVITY("exercise"),
    MINDFULNESS("mindfulness")
}

object HealthFactsBuilder {

    fun detectTopics(message: String): Set<HealthTopic> {
        val lower = message.lowercase()
        val detected = mutableSetOf<HealthTopic>()

        if (containsAny(lower, "sleep", "slept", "insomnia", "bedtime", "ঘুম")) detected.add(HealthTopic.SLEEP)
        if (containsAny(lower, "step", "walked", "distance")) detected.add(HealthTopic.STEPS)
        if (containsAny(lower, "weight", "weigh", "bmi", "ওজন")) detected.add(HealthTopic.WEIGHT)
        if (containsAny(lower, "glucose", "blood sugar", "sugar level", "sugar reading", "সুগার")) detected.add(HealthTopic.GLUCOSE)
        if (containsAny(lower, "blood pressure", " bp", "systolic", "diastolic", "প্রেসার")) detected.add(HealthTopic.BLOOD_PRESSURE)
        if (containsAny(lower, "heart rate", "resting heart", "pulse", "hrv", "spo2", "oxygen")) detected.add(HealthTopic.HEART)
        if (containsAny(lower, "water intake", "how much water", "hydrat")) detected.add(HealthTopic.WATER)
        if (containsAny(lower, "calorie", "kcal", "carbs", "protein", "macro", "ক্যালরি")) detected.add(HealthTopic.CALORIES)
        if (containsAny(lower, "exercise", "workout", "active minutes")) detected.add(HealthTopic.ACTIVITY)
        if (containsAny(lower, "meditat", "mindful")) detected.add(HealthTopic.MINDFULNESS)

        return detected.take(3).toSet()
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }

    fun isPersonalDataQuestion(message: String): Boolean {
        val topics = detectTopics(message)
        if (topics.isEmpty()) return false
        val lower = message.lowercase()
        val triggers = listOf(
            "my ", "did i", "have i", "how much did i", "how many", "how was", "how did i",
            "today", "yesterday", "last night", "this week", "last week", "this month", "last month",
            "average", "lately", "recently", "আমার", "আজ", "গত"
        )
        return triggers.any { lower.contains(it) }
    }

    fun build(
        topics: Set<HealthTopic>,
        profile: UserProfile,
        metrics: List<DailyMetric>,
        todayDate: String
    ): String {
        if (topics.isEmpty()) return ""

        val sortedMetrics = metrics.sortedBy { it.date }
        val filteredMetrics = sortedMetrics.filter { it.date <= todayDate }

        val recent7 = filteredMetrics.takeLast(7)
        val prev7 = filteredMetrics.dropLast(7).takeLast(7)
        val last30 = filteredMetrics.takeLast(30)

        val sb = StringBuilder()
        sb.append("FACTS (computed by the app from the user's own records; use only these numbers):\n")

        for (topic in topics) {
            val line = buildTopicLine(topic, profile, recent7, prev7, last30, todayDate)
            sb.append(line).append("\n")
        }

        return sb.toString().trimEnd()
    }

    private fun buildTopicLine(
        topic: HealthTopic,
        profile: UserProfile,
        recent7: List<DailyMetric>,
        prev7: List<DailyMetric>,
        last30: List<DailyMetric>,
        todayDate: String
    ): String {
        return when (topic) {
            HealthTopic.SLEEP -> {
                val valid30 = last30.filter { it.sleepHours > 0f }
                if (valid30.isEmpty()) return "SLEEP: no records in the last 30 days."
                val lastNight = valid30.last()
                val valid7 = recent7.filter { it.sleepHours > 0f }
                val avg7 = if (valid7.isNotEmpty()) valid7.map { it.sleepHours }.average().toFloat() else 0f
                val avg30 = valid30.map { it.sleepHours }.average().toFloat()
                val best7 = if (valid7.isNotEmpty()) valid7.maxOf { it.sleepHours } else 0f
                val shortest7 = if (valid7.isNotEmpty()) valid7.minOf { it.sleepHours } else 0f
                val metGoalCount = if (profile.sleepGoalHours > 0f) valid7.count { it.sleepHours >= profile.sleepGoalHours } else 0

                val validPrev7 = prev7.filter { it.sleepHours > 0f }
                val avgPrev7 = if (validPrev7.isNotEmpty()) validPrev7.map { it.sleepHours }.average().toFloat() else 0f
                val diffPrev = avg7 - avgPrev7

                val parts = mutableListOf<String>()
                parts.add(String.format(Locale.US, "last night %.1f h (%s)", lastNight.sleepHours, lastNight.date))
                if (avg7 > 0f) parts.add(String.format(Locale.US, "7-day avg %.1f h over %d nights", avg7, valid7.size))
                parts.add(String.format(Locale.US, "30-day avg %.1f h", avg30))
                if (best7 > 0f) parts.add(String.format(Locale.US, "7-day best %.1f h, shortest %.1f h", best7, shortest7))
                if (profile.sleepGoalHours > 0f) parts.add(String.format(Locale.US, "goal %.1f h met %d/7 nights", profile.sleepGoalHours, metGoalCount))
                if (validPrev7.isNotEmpty()) parts.add(String.format(Locale.US, "change vs prev week %+.1f h", diffPrev))

                "SLEEP: " + parts.joinToString("; ") + "."
            }

            HealthTopic.STEPS -> {
                val valid30 = last30.filter { it.steps > 0 }
                if (valid30.isEmpty()) return "STEPS: no records in the last 30 days."
                val todayMetric = last30.find { it.date == todayDate }
                val valid7 = recent7.filter { it.steps > 0 }
                val avg7 = if (valid7.isNotEmpty()) valid7.map { it.steps }.average().roundToInt() else 0
                val avg30 = valid30.map { it.steps }.average().roundToInt()
                val best7 = if (valid7.isNotEmpty()) valid7.maxOf { it.steps } else 0
                val metGoalCount = if (profile.stepGoal > 0) valid7.count { it.steps >= profile.stepGoal } else 0
                val avgDist7Meters = if (valid7.isNotEmpty()) valid7.map { it.distanceMeters }.average().toFloat() else 0f
                val distStr = if (avgDist7Meters > 0f) WalkingMetrics.formatDistance(avgDist7Meters, profile.useImperialUnits) else ""

                val parts = mutableListOf<String>()
                if (todayMetric != null && todayMetric.steps > 0) parts.add("today ${todayMetric.steps} steps")
                if (avg7 > 0) parts.add("7-day avg $avg7 steps")
                parts.add("30-day avg $avg30 steps")
                if (best7 > 0) parts.add("7-day best $best7 steps")
                if (profile.stepGoal > 0) parts.add("goal ${profile.stepGoal} met $metGoalCount/7 days")
                if (distStr.isNotBlank()) parts.add("7-day avg dist $distStr")

                "STEPS: " + parts.joinToString("; ") + "."
            }

            HealthTopic.WEIGHT -> {
                val valid30 = last30.filter { it.weightKg > 0f }
                if (valid30.isEmpty() && profile.weightKg <= 0f) return "WEIGHT: no records in the last 30 days."
                val latest = valid30.lastOrNull()
                val weightVal = latest?.weightKg ?: profile.weightKg
                val weightDate = latest?.date ?: "profile"
                val weightStr = formatWeight(weightVal, profile.useImperialUnits)

                val parts = mutableListOf<String>()
                parts.add("latest $weightStr ($weightDate)")

                if (valid30.size >= 2) {
                    val oldestVal = valid30.first().weightKg
                    val newestVal = valid30.last().weightKg
                    val diffKg = newestVal - oldestVal
                    val diffStr = formatWeight(kotlin.math.abs(diffKg), profile.useImperialUnits)
                    val sign = if (diffKg >= 0f) "+" else "-"
                    parts.add("30-day change $sign$diffStr")
                }

                "WEIGHT: " + parts.joinToString("; ") + "."
            }

            HealthTopic.GLUCOSE -> {
                data class GlucoseEntry(val label: String, val value: Float, val date: String)
                val entries = mutableListOf<GlucoseEntry>()
                for (m in recent7) {
                    if (m.bloodGlucoseBeforeBreakfast > 0f) entries.add(GlucoseEntry("Fasting", m.bloodGlucoseBeforeBreakfast, m.date))
                    if (m.bloodGlucoseAfterBreakfast > 0f) entries.add(GlucoseEntry("Post-Breakfast", m.bloodGlucoseAfterBreakfast, m.date))
                    if (m.bloodGlucoseBeforeLunch > 0f) entries.add(GlucoseEntry("Pre-Lunch", m.bloodGlucoseBeforeLunch, m.date))
                    if (m.bloodGlucoseAfterLunch > 0f) entries.add(GlucoseEntry("Post-Lunch", m.bloodGlucoseAfterLunch, m.date))
                    if (m.bloodGlucoseBeforeDinner > 0f) entries.add(GlucoseEntry("Pre-Dinner", m.bloodGlucoseBeforeDinner, m.date))
                    if (m.bloodGlucoseAfterDinner > 0f) entries.add(GlucoseEntry("Post-Dinner", m.bloodGlucoseAfterDinner, m.date))
                    if (m.bloodGlucoseMorning > 0f) entries.add(GlucoseEntry("Morning", m.bloodGlucoseMorning, m.date))
                    if (m.bloodGlucoseNight > 0f) entries.add(GlucoseEntry("Night", m.bloodGlucoseNight, m.date))
                }

                if (entries.isEmpty()) return "GLUCOSE: no records in the last 30 days."

                val latest = entries.last()
                val latestStr = formatGlucose(latest.value, profile.glucoseUnitMgdl)
                val avgVal = entries.map { it.value }.average().toFloat()
                val avgStr = formatGlucose(avgVal, profile.glucoseUnitMgdl)
                val minVal = entries.minOf { it.value }
                val minStr = formatGlucose(minVal, profile.glucoseUnitMgdl)
                val maxVal = entries.maxOf { it.value }
                val maxStr = formatGlucose(maxVal, profile.glucoseUnitMgdl)

                val parts = mutableListOf<String>()
                parts.add("latest $latestStr (${latest.label}, ${latest.date})")
                parts.add("7-day avg $avgStr over ${entries.size} readings")
                parts.add("lowest $minStr, highest $maxStr")

                if (profile.bloodGlucoseTargetMin > 0f && profile.bloodGlucoseTargetMax > 0f) {
                    val inRangeCount = entries.count { it.value in profile.bloodGlucoseTargetMin..profile.bloodGlucoseTargetMax }
                    parts.add("target range met $inRangeCount/${entries.size} times")
                }

                "GLUCOSE: " + parts.joinToString("; ") + "."
            }

            HealthTopic.BLOOD_PRESSURE -> {
                val valid30 = last30.filter { !it.bloodPressure.isNullOrBlank() }
                if (valid30.isEmpty()) return "BLOOD PRESSURE: no records in the last 30 days."

                val latest3 = valid30.takeLast(3)
                val readStrs = latest3.map { "${it.bloodPressure} (${it.date})" }

                val valid7 = recent7.filter { !it.bloodPressure.isNullOrBlank() }
                var avgSys = 0
                var avgDia = 0
                var count = 0
                val bpRegex = Regex("""(\d+)\s*/\s*(\d+)""")
                for (m in valid7) {
                    val match = bpRegex.find(m.bloodPressure ?: "")
                    if (match != null) {
                        avgSys += match.groupValues[1].toInt()
                        avgDia += match.groupValues[2].toInt()
                        count++
                    }
                }

                val parts = mutableListOf<String>()
                parts.add("latest " + readStrs.joinToString(", "))
                if (count > 0) {
                    val sys = (avgSys.toDouble() / count).roundToInt()
                    val dia = (avgDia.toDouble() / count).roundToInt()
                    parts.add("7-day avg $sys/$dia")
                }

                "BLOOD PRESSURE: " + parts.joinToString("; ") + "."
            }

            HealthTopic.HEART -> {
                val valid30 = last30.filter { it.restingHeartRate > 0 || it.heartRateVariability > 0f || it.oxygenSaturation > 0f }
                if (valid30.isEmpty()) return "HEART: no records in the last 30 days."

                val valid7Rest = recent7.filter { it.restingHeartRate > 0 }
                val valid7Hrv = recent7.filter { it.heartRateVariability > 0f }
                val valid7Spo2 = recent7.filter { it.oxygenSaturation > 0f }

                val parts = mutableListOf<String>()

                if (valid30.any { it.restingHeartRate > 0 }) {
                    val latestRest = valid30.last { it.restingHeartRate > 0 }
                    val avgRest7 = if (valid7Rest.isNotEmpty()) valid7Rest.map { it.restingHeartRate }.average().roundToInt() else 0
                    if (avgRest7 > 0) {
                        parts.add("resting HR latest ${latestRest.restingHeartRate} bpm (${latestRest.date}), 7-day avg $avgRest7 bpm")
                    } else {
                        parts.add("resting HR latest ${latestRest.restingHeartRate} bpm (${latestRest.date})")
                    }
                }

                if (valid30.any { it.heartRateVariability > 0f }) {
                    val latestHrv = valid30.last { it.heartRateVariability > 0f }
                    val avgHrv7 = if (valid7Hrv.isNotEmpty()) valid7Hrv.map { it.heartRateVariability }.average().toFloat() else 0f
                    if (avgHrv7 > 0f) {
                        parts.add(String.format(Locale.US, "HRV latest %.1f ms, 7-day avg %.1f ms", latestHrv.heartRateVariability, avgHrv7))
                    } else {
                        parts.add(String.format(Locale.US, "HRV latest %.1f ms", latestHrv.heartRateVariability))
                    }
                }

                if (valid30.any { it.oxygenSaturation > 0f }) {
                    val latestSpo2 = valid30.last { it.oxygenSaturation > 0f }
                    val avgSpo27 = if (valid7Spo2.isNotEmpty()) valid7Spo2.map { it.oxygenSaturation }.average().toFloat() else 0f
                    if (avgSpo27 > 0f) {
                        parts.add(String.format(Locale.US, "SpO2 latest %.1f%%, 7-day avg %.1f%%", latestSpo2.oxygenSaturation, avgSpo27))
                    } else {
                        parts.add(String.format(Locale.US, "SpO2 latest %.1f%%", latestSpo2.oxygenSaturation))
                    }
                }

                "HEART: " + parts.joinToString("; ") + "."
            }

            HealthTopic.WATER -> {
                val valid30 = last30.filter { it.waterLiters > 0f }
                if (valid30.isEmpty()) return "WATER: no records in the last 30 days."

                val todayMetric = last30.find { it.date == todayDate }
                val valid7 = recent7.filter { it.waterLiters > 0f }
                val avg7 = if (valid7.isNotEmpty()) valid7.map { it.waterLiters }.average().toFloat() else 0f

                val parts = mutableListOf<String>()
                if (todayMetric != null && todayMetric.waterLiters > 0f) {
                    parts.add(String.format(Locale.US, "today %.1f L", todayMetric.waterLiters))
                }
                if (avg7 > 0f) parts.add(String.format(Locale.US, "7-day avg %.1f L", avg7))
                if (profile.dailyWaterLimitLiters > 0f) {
                    parts.add(String.format(Locale.US, "daily limit %.1f L", profile.dailyWaterLimitLiters))
                }

                "WATER: " + parts.joinToString("; ") + "."
            }

            HealthTopic.CALORIES -> {
                val valid30 = last30.filter { it.caloriesConsumed > 0 }
                if (valid30.isEmpty()) return "CALORIES: no records in the last 30 days."

                val todayMetric = last30.find { it.date == todayDate }
                val valid7 = recent7.filter { it.caloriesConsumed > 0 }
                val avg7 = if (valid7.isNotEmpty()) valid7.map { it.caloriesConsumed }.average().roundToInt() else 0
                val valid7Active = recent7.filter { it.activeCaloriesBurned > 0 }
                val avgActive7 = if (valid7Active.isNotEmpty()) valid7Active.map { it.activeCaloriesBurned }.average().roundToInt() else 0

                val parts = mutableListOf<String>()
                if (todayMetric != null && todayMetric.caloriesConsumed > 0) {
                    parts.add("today ${todayMetric.caloriesConsumed} kcal (carbs ${todayMetric.carbsG.roundToInt()}g, protein ${todayMetric.proteinG.roundToInt()}g, fat ${todayMetric.fatG.roundToInt()}g)")
                }
                if (avg7 > 0) parts.add("7-day avg $avg7 kcal consumed")
                if (profile.dailyCalorieLimit > 0) parts.add("daily limit ${profile.dailyCalorieLimit} kcal")
                if (avgActive7 > 0) parts.add("7-day avg active burned $avgActive7 kcal")

                "CALORIES: " + parts.joinToString("; ") + "."
            }

            HealthTopic.ACTIVITY -> {
                val valid30 = last30.filter { it.exerciseMinutes > 0 || it.activeCaloriesBurned > 0 }
                if (valid30.isEmpty()) return "EXERCISE: no records in the last 30 days."

                val valid7Ex = recent7.filter { it.exerciseMinutes > 0 }
                val avgEx7 = if (valid7Ex.isNotEmpty()) valid7Ex.map { it.exerciseMinutes }.average().roundToInt() else 0
                val valid7Act = recent7.filter { it.activeCaloriesBurned > 0 }
                val avgAct7 = if (valid7Act.isNotEmpty()) valid7Act.map { it.activeCaloriesBurned }.average().roundToInt() else 0

                val parts = mutableListOf<String>()
                if (avgEx7 > 0) parts.add("7-day avg $avgEx7 min exercise")
                parts.add("${valid7Ex.size}/7 days with exercise")
                if (avgAct7 > 0) parts.add("7-day avg $avgAct7 active kcal burned")

                "EXERCISE: " + parts.joinToString("; ") + "."
            }

            HealthTopic.MINDFULNESS -> {
                val valid30 = last30.filter { it.mindfulnessMinutes > 0 }
                if (valid30.isEmpty()) return "MINDFULNESS: no records in the last 30 days."

                val valid7 = recent7.filter { it.mindfulnessMinutes > 0 }
                val totalMin7 = valid7.sumOf { it.mindfulnessMinutes }

                val parts = mutableListOf<String>()
                parts.add("last 7 days total $totalMin7 min")
                parts.add("${valid7.size}/7 days with session")

                "MINDFULNESS: " + parts.joinToString("; ") + "."
            }
        }
    }

    fun sourceLabels(topics: Set<HealthTopic>): String {
        if (topics.isEmpty()) return ""
        val labels = topics.joinToString(", ") { it.label }
        return "$labels (last 30 days)"
    }
}
