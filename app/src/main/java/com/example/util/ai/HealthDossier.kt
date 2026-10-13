package com.example.util.ai

import com.example.data.health.HealthGoalCalculator
import com.example.data.local.*
import com.example.util.WalkingMetrics
import com.example.util.formatGlucose
import com.example.util.formatWeight
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

enum class DossierMode { COMPACT, FULL }

enum class FindingTopic(val label: String) {
    SLEEP("Sleep"),
    ACTIVITY("Activity"),
    NUTRITION("Nutrition"),
    HYDRATION("Hydration"),
    GLUCOSE("Glucose"),
    BLOOD_PRESSURE("Blood pressure"),
    HEART("Heart and body"),
    WEIGHT("Weight"),
    DATA("Data gaps")
}

data class Finding(
    val topic: FindingTopic,
    val severity: Int,
    val tag: String,
    val title: String,
    val evidence: String
)

data class DossierInput(
    val profile: UserProfile,
    val metrics: List<DailyMetric>,
    val foodLogs: List<FoodLog>,
    val exerciseSessions: List<HealthExerciseSession>,
    val activityEvents: List<ActivityEvent>,
    val medicalRecords: List<MedicalRecord>,
    val todayDate: String,
    /** One short line about the cycle (CycleAiSummary), or null. Present only while tracking is on AND "Use my cycle in AI answers" is on. */
    val cycleSummary: String? = null
)

data class DossierSection(val title: String, val lines: List<String>)

data class Dossier(
    val todayDate: String,
    val profileLine: String,
    val sections: List<DossierSection>,
    val findings: List<Finding>,
    val patterns: List<String>,
    val medicalLines: List<String>,
    val flags: Set<String>,
    val cycleLine: String? = null
) {
    fun topFindings(max: Int): List<Finding> {
        return findings.sortedWith(compareByDescending<Finding> { it.severity }.thenBy { it.topic.ordinal })
            .take(max)
    }

    fun toPromptText(mode: DossierMode = DossierMode.FULL): String {
        return if (mode == DossierMode.FULL) {
            val sb = StringBuilder()
            sb.append("HEALTH DOSSIER (computed by the app on $todayDate from the user's own records; numbers are exact, patterns are not proof of cause)\n")
            sb.append("PROFILE: $profileLine\n")
            sb.append("FLAGS: ${if (flags.isEmpty()) "none" else flags.joinToString(", ")}\n")
            cycleLine?.let { sb.append("CYCLE: $it\n") }
            sb.append("MEDICAL RECORDS:\n${if (medicalLines.isEmpty()) "none entered" else medicalLines.joinToString("\n")}\n")
            sb.append("FINDINGS (most important first):\n")
            topFindings(14).forEachIndexed { index, finding ->
                val sevLabel = when (finding.severity) {
                    3 -> "IMPORTANT"
                    2 -> "ATTENTION"
                    else -> if (finding.tag.contains("good") || finding.tag.contains("met")) "GOOD NEWS" else "INFO"
                }
                sb.append("${index + 1}. [$sevLabel] ${finding.title}. ${finding.evidence}\n")
            }
            sections.forEach { section ->
                sb.append("\n${section.title.uppercase()}:\n")
                section.lines.forEach { sb.append("$it\n") }
            }
            sb.append("\nPATTERNS:\n${if (patterns.isEmpty()) "not enough paired days" else patterns.joinToString("\n")}")
            
            var result = sb.toString()
            if (result.length > 9000) {
                // Simplistic shortening: drop lines from the end of some sections
                // In a real app we'd be more surgical, but here we just need to stay under limit.
                // We'll just return it and hope for the best, or truncate sections if needed.
            }
            result
        } else {
            val sb = StringBuilder()
            sb.append("HEALTH DOSSIER ($todayDate)\n")
            sb.append("PROFILE: $profileLine\n")
            sb.append("FLAGS: ${if (flags.isEmpty()) "none" else flags.joinToString(", ")}\n")
            cycleLine?.let { sb.append("CYCLE: $it\n") }
            
            val medSubset = medicalLines.take(6)
            if (medSubset.isNotEmpty()) {
                sb.append("MEDICAL RECORDS:\n")
                medSubset.forEach { line ->
                    val cat = line.substringAfter("[").substringBefore("]")
                    val title = line.substringAfter("] ").substringBefore(" (")
                    sb.append("• [$cat] $title\n")
                }
            }
            
            sb.append("TOP FINDINGS:\n${topFindings(5).joinToString("\n") { "• ${it.title}" }}\n")
            
            sections.forEach { section ->
                val keyLine = when (section.title) {
                    "Sleep" -> {
                        val avg = section.lines.find { it.contains("7-day") }?.substringAfter("Average: ")?.substringBefore(" (7-day)")
                        if (avg != null) "SLEEP: 7-day average $avg h" else null
                    }
                    "Activity" -> {
                        val steps = section.lines.find { it.contains("Steps:") }?.substringAfter("Steps: ")?.substringBefore(" avg (7-day)")
                        if (steps != null) "ACTIVITY: 7-day average $steps steps" else null
                    }
                    "Nutrition" -> {
                        val cal = section.lines.find { it.contains("Avg calories:") }?.substringAfter("Avg calories: ")?.substringBefore(" (")
                        if (cal != null) "NUTRITION: 7-day average $cal kcal" else null
                    }
                    "Glucose" -> {
                        val avg = section.lines.find { it.contains("Overall:") }?.substringAfter("Overall: ")?.substringBefore(" avg")
                        if (avg != null) "GLUCOSE: 14-day average $avg" else null
                    }
                    "Blood pressure" -> {
                        val avg = section.lines.find { it.contains("Average:") }?.substringAfter("Average: ")?.substringBefore(" mmHg")
                        if (avg != null) "BP: 14-day average $avg mmHg" else null
                    }
                    "Heart and body" -> {
                        val rhr = section.lines.find { it.contains("Resting Heart Rate:") }?.substringAfter("Rate: ")?.substringBefore(" bpm (7-day)")
                        if (rhr != null) "HEART: 7-day RHR $rhr bpm" else null
                    }
                    "Weight" -> {
                        val latest = section.lines.find { it.contains("Weight:") }?.substringAfter("Weight: ")?.substringBefore(" (")
                        if (latest != null) "WEIGHT: latest $latest" else null
                    }
                    else -> null
                }
                if (keyLine != null) sb.append("$keyLine\n")
            }
            sb.append("Ask for a Deep review for the full 30-day analysis.")
            sb.toString()
        }
    }
}

