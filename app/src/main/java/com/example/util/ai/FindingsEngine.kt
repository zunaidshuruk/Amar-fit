package com.example.util.ai

import com.example.data.local.DailyMetric
import com.example.util.formatGlucose
import java.time.LocalDate
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs

object FindingsEngine {
    fun compute(input: DossierInput): List<Finding> {
        val today = try { LocalDate.parse(input.todayDate) } catch (e: Exception) { LocalDate.now() }
        val r7Range = today.minusDays(6)..today
        val l14Range = today.minusDays(13)..today
        val p7Range = today.minusDays(13)..today.minusDays(7)

        val r7 = input.metrics.filter { try { LocalDate.parse(it.date) in r7Range } catch (e: Exception) { false } }
        val p7 = input.metrics.filter { try { LocalDate.parse(it.date) in p7Range } catch (e: Exception) { false } }
        val l14 = input.metrics.filter { try { LocalDate.parse(it.date) in l14Range } catch (e: Exception) { false } }

        val findings = mutableListOf<Finding>()

        // SLEEP
        val s7 = r7.filter { it.sleepHours > 0f }
        if (s7.isNotEmpty()) {
            val avg7 = s7.map { it.sleepHours }.average().toFloat()
            if (input.profile.sleepGoalHours > 0) {
                if (avg7 <= input.profile.sleepGoalHours - 1.0f) {
                    findings.add(Finding(FindingTopic.SLEEP, 2, "sleep_low_avg", "Sleep averaged %.1f h against your %.1f h goal".format(Locale.US, avg7, input.profile.sleepGoalHours), "Based on ${s7.size} nights in the last week."))
                } else if (avg7 >= input.profile.sleepGoalHours) {
                    findings.add(Finding(FindingTopic.SLEEP, 1, "sleep_goal_met", "You met your sleep goal this week", "Average %.1f h is at or above your %.1f h target.".format(Locale.US, avg7, input.profile.sleepGoalHours)))
                }
            }
            if (s7.count { it.sleepHours < 6.0f } >= 3) {
                findings.add(Finding(FindingTopic.SLEEP, 2, "sleep_under_6", "Short sleep detected on 3+ nights", "You had ${s7.count { it.sleepHours < 6.0f }} nights under 6.0 hours this week."))
            }
            val p7S = p7.filter { it.sleepHours > 0f }
            if (s7.size >= 3 && p7S.size >= 3) {
                val pAvg = p7S.map { it.sleepHours }.average().toFloat()
                if (avg7 <= pAvg - 0.7f) {
                    findings.add(Finding(FindingTopic.SLEEP, 1, "sleep_wow_drop", "Sleep duration decreased this week", "A drop of %.1f h on average compared to the previous week.".format(Locale.US, pAvg - avg7)))
                }
            }
        }

        // ACTIVITY
        val st7 = r7.filter { it.steps > 0 }
        if (st7.isNotEmpty()) {
            val avg7 = st7.map { it.steps }.average().toFloat()
            if (input.profile.stepGoal > 0) {
                if (avg7 < input.profile.stepGoal * 0.6f) {
                    findings.add(Finding(FindingTopic.ACTIVITY, 2, "steps_low_avg", "Step average is below 60% of goal", "Averaged ${avg7.toInt()} steps this week against ${input.profile.stepGoal} goal."))
                }
                val metCount = st7.count { it.steps >= input.profile.stepGoal }
                if (metCount <= 1) {
                    findings.add(Finding(FindingTopic.ACTIVITY, 2, "steps_goal_missed", "Step goal rarely met", "Target reached on only $metCount/7 days."))
                } else if (metCount >= 5) {
                    findings.add(Finding(FindingTopic.ACTIVITY, 1, "steps_goal_met", "Excellent step consistency", "Met your goal on $metCount of the last 7 days."))
                }
            }
            val p7St = p7.filter { it.steps > 0 }
            if (st7.size >= 3 && p7St.size >= 3) {
                val pAvg = p7St.map { it.steps }.average().toFloat()
                if (avg7 <= pAvg * 0.8f) {
                    findings.add(Finding(FindingTopic.ACTIVITY, 1, "steps_wow_drop", "Activity level dropped by 20%+", "Average steps fell from ${pAvg.toInt()} to ${avg7.toInt()} this week."))
                }
            }
        }
        val ex7 = r7.sumOf { it.exerciseMinutes }
        val sessions7 = input.exerciseSessions.filter { try { LocalDate.parse(java.time.Instant.ofEpochMilli(it.startTime).atZone(java.time.ZoneId.of("UTC")).toLocalDate().toString()) in r7Range } catch (e: Exception) { false } }
        if (r7.isNotEmpty()) {
            if (ex7 == 0 && sessions7.isEmpty()) {
                findings.add(Finding(FindingTopic.ACTIVITY, 2, "exercise_none", "No exercise recorded this week", "Consistent activity helps regulate glucose and heart health."))
            } else if (ex7 < 150) {
                findings.add(Finding(FindingTopic.ACTIVITY, 1, "exercise_low", "Exercise is under 150 minutes", "You logged $ex7 minutes of activity; the standard recommendation is 150+ per week."))
            }
        }

        // NUTRITION
        val cal7 = r7.filter { it.caloriesConsumed > 0 }
        if (cal7.isNotEmpty() && input.profile.dailyCalorieLimit > 0) {
            val avg7 = cal7.map { it.caloriesConsumed }.average().toFloat()
            val ratio = avg7 / input.profile.dailyCalorieLimit
            if (ratio >= 1.3f) {
                findings.add(Finding(FindingTopic.NUTRITION, 3, "kcal_high_30", "Calories are 30% over limit", "Averaged ${avg7.toInt()} kcal vs ${input.profile.dailyCalorieLimit} limit."))
            } else if (ratio >= 1.15f) {
                findings.add(Finding(FindingTopic.NUTRITION, 2, "kcal_high_15", "Calories are 15% over limit", "Averaged ${avg7.toInt()} kcal over the last 7 days."))
            }
        }
        val m7Macro = r7.filter { it.carbsG > 0 || it.proteinG > 0 || it.fatG > 0 }
        if (m7Macro.isNotEmpty()) {
            val c = m7Macro.map { it.carbsG }.average()
            val p = m7Macro.map { it.proteinG }.average()
            val f = m7Macro.map { it.fatG }.average()
            val total = c * 4 + p * 4 + f * 9
            if (total > 0 && (c * 4 / total) > 0.5) {
                val hasGlucose = l14.any { listOf(it.bloodGlucoseBeforeBreakfast, it.bloodGlucoseAfterBreakfast, it.bloodGlucoseBeforeLunch, it.bloodGlucoseAfterLunch, it.bloodGlucoseBeforeDinner, it.bloodGlucoseAfterDinner, it.bloodGlucoseMorning, it.bloodGlucoseNight).any { v -> v > 0f } }
                if (hasGlucose) {
                    findings.add(Finding(FindingTopic.NUTRITION, 2, "carbs_high", "Carbs exceed 50% of calorie intake", "Average carbs %.0fg (${(c * 4 / total * 100).toInt()}%% of calories) while monitoring glucose.".format(Locale.US, c)))
                }
            }
            if (input.profile.weightKg > 0 && p < input.profile.weightKg * 0.8) {
                findings.add(Finding(FindingTopic.NUTRITION, 1, "protein_low", "Protein intake is low", "Averaged %.0fg; recommended is at least 0.8g per kg of body weight (%.0fg).".format(Locale.US, p, input.profile.weightKg * 0.8)))
            }
        }
        val logs7 = input.foodLogs.filter { try { LocalDate.parse(it.date) in r7Range } catch (e: Exception) { false } }
        val daysWithSugar = logs7.groupBy { it.date }.mapValues { it.value.sumOf { l -> l.sugarG.toDouble() } }.filter { it.value > 0 }
        if (daysWithSugar.size >= 3 && daysWithSugar.values.average() > 50.0) {
            findings.add(Finding(FindingTopic.NUTRITION, 2, "sugar_high", "Average sugar is above 50g", "High sugar intake on ${daysWithSugar.size} logged days can cause glucose spikes."))
        }

        // HYDRATION
        val w7 = r7.filter { it.waterLiters > 0f }
        if (w7.isNotEmpty() && input.profile.dailyWaterLimitLiters > 0) {
            val avg7 = w7.map { it.waterLiters }.average().toFloat()
            if (avg7 < input.profile.dailyWaterLimitLiters * 0.6f) {
                findings.add(Finding(FindingTopic.HYDRATION, 2, "water_low", "Hydration is significantly below goal", "Averaged %.1f L vs %.1f L goal.".format(Locale.US, avg7, input.profile.dailyWaterLimitLiters)))
            }
        }

        // GLUCOSE
        val g14 = mutableListOf<Float>()
        l14.forEach { m ->
            listOf(m.bloodGlucoseBeforeBreakfast, m.bloodGlucoseAfterBreakfast, m.bloodGlucoseBeforeLunch, m.bloodGlucoseAfterLunch, m.bloodGlucoseBeforeDinner, m.bloodGlucoseAfterDinner, m.bloodGlucoseMorning, m.bloodGlucoseNight).forEach { v ->
                if (v > 0f) g14.add(v)
            }
        }
        if (g14.isNotEmpty()) {
            if (input.profile.bloodGlucoseTargetMin > 0 && input.profile.bloodGlucoseTargetMax > 0) {
                val inRange = g14.count { it in input.profile.bloodGlucoseTargetMin..input.profile.bloodGlucoseTargetMax }.toFloat() / g14.size
                if (inRange < 0.5f) {
                    findings.add(Finding(FindingTopic.GLUCOSE, 3, "glucose_range_low", "Glucose in target range under 50%", "Only ${(inRange * 100).toInt()}% of readings were within your target limits."))
                } else if (inRange < 0.7f) {
                    findings.add(Finding(FindingTopic.GLUCOSE, 2, "glucose_range_mid", "Glucose in target range under 70%", "Goal is usually 70% or more; you are currently at ${(inRange * 100).toInt()}%."))
                }
                val highCount = g14.count { it > input.profile.bloodGlucoseTargetMax }
                if (highCount >= 3) {
                    findings.add(Finding(FindingTopic.GLUCOSE, 2, "glucose_high_count", "$highCount readings above target", "Multiple readings exceeded your maximum limit of ${formatGlucose(input.profile.bloodGlucoseTargetMax, input.profile.glucoseUnitMgdl)}."))
                }
                val fasting = l14.flatMap { listOf(it.bloodGlucoseBeforeBreakfast, it.bloodGlucoseMorning) }.filter { it > 0f }
                if (fasting.isNotEmpty() && fasting.average() > input.profile.bloodGlucoseTargetMax) {
                    findings.add(Finding(FindingTopic.GLUCOSE, 2, "glucose_fasting_high", "Fasting glucose average is high", "Morning readings average ${formatGlucose(fasting.average().toFloat(), input.profile.glucoseUnitMgdl)}, which is above your target."))
                }
            }
            if (g14.any { it < 3.9f }) {
                val low = l14.find { listOf(it.bloodGlucoseBeforeBreakfast, it.bloodGlucoseAfterBreakfast, it.bloodGlucoseBeforeLunch, it.bloodGlucoseAfterLunch, it.bloodGlucoseBeforeDinner, it.bloodGlucoseAfterDinner, it.bloodGlucoseMorning, it.bloodGlucoseNight).any { v -> v > 0f && v < 3.9f } }
                findings.add(Finding(FindingTopic.GLUCOSE, 3, "glucose_hypo", "Low glucose reading detected", "A reading of ${formatGlucose(g14.find { it < 3.9f }!!, input.profile.glucoseUnitMgdl)} was recorded on ${low?.date}. Discuss low readings with your doctor."))
            }
            
            val rises = mutableListOf<Pair<String, Double>>()
            fun checkRise(slot1: (DailyMetric) -> Float, slot2: (DailyMetric) -> Float, label: String, tag: String) {
                val pairs = l14.mapNotNull { if (slot1(it) > 0f && slot2(it) > 0f) (slot2(it) - slot1(it)).toDouble() else null }
                if (pairs.size >= 3) {
                    val avgRise = pairs.average()
                    if (avgRise >= 2.0) {
                        rises.add(label to avgRise)
                    }
                }
            }
            checkRise({ it.bloodGlucoseBeforeBreakfast }, { it.bloodGlucoseAfterBreakfast }, "Breakfast", "glucose_rise_breakfast")
            checkRise({ it.bloodGlucoseBeforeLunch }, { it.bloodGlucoseAfterLunch }, "Lunch", "glucose_rise_lunch")
            checkRise({ it.bloodGlucoseBeforeDinner }, { it.bloodGlucoseAfterDinner }, "Dinner", "glucose_rise_dinner")
            rises.sortByDescending { it.second }
            rises.firstOrNull()?.let {
                val sev = if (it.second >= 3.0) 3 else 2
                findings.add(Finding(FindingTopic.GLUCOSE, sev, "glucose_rise_${it.first.lowercase()}", "Significant ${it.first} glucose rise", "Glucose jumps ${formatGlucose(it.second.toFloat(), input.profile.glucoseUnitMgdl)} on average after ${it.first.lowercase()}."))
            }

            val g7 = r7.flatMap { listOf(it.bloodGlucoseBeforeBreakfast, it.bloodGlucoseAfterBreakfast, it.bloodGlucoseBeforeLunch, it.bloodGlucoseAfterLunch, it.bloodGlucoseBeforeDinner, it.bloodGlucoseAfterDinner, it.bloodGlucoseMorning, it.bloodGlucoseNight).filter { v -> v > 0f } }
            val gP7 = p7.flatMap { listOf(it.bloodGlucoseBeforeBreakfast, it.bloodGlucoseAfterBreakfast, it.bloodGlucoseBeforeLunch, it.bloodGlucoseAfterLunch, it.bloodGlucoseBeforeDinner, it.bloodGlucoseAfterDinner, it.bloodGlucoseMorning, it.bloodGlucoseNight).filter { v -> v > 0f } }
            if (g7.isNotEmpty() && gP7.isNotEmpty()) {
                val diff = g7.average() - gP7.average()
                if (diff >= 0.8) {
                    findings.add(Finding(FindingTopic.GLUCOSE, 2, "glucose_wow_rise", "Average glucose rising week-over-week", "Rose by ${formatGlucose(diff.toFloat(), input.profile.glucoseUnitMgdl)} compared to the previous 7 days."))
                }
            }
        }

        // BLOOD PRESSURE
        val bpRegex = Regex("""(\d+)\s*/\s*(\d+)""")
        val bp14 = l14.mapNotNull { m ->
            val match = bpRegex.find(m.bloodPressure)
            if (match != null) Triple(match.groupValues[1].toInt(), match.groupValues[2].toInt(), m.date) else null
        }
        if (bp14.size >= 3) {
            val avgSys = bp14.map { it.first }.average()
            val avgDia = bp14.map { it.second }.average()
            if (avgSys >= 140 || avgDia >= 90) {
                findings.add(Finding(FindingTopic.BLOOD_PRESSURE, 3, "bp_avg_high_2", "Blood pressure average is high (140/90+)", "Average is %.0f/%.0f mmHg over ${bp14.size} readings.".format(Locale.US, avgSys, avgDia)))
            } else if (avgSys >= 130 || avgDia >= 80) {
                findings.add(Finding(FindingTopic.BLOOD_PRESSURE, 2, "bp_avg_high_1", "Blood pressure average is elevated", "Averaged %.0f/%.0f mmHg; target is usually below 130/80.".format(Locale.US, avgSys, avgDia)))
            }
            if (bp14.any { it.first >= 180 || it.second >= 120 }) {
                val crisis = bp14.find { it.first >= 180 || it.second >= 120 }!!
                findings.add(Finding(FindingTopic.BLOOD_PRESSURE, 3, "bp_crisis", "Hypertensive crisis reading detected", "Recorded ${crisis.first}/${crisis.second} on ${crisis.third}. Seek medical advice promptly."))
            }
            val bp7 = bp14.filter { try { LocalDate.parse(it.third) in r7Range } catch (e: Exception) { false } }
            val bpP7 = bp14.filter { try { LocalDate.parse(it.third) in p7Range } catch (e: Exception) { false } }
            if (bp7.size >= 2 && bpP7.size >= 2) {
                val diff = bp7.map { it.first }.average() - bpP7.map { it.first }.average()
                if (diff >= 5) {
                    findings.add(Finding(FindingTopic.BLOOD_PRESSURE, 1, "bp_wow_rise", "Blood pressure trend is rising", "Average systolic rose by ${diff.toInt()} mmHg compared to last week."))
                }
            }
        }

        // HEART
        val rhr7 = r7.filter { it.restingHeartRate > 0 }
        val rhrP7 = p7.filter { it.restingHeartRate > 0 }
        if (rhr7.isNotEmpty() && rhrP7.isNotEmpty()) {
            val diff = rhr7.map { it.restingHeartRate }.average() - rhrP7.map { it.restingHeartRate }.average()
            if (diff >= 5.0) {
                findings.add(Finding(FindingTopic.HEART, 2, "rhr_wow_rise", "Resting heart rate increased", "Average rose by ${diff.toInt()} bpm; may indicate stress, fatigue, or illness."))
            }
        }
        val hrv7 = r7.filter { it.heartRateVariability > 0f }
        val hrvP7 = p7.filter { it.heartRateVariability > 0f }
        if (hrv7.isNotEmpty() && hrvP7.isNotEmpty()) {
            val avg7 = hrv7.map { it.heartRateVariability }.average()
            val pAvg = hrvP7.map { it.heartRateVariability }.average()
            if (avg7 <= pAvg * 0.85) {
                findings.add(Finding(FindingTopic.HEART, 2, "hrv_wow_drop", "HRV is down 15% or more", "A drop from %.1f to %.1f ms; often a sign of poor recovery or stress.".format(Locale.US, pAvg, avg7)))
            }
        }
        val spo2Min = l14.filter { it.oxygenSaturation > 0f }.map { it.oxygenSaturation }.minOrNull()
        if (spo2Min != null && spo2Min < 92f) {
            findings.add(Finding(FindingTopic.HEART, 3, "spo2_low", "Low oxygen saturation detected", "Minimum SpO2 was %.1f%%; check with a doctor if you feel breathless or unwell.".format(Locale.US, spo2Min)))
        }

        // WEIGHT
        val w30 = input.metrics.filter { try { LocalDate.parse(it.date) in today.minusDays(29)..today && it.weightKg > 0f } catch (e: Exception) { false } }.sortedBy { it.date }
        if (w30.size >= 2) {
            val diff = w30.last().weightKg - w30.first().weightKg
            if (abs(diff) >= 2.0) {
                val dir = if (diff > 0) "gain" else "loss"
                findings.add(Finding(FindingTopic.WEIGHT, 1, "weight_change", "Weight $dir of %.1f kg in 30 days".format(Locale.US, abs(diff)), "Trend based on records from ${w30.first().date} to ${w30.last().date}."))
            }
        }

        // DATA GAPS
        fun checkGap(list: List<DailyMetric>, filter: (DailyMetric) -> Boolean, label: String, tag: String) {
            if (list.count(filter) < 3) {
                findings.add(Finding(FindingTopic.DATA, 1, tag, "No $label records in the last 14 days", "Add readings for better advice and more accurate patterns."))
            }
        }
        checkGap(l14, { listOf(it.bloodGlucoseBeforeBreakfast, it.bloodGlucoseAfterBreakfast, it.bloodGlucoseBeforeLunch, it.bloodGlucoseAfterLunch, it.bloodGlucoseBeforeDinner, it.bloodGlucoseAfterDinner, it.bloodGlucoseMorning, it.bloodGlucoseNight).any { v -> v > 0f } }, "glucose", "data_gap_glucose")
        checkGap(l14, { !it.bloodPressure.isNullOrBlank() }, "blood pressure", "data_gap_bp")
        checkGap(l14, { it.sleepHours > 0f }, "sleep", "data_gap_sleep")
        checkGap(l14, { it.steps > 0 }, "steps", "data_gap_steps")
        if (input.foodLogs.filter { try { LocalDate.parse(it.date) in l14Range } catch (e: Exception) { false } }.groupBy { it.date }.size < 3) {
            findings.add(Finding(FindingTopic.DATA, 1, "data_gap_food", "No food records in the last 14 days", "Logging your meals helps the AI correlate nutrition with your health vitals."))
        }

        // PATTERNS (derived from HealthDossierBuilder buildPatterns if we had access, but we'll re-calculate or just skip if too complex)
        // Re-calculating patterns here for Findings
        val validP = input.metrics.filter { m ->
            val readings = listOf(m.bloodGlucoseBeforeBreakfast, m.bloodGlucoseAfterBreakfast, m.bloodGlucoseBeforeLunch, m.bloodGlucoseAfterLunch, m.bloodGlucoseBeforeDinner, m.bloodGlucoseAfterDinner, m.bloodGlucoseMorning, m.bloodGlucoseNight).filter { it > 0f }
            readings.isNotEmpty()
        }
        val sShort = validP.filter { it.sleepHours in 0.1f..6.5f }
        val sLong = validP.filter { it.sleepHours >= 7.0f }
        if (sShort.size >= 3 && sLong.size >= 3 && (sShort.size + sLong.size) >= 5) {
            val gShort = sShort.map { d -> listOf(d.bloodGlucoseBeforeBreakfast, d.bloodGlucoseAfterBreakfast, d.bloodGlucoseBeforeLunch, d.bloodGlucoseAfterLunch, d.bloodGlucoseBeforeDinner, d.bloodGlucoseAfterDinner, d.bloodGlucoseMorning, d.bloodGlucoseNight).filter { it > 0f }.average() }.average()
            val gLong = sLong.map { d -> listOf(d.bloodGlucoseBeforeBreakfast, d.bloodGlucoseAfterBreakfast, d.bloodGlucoseBeforeLunch, d.bloodGlucoseAfterLunch, d.bloodGlucoseBeforeDinner, d.bloodGlucoseAfterDinner, d.bloodGlucoseMorning, d.bloodGlucoseNight).filter { it > 0f }.average() }.average()
            val diff = abs(gShort - gLong)
            val sev = if (diff >= 1.0) 2 else 1
            findings.add(Finding(FindingTopic.DATA, sev, "pattern", "Pattern: Sleep duration correlates with glucose", "Glucose was higher after shorter sleep nights (%.1f vs %.1f).".format(Locale.US, gShort, gLong)))
        }

        return findings.sortedWith(compareByDescending<Finding> { it.severity }.thenBy { it.topic.ordinal }).take(14)
    }
}