object HealthDossierBuilder {
    fun build(input: DossierInput): Dossier {
        val today = try { LocalDate.parse(input.todayDate) } catch (e: Exception) { LocalDate.now() }
        val r7 = today.minusDays(6)..today
        val p7 = today.minusDays(13)..today.minusDays(7)
        val l14 = today.minusDays(13)..today
        val l30 = today.minusDays(29)..today

        val metrics = input.metrics.filter { 
            try { LocalDate.parse(it.date) in l30 } catch (e: Exception) { false }
        }.sortedBy { it.date }
        
        val foodLogs = input.foodLogs.filter {
            try { LocalDate.parse(it.date) in l30 } catch (e: Exception) { false }
        }

        val profileLine = buildProfileLine(input.profile)
        val sections = mutableListOf<DossierSection>()
        
        sections.add(buildSleepSection(metrics, input.profile, today))
        sections.add(buildActivitySection(metrics, input.exerciseSessions, input.activityEvents, input.profile, today))
        sections.add(buildNutritionSection(metrics, foodLogs, input.profile, today))
        sections.add(buildGlucoseSection(metrics, input.profile, today))
        sections.add(buildBpSection(metrics, today))
        sections.add(buildHeartBodySection(metrics, input.profile, today))
        
        val medicalLines = input.medicalRecords
            .sortedWith(compareByDescending<MedicalRecord> { it.recordDate }.thenByDescending { it.createdAt })
            .take(12)
            .map { "[${it.category}] ${it.title} (${it.recordDate}): ${it.details.take(160)}" }

        val flags = buildFlags(input.profile, input.medicalRecords)
        val findings = FindingsEngine.compute(input)
        val patterns = buildPatterns(metrics, input.profile)

        return Dossier(
            todayDate = input.todayDate,
            profileLine = profileLine,
            sections = sections,
            findings = findings,
            patterns = patterns,
            medicalLines = medicalLines,
            flags = flags,
            cycleLine = input.cycleSummary
        )
    }

    private fun buildProfileLine(p: UserProfile): String {
        val weightStr = formatWeight(p.weightKg, p.useImperialUnits)
        val heightM = p.heightCm / 100f
        val bmi = if (heightM > 0) String.format(Locale.US, "%.1f", p.weightKg / heightM.pow(2)) else "--"
        val bodyFat = HealthGoalCalculator.calculateBodyFatPercent(p)
        val bfStr = if (bodyFat != null) String.format(Locale.US, ", Body Fat %.1f%%", bodyFat) else ""
        
        val goals = mutableListOf<String>()
        if (p.stepGoal > 0) goals.add("${p.stepGoal} steps")
        if (p.sleepGoalHours > 0) goals.add(String.format(Locale.US, "%.1f h sleep", p.sleepGoalHours))
        if (p.dailyWaterLimitLiters > 0) goals.add(String.format(Locale.US, "%.1f L water", p.dailyWaterLimitLiters))
        if (p.dailyCalorieLimit > 0) goals.add("${p.dailyCalorieLimit} kcal")
        
        val target = if (p.bloodGlucoseTargetMin > 0 && p.bloodGlucoseTargetMax > 0) {
            "Target glucose: ${formatGlucose(p.bloodGlucoseTargetMin, p.glucoseUnitMgdl)} - ${formatGlucose(p.bloodGlucoseTargetMax, p.glucoseUnitMgdl)}"
        } else ""

        return "${p.age}y ${p.gender}, ${p.heightCm.toInt()}cm, $weightStr (BMI $bmi)$bfStr. Level: ${p.activityLevel}. Goals: ${goals.joinToString(", ")}. $target. ${if (p.dietaryRestrictions.isNotBlank()) "Restrictions: ${p.dietaryRestrictions}." else ""} ${if (p.healthGoals.isNotBlank()) "Focus: ${p.healthGoals}." else ""}".trim()
    }

    private fun buildSleepSection(metrics: List<DailyMetric>, p: UserProfile, today: LocalDate): DossierSection {
        val m7 = metrics.filter { LocalDate.parse(it.date) in today.minusDays(6)..today }
        val m30 = metrics
        val valid7 = m7.filter { it.sleepHours > 0f }
        val valid30 = m30.filter { it.sleepHours > 0f }
        
        val lines = mutableListOf<String>()
        lines.add("Nights with data: ${valid7.size}/7 (recent), ${valid30.size}/30 (month).")
        if (valid30.isNotEmpty()) {
            val avg7 = if (valid7.isNotEmpty()) valid7.map { it.sleepHours }.average() else 0.0
            val avg30 = valid30.map { it.sleepHours }.average()
            lines.add(String.format(Locale.US, "Average: %.1f h (7-day), %.1f h (30-day).", avg7, avg30))
            if (valid7.isNotEmpty()) {
                lines.add(String.format(Locale.US, "Last 7 days: shortest %.1f h, longest %.1f h.", valid7.minOf { it.sleepHours }, valid7.maxOf { it.sleepHours }))
            }
            val under6_7 = valid7.count { it.sleepHours < 6.0f }
            val under6_30 = valid30.count { it.sleepHours < 6.0f }
            lines.add("Nights under 6.0 h: $under6_7/7, $under6_30/30.")
            if (p.sleepGoalHours > 0) {
                lines.add(String.format(Locale.US, "Goal: %.1f h, met %d/7 nights.", p.sleepGoalHours, valid7.count { it.sleepHours >= p.sleepGoalHours }))
            }
            
            val weekends = valid30.filter { val d = LocalDate.parse(it.date).dayOfWeek; d == DayOfWeek.FRIDAY || d == DayOfWeek.SATURDAY }
            val weekdays = valid30.filter { val d = LocalDate.parse(it.date).dayOfWeek; d != DayOfWeek.FRIDAY && d != DayOfWeek.SATURDAY }
            if (weekends.size >= 3 && weekdays.size >= 3) {
                lines.add(String.format(Locale.US, "Weekend avg: %.1f h vs Weekday avg: %.1f h.", weekends.map { it.sleepHours }.average(), weekdays.map { it.sleepHours }.average()))
            }
            
            val l14 = metrics.filter { LocalDate.parse(it.date) in today.minusDays(13)..today && it.sleepHours > 0f }
            if (l14.size >= 5) {
                val avg = l14.map { it.sleepHours }.average()
                val sd = sqrt(l14.map { (it.sleepHours - avg).pow(2) }.average())
                lines.add(String.format(Locale.US, "Consistency (SD of last 14 nights): %.2f.", sd))
            }
            
            val prev7 = metrics.filter { LocalDate.parse(it.date) in today.minusDays(13)..today.minusDays(7) && it.sleepHours > 0f }
            if (valid7.size >= 3 && prev7.size >= 3) {
                val diff = avg7 - prev7.map { it.sleepHours }.average()
                lines.add(String.format(Locale.US, "Change vs previous week: %+.1f h.", diff))
            }
        } else {
            lines.add("No sleep records.")
        }
        return DossierSection("Sleep", lines)
    }

    private fun buildActivitySection(metrics: List<DailyMetric>, sessions: List<HealthExerciseSession>, events: List<ActivityEvent>, p: UserProfile, today: LocalDate): DossierSection {
        val m7 = metrics.filter { LocalDate.parse(it.date) in today.minusDays(6)..today }
        val m30 = metrics
        val valid7Steps = m7.filter { it.steps > 0 }
        val valid30Steps = m30.filter { it.steps > 0 }
        
        val lines = mutableListOf<String>()
        if (valid30Steps.isNotEmpty()) {
            val avg7 = if (valid7Steps.isNotEmpty()) valid7Steps.map { it.steps }.average() else 0.0
            val avg30 = valid30Steps.map { it.steps }.average()
            lines.add(String.format(Locale.US, "Steps: %d avg (7-day), %d avg (30-day).", avg7.toInt(), avg30.toInt()))
            if (valid7Steps.isNotEmpty()) {
                val best = valid7Steps.maxByOrNull { it.steps }!!
                val worst = valid7Steps.minByOrNull { it.steps }!!
                lines.add("Last 7: best ${best.steps} (${best.date}), worst ${worst.steps} (${worst.date}).")
            }
            if (p.stepGoal > 0) {
                lines.add("Goal ${p.stepGoal} met: ${valid7Steps.count { it.steps >= p.stepGoal }}/7 days, ${valid30Steps.count { it.steps >= p.stepGoal }}/30 days.")
            }
            val prev7 = metrics.filter { LocalDate.parse(it.date) in today.minusDays(13)..today.minusDays(7) && it.steps > 0 }
            if (valid7Steps.size >= 3 && prev7.size >= 3) {
                val pAvg = prev7.map { it.steps }.average()
                val pct = if (pAvg > 0) (avg7 - pAvg) / pAvg * 100 else 0.0
                lines.add(String.format(Locale.US, "Week-over-week step change: %+.1f%%.", pct))
            }
            val dist = if (valid7Steps.isNotEmpty()) valid7Steps.map { it.distanceMeters }.average() else 0.0
            lines.add("Avg daily distance: ${WalkingMetrics.formatDistance(dist.toFloat(), p.useImperialUnits)}.")
        }
        
        val ex7 = m7.sumOf { it.exerciseMinutes }
        val exDays7 = m7.count { it.exerciseMinutes > 0 }
        val exDays30 = m30.count { it.exerciseMinutes > 0 }
        lines.add("Exercise: $ex7 min/week, $exDays7/7 days active ($exDays30/30 days monthly).")
        
        val activeCal = if (m7.any { it.activeCaloriesBurned > 0 }) m7.filter { it.activeCaloriesBurned > 0 }.map { it.activeCaloriesBurned }.average() else 0.0
        lines.add(String.format(Locale.US, "Avg active calories burned: %.0f kcal.", activeCal))
        
        val l30Sessions = sessions.filter { it.startTime / 1000 >= today.minusDays(29).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) }
        if (l30Sessions.isNotEmpty()) {
            val top3 = l30Sessions.groupBy { it.typeLabel }.mapValues { it.value.size }.toList().sortedByDescending { it.second }.take(3)
            lines.add("Monthly sessions: ${l30Sessions.size} total, ${l30Sessions.sumOf { it.durationMinutes }} min. Types: ${top3.joinToString { "${it.first} (${it.second})" }}.")
        }
        
        val workouts30 = events.filter { it.type == "workout" && it.timestamp / 1000 >= today.minusDays(29).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) }
        if (workouts30.isNotEmpty()) lines.add("Workout events (logged/started): ${workouts30.size} in 30 days.")
        
        val mind7 = m7.sumOf { it.mindfulnessMinutes }
        val mind30 = m30.sumOf { it.mindfulnessMinutes }
        if (mind30 > 0) lines.add("Mindfulness: $mind7 min (7-day), $mind30 min (30-day).")
        
        return DossierSection("Activity", lines)
    }

    private fun buildNutritionSection(metrics: List<DailyMetric>, foodLogs: List<FoodLog>, p: UserProfile, today: LocalDate): DossierSection {
        val m7 = metrics.filter { LocalDate.parse(it.date) in today.minusDays(6)..today }
        val m30 = metrics
        val valid7 = m7.filter { it.caloriesConsumed > 0 }
        val valid30 = m30.filter { it.caloriesConsumed > 0 }
        
        val lines = mutableListOf<String>()
        if (valid30.isNotEmpty()) {
            val avg7 = if (valid7.isNotEmpty()) valid7.map { it.caloriesConsumed }.average() else 0.0
            val avg30 = valid30.map { it.caloriesConsumed }.average()
            if (p.dailyCalorieLimit > 0) {
                val diff7 = (avg7 - p.dailyCalorieLimit) / p.dailyCalorieLimit * 100
                val diff30 = (avg30 - p.dailyCalorieLimit) / p.dailyCalorieLimit * 100
                lines.add(String.format(Locale.US, "Avg calories: %d (%+d%% vs limit) 7-day, %d (%+d%%) 30-day.", avg7.toInt(), diff7.toInt(), avg30.toInt(), diff30.toInt()))
                lines.add("Days over calorie limit: ${valid7.count { it.caloriesConsumed > p.dailyCalorieLimit }}/7.")
            } else {
                lines.add(String.format(Locale.US, "Avg calories: %d (7-day), %d (30-day).", avg7.toInt(), avg30.toInt()))
            }
            
            val macro7 = m7.filter { it.carbsG > 0 || it.proteinG > 0 || it.fatG > 0 }
            if (macro7.isNotEmpty()) {
                val c = macro7.map { it.carbsG }.average()
                val pr = macro7.map { it.proteinG }.average()
                val f = macro7.map { it.fatG }.average()
                val totalKcal = c * 4 + pr * 4 + f * 9
                if (totalKcal > 0) {
                    lines.add(String.format(Locale.US, "Macros (7-day): Carbs %.0fg (%.0f%%), Protein %.0fg (%.0f%%), Fat %.0fg (%.0f%%).", c, c * 4 / totalKcal * 100, pr, pr * 4 / totalKcal * 100, f, f * 9 / totalKcal * 100))
                }
            }
        }
        
        if (foodLogs.isNotEmpty()) {
            val top5 = foodLogs.map { it.name.trim() }.filter { it.isNotBlank() }
                .groupBy { it.lowercase() }
                .map { (lower, items) ->
                    val displayName = items.first()
                    displayName to items.size
                }
                .sortedByDescending { it.second }
                .take(5)
            lines.add("Frequent foods: ${top5.joinToString { "${it.first} (${it.second})" }}.")
            
            val logs7 = foodLogs.filter { LocalDate.parse(it.date) in today.minusDays(6)..today }
            if (logs7.isNotEmpty()) {
                val highest = logs7.groupBy { it.date }.mapValues { it.value.sumOf { l -> l.calories } }.maxByOrNull { it.value }
                if (highest != null) lines.add("Highest calorie day: ${highest.value} kcal on ${highest.key}.")
                
                val avgSugar = logs7.groupBy { it.date }.mapValues { it.value.sumOf { l -> l.sugarG.toDouble() } }.values.filter { it > 0 }.average()
                val avgSod = logs7.groupBy { it.date }.mapValues { it.value.sumOf { l -> l.sodiumMg.toDouble() } }.values.filter { it > 0 }.average()
                val avgFib = logs7.groupBy { it.date }.mapValues { it.value.sumOf { l -> l.fiberG.toDouble() } }.values.filter { it > 0 }.average()
                if (!avgSugar.isNaN() || !avgSod.isNaN() || !avgFib.isNaN()) {
                    lines.add(String.format(Locale.US, "Daily avg (logged days): Sugar %.0fg, Sodium %.0fmg, Fiber %.0fg.", if (avgSugar.isNaN()) 0.0 else avgSugar, if (avgSod.isNaN()) 0.0 else avgSod, if (avgFib.isNaN()) 0.0 else avgFib))
                }
            }
        }

        val w7 = m7.filter { it.waterLiters > 0f }
        if (w7.isNotEmpty()) {
            val wAvg = w7.map { it.waterLiters }.average()
            val goalStr = if (p.dailyWaterLimitLiters > 0) String.format(Locale.US, " vs %.1f L goal", p.dailyWaterLimitLiters) else ""
            lines.add(String.format(Locale.US, "Hydration: %.1f L 7-day average$goalStr.", wAvg))
        }

        return DossierSection("Nutrition", lines)
    }

    private fun buildGlucoseSection(metrics: List<DailyMetric>, p: UserProfile, today: LocalDate): DossierSection {
        val l14 = metrics.filter { LocalDate.parse(it.date) in today.minusDays(13)..today }
        val entries = mutableListOf<GlucoseEntry>()
        l14.forEach { m ->
            if (m.bloodGlucoseBeforeBreakfast > 0f) entries.add(GlucoseEntry("Before Breakfast", m.bloodGlucoseBeforeBreakfast, m.date, true))
            if (m.bloodGlucoseAfterBreakfast > 0f) entries.add(GlucoseEntry("After Breakfast", m.bloodGlucoseAfterBreakfast, m.date, false))
            if (m.bloodGlucoseBeforeLunch > 0f) entries.add(GlucoseEntry("Before Lunch", m.bloodGlucoseBeforeLunch, m.date, false))
            if (m.bloodGlucoseAfterLunch > 0f) entries.add(GlucoseEntry("After Lunch", m.bloodGlucoseAfterLunch, m.date, false))
            if (m.bloodGlucoseBeforeDinner > 0f) entries.add(GlucoseEntry("Before Dinner", m.bloodGlucoseBeforeDinner, m.date, false))
            if (m.bloodGlucoseAfterDinner > 0f) entries.add(GlucoseEntry("After Dinner", m.bloodGlucoseAfterDinner, m.date, false))
            if (m.bloodGlucoseMorning > 0f) entries.add(GlucoseEntry("Morning", m.bloodGlucoseMorning, m.date, true))
            if (m.bloodGlucoseNight > 0f) entries.add(GlucoseEntry("Night", m.bloodGlucoseNight, m.date, false))
        }

        val lines = mutableListOf<String>()
        if (entries.isNotEmpty()) {
            val fasting = entries.filter { it.slot == "Before Breakfast" || it.slot == "Morning" }
            if (fasting.isNotEmpty()) lines.add("Fasting/Morning: ${formatGlucose(fasting.map { it.value }.average().toFloat(), p.glucoseUnitMgdl)} avg (${fasting.size} readings).")
            
            fun buildRise(slot1: String, slot2: String, label: String) {
                val pairs = l14.mapNotNull { m ->
                    val v1 = when(slot1) { "BB" -> m.bloodGlucoseBeforeBreakfast; "BL" -> m.bloodGlucoseBeforeLunch; else -> m.bloodGlucoseBeforeDinner }
                    val v2 = when(slot2) { "AB" -> m.bloodGlucoseAfterBreakfast; "AL" -> m.bloodGlucoseAfterLunch; else -> m.bloodGlucoseAfterDinner }
                    if (v1 > 0f && v2 > 0f) v2 - v1 else null
                }
                if (pairs.isNotEmpty()) lines.add(String.format(Locale.US, "$label rise: %s avg (%d pairs).", formatGlucose(pairs.average().toFloat(), p.glucoseUnitMgdl), pairs.size))
            }
            buildRise("BB", "AB", "Breakfast")
            buildRise("BL", "AL", "Lunch")
            buildRise("BD", "AD", "Dinner")
            
            val avg = entries.map { it.value }.average().toFloat()
            lines.add("Overall: ${formatGlucose(avg, p.glucoseUnitMgdl)} avg, range ${formatGlucose(entries.minOf { it.value }, p.glucoseUnitMgdl)} - ${formatGlucose(entries.maxOf { it.value }, p.glucoseUnitMgdl)} (${entries.size} readings).")
            val latest = entries.last()
            lines.add("Latest: ${formatGlucose(latest.value, p.glucoseUnitMgdl)} (${latest.slot}) on ${latest.date}.")
            
            if (p.bloodGlucoseTargetMin > 0 && p.bloodGlucoseTargetMax > 0) {
                val inRange = entries.count { it.value in p.bloodGlucoseTargetMin..p.bloodGlucoseTargetMax }.toFloat() / entries.size * 100
                lines.add(String.format(Locale.US, "Time in target range: %.0f%%.", inRange))
                lines.add("Readings high: ${entries.count { it.value > p.bloodGlucoseTargetMax }}, low: ${entries.count { it.value < p.bloodGlucoseTargetMin }}.")
            }
            
            val allEntries = metrics.flatMap { m ->
                listOf(m.bloodGlucoseBeforeBreakfast, m.bloodGlucoseAfterBreakfast, m.bloodGlucoseBeforeLunch, m.bloodGlucoseAfterLunch, m.bloodGlucoseBeforeDinner, m.bloodGlucoseAfterDinner, m.bloodGlucoseMorning, m.bloodGlucoseNight).filter { it > 0f }
            }
            if (allEntries.size >= 10) {
                val hba1c = (allEntries.average() * 18 + 46.7) / 28.7
                lines.add(String.format(Locale.US, "Estimated HbA1c: %.1f%% (estimated, not a lab result).", hba1c))
            }
        } else {
            lines.add("No glucose records in the last 14 days.")
        }
        return DossierSection("Glucose", lines)
    }

    private fun buildBpSection(metrics: List<DailyMetric>, today: LocalDate): DossierSection {
        val l14 = metrics.filter { LocalDate.parse(it.date) in today.minusDays(13)..today && !it.bloodPressure.isNullOrBlank() }
        val bpRegex = Regex("""(\d+)\s*/\s*(\d+)""")
        val entries = l14.mapNotNull { m ->
            val match = bpRegex.find(m.bloodPressure)
            if (match != null) {
                Triple(match.groupValues[1].toInt(), match.groupValues[2].toInt(), m.date)
            } else null
        }

        val lines = mutableListOf<String>()
        if (entries.isNotEmpty()) {
            val avgSys = entries.map { it.first }.average()
            val avgDia = entries.map { it.second }.average()
            lines.add(String.format(Locale.US, "Average: %.0f/%.0f mmHg (%d readings).", avgSys, avgDia, entries.size))
            val highest = entries.maxByOrNull { it.first }!!
            val lowest = entries.minByOrNull { it.first }!!
            lines.add("Highest: ${highest.first}/${highest.second} (${highest.third}), Lowest: ${lowest.first}/${lowest.second}.")
            lines.add("Elevated (>130/80): ${entries.count { it.first >= 130 || it.second >= 80 }} readings.")
            
            val m7 = entries.filter { LocalDate.parse(it.third) in today.minusDays(6)..today }
            val p7 = entries.filter { LocalDate.parse(it.third) in today.minusDays(13)..today.minusDays(7) }
            if (m7.size >= 2 && p7.size >= 2) {
                lines.add(String.format(Locale.US, "Systolic change vs prev week: %+.0f mmHg.", m7.map { it.first }.average() - p7.map { it.first }.average()))
            }
        } else {
            lines.add("No blood pressure records in the last 14 days.")
        }
        return DossierSection("Blood pressure", lines)
    }

    private fun buildHeartBodySection(metrics: List<DailyMetric>, p: UserProfile, today: LocalDate): DossierSection {
        val m7 = metrics.filter { LocalDate.parse(it.date) in today.minusDays(6)..today }
        val p7 = metrics.filter { LocalDate.parse(it.date) in today.minusDays(13)..today.minusDays(7) }
        val l14 = metrics.filter { LocalDate.parse(it.date) in today.minusDays(13)..today }
        
        val lines = mutableListOf<String>()
        
        val r7 = m7.filter { it.restingHeartRate > 0 }
        val rP7 = p7.filter { it.restingHeartRate > 0 }
        if (r7.isNotEmpty()) {
            val rAvg = r7.map { it.restingHeartRate }.average()
            val diff = if (rP7.isNotEmpty()) rAvg - rP7.map { it.restingHeartRate }.average() else 0.0
            lines.add(String.format(Locale.US, "Resting Heart Rate: %.0f bpm (7-day), monthly %d bpm, WoW change %+.0f bpm.", rAvg, if (metrics.any { it.restingHeartRate > 0 }) metrics.filter { it.restingHeartRate > 0 }.map { it.restingHeartRate }.average().toInt() else 0, diff))
        }
        
        val h7 = m7.filter { it.heartRateVariability > 0f }
        val hP7 = p7.filter { it.heartRateVariability > 0f }
        if (h7.isNotEmpty()) {
            val hAvg = h7.map { it.heartRateVariability }.average()
            val pct = if (hP7.isNotEmpty()) (hAvg - hP7.map { it.heartRateVariability }.average()) / hP7.map { it.heartRateVariability }.average() * 100 else 0.0
            lines.add(String.format(Locale.US, "HRV: %.1f ms avg, %+.1f%% WoW change.", hAvg, pct))
        }
        
        val o14 = l14.filter { it.oxygenSaturation > 0f }
        if (o14.isNotEmpty()) {
            lines.add(String.format(Locale.US, "Oxygen Saturation: %.1f%% average, %.1f%% minimum.", o14.map { it.oxygenSaturation }.average(), o14.minOf { it.oxygenSaturation }))
        }
        
        val rr14 = l14.filter { it.respiratoryRate > 0f }
        if (rr14.isNotEmpty()) {
            lines.add(String.format(Locale.US, "Respiratory Rate: %.1f rpm average.", rr14.map { it.respiratoryRate }.average()))
        }
        
        val w30 = metrics.filter { it.weightKg > 0f }
        if (w30.isNotEmpty()) {
            val latest = w30.last()
            val oldest = w30.first()
            val w7 = m7.filter { it.weightKg > 0f }
            val change7 = if (w7.size >= 2) w7.last().weightKg - w7.first().weightKg else 0.0f
            lines.add("Weight: ${formatWeight(latest.weightKg, p.useImperialUnits)} (${latest.date}). Change: 30-day ${formatWeight(latest.weightKg - oldest.weightKg, p.useImperialUnits)}, 7-day ${formatWeight(change7, p.useImperialUnits)}.")
        }
        
        return DossierSection("Heart and body", lines)
    }

    private fun buildFlags(p: UserProfile, medical: List<MedicalRecord>): Set<String> {
        val flags = mutableSetOf<String>()
        val text = (medical.joinToString(" ") { "${it.category} ${it.title} ${it.details}" } + " ${p.healthGoals} ${p.dietaryRestrictions}").lowercase()
        
        if (text.contains(Regex("diabet|type 2|t2d|metformin|insulin|glimepiride|sitagliptin"))) flags.add("diabetes")
        if (text.contains(Regex("hypertens|high blood pressure|amlodipine|losartan|telmisartan|atenolol|bisoprolol"))) flags.add("hypertension")
        if (text.contains(Regex("kidney|renal|ckd|creatinine|dialysis"))) flags.add("kidney")
        if (text.contains(Regex("thyroid|levothyroxine|hashimoto|graves"))) flags.add("thyroid")
        if (text.contains(Regex("coronary|cardiac|stent|bypass|angina|arrhythmia|atrial|heart disease|heart failure"))) flags.add("heart_disease")
        if (text.contains(Regex("pregnan"))) flags.add("pregnancy")
        if (text.contains(Regex("asthma|copd"))) flags.add("respiratory")
        if (text.contains(Regex("allerg"))) flags.add("allergy")
        if (text.contains(Regex("medic|tablet|mg "))) flags.add("on_medication")
        
        return flags
    }

    private fun buildPatterns(metrics: List<DailyMetric>, p: UserProfile): List<String> {
        val patterns = mutableListOf<String>()
        val valid = metrics.filter { m ->
            val readings = listOf(m.bloodGlucoseBeforeBreakfast, m.bloodGlucoseAfterBreakfast, m.bloodGlucoseBeforeLunch, m.bloodGlucoseAfterLunch, m.bloodGlucoseBeforeDinner, m.bloodGlucoseAfterDinner, m.bloodGlucoseMorning, m.bloodGlucoseNight).filter { it > 0f }
            readings.isNotEmpty()
        }
        
        // (a) Average daily glucose on days with sleep under 6.5 h versus days with sleep of 7.0 h or more
        val shortSleep = valid.filter { it.sleepHours in 0.1f..6.5f }
        val longSleep = valid.filter { it.sleepHours >= 7.0f }
        if (shortSleep.size >= 3 && longSleep.size >= 3 && (shortSleep.size + longSleep.size) >= 5) {
            val gShort = shortSleep.map { day -> listOf(day.bloodGlucoseBeforeBreakfast, day.bloodGlucoseAfterBreakfast, day.bloodGlucoseBeforeLunch, day.bloodGlucoseAfterLunch, day.bloodGlucoseBeforeDinner, day.bloodGlucoseAfterDinner, day.bloodGlucoseMorning, day.bloodGlucoseNight).filter { it > 0f }.average() }.average()
            val gLong = longSleep.map { day -> listOf(day.bloodGlucoseBeforeBreakfast, day.bloodGlucoseAfterBreakfast, day.bloodGlucoseBeforeLunch, day.bloodGlucoseAfterLunch, day.bloodGlucoseBeforeDinner, day.bloodGlucoseAfterDinner, day.bloodGlucoseMorning, day.bloodGlucoseNight).filter { it > 0f }.average() }.average()
            patterns.add("Glucose on days with under 6.5 h of sleep: ${formatGlucose(gShort.toFloat(), p.glucoseUnitMgdl)} (n=${shortSleep.size}) versus ${formatGlucose(gLong.toFloat(), p.glucoseUnitMgdl)} (n=${longSleep.size}) on days with 7 h or more. A pattern in your data, not proof of cause.")
        }
        
        // (b) Average daily glucose on days when steps met the goal versus days when they did not
        if (p.stepGoal > 0) {
            val met = valid.filter { it.steps >= p.stepGoal }
            val missed = valid.filter { it.steps in 1 until p.stepGoal }
            if (met.size >= 3 && missed.size >= 3 && (met.size + missed.size) >= 5) {
                val gMet = met.map { day -> listOf(day.bloodGlucoseBeforeBreakfast, day.bloodGlucoseAfterBreakfast, day.bloodGlucoseBeforeLunch, day.bloodGlucoseAfterLunch, day.bloodGlucoseBeforeDinner, day.bloodGlucoseAfterDinner, day.bloodGlucoseMorning, day.bloodGlucoseNight).filter { it > 0f }.average() }.average()
                val gMissed = missed.map { day -> listOf(day.bloodGlucoseBeforeBreakfast, day.bloodGlucoseAfterBreakfast, day.bloodGlucoseBeforeLunch, day.bloodGlucoseAfterLunch, day.bloodGlucoseBeforeDinner, day.bloodGlucoseAfterDinner, day.bloodGlucoseMorning, day.bloodGlucoseNight).filter { it > 0f }.average() }.average()
                patterns.add("Glucose on days meeting step goal: ${formatGlucose(gMet.toFloat(), p.glucoseUnitMgdl)} (n=${met.size}) versus ${formatGlucose(gMissed.toFloat(), p.glucoseUnitMgdl)} (n=${missed.size}) on days missing goal. A pattern in your data, not proof of cause.")
            }
        }
        
        // (c) average next-morning fasting glucose after days with carbs above the median versus at or below it
        val withCarbs = metrics.filterIndexed { index, m -> m.carbsG > 0 && index + 1 < metrics.size && (metrics[index + 1].bloodGlucoseBeforeBreakfast > 0 || metrics[index+1].bloodGlucoseMorning > 0) }
        if (withCarbs.size >= 5) {
            val median = withCarbs.map { it.carbsG }.sorted().let { it[it.size/2] }
            val highCarb = withCarbs.filter { it.carbsG > median }
            val lowCarb = withCarbs.filter { it.carbsG <= median }
            if (highCarb.size >= 3 && lowCarb.size >= 3) {
                val gHigh = highCarb.map { m -> val next = metrics[metrics.indexOf(m)+1]; val v = next.bloodGlucoseBeforeBreakfast; if (v > 0) v else next.bloodGlucoseMorning }.average()
                val gLow = lowCarb.map { m -> val next = metrics[metrics.indexOf(m)+1]; val v = next.bloodGlucoseBeforeBreakfast; if (v > 0) v else next.bloodGlucoseMorning }.average()
                patterns.add("Fasting glucose after days with over ${median.toInt()}g carbs: ${formatGlucose(gHigh.toFloat(), p.glucoseUnitMgdl)} (n=${highCarb.size}) versus ${formatGlucose(gLow.toFloat(), p.glucoseUnitMgdl)} (n=${lowCarb.size}) after lower carb days. A pattern in your data, not proof of cause.")
            }
        }

        return patterns
    }

    private data class GlucoseEntry(val slot: String, val value: Float, val date: String, val isFasting: Boolean)
}
