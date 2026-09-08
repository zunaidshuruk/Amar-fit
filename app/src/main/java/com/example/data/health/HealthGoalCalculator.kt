package com.example.data.health

import com.example.data.local.UserProfile
import kotlin.math.roundToInt

object HealthGoalCalculator {

    fun calculateStepGoal(profile: UserProfile): Int {
        return when (profile.activityLevel.trim().lowercase()) {
            "sedentary" -> 8000
            "light" -> 9000
            "moderate" -> 10000
            "active" -> 12000
            "very active" -> 14000
            else -> 10000
        }
    }

    fun calculateSleepGoalHours(profile: UserProfile): Float {
        return when {
            profile.age <= 0 -> 8f
            profile.age in 1..17 -> 9f
            profile.age in 18..64 -> 8f
            else -> 7.5f // age >= 65
        }
    }

    fun calculateWaterGoalLiters(profile: UserProfile): Float {
        val baseWater = if (profile.weightKg <= 0f) {
            2.5f
        } else {
            (profile.weightKg * 33f) / 1000f
        }

        val activityBonus = when (profile.activityLevel.trim().lowercase()) {
            "active" -> 0.3f
            "very active" -> 0.5f
            else -> 0.0f
        }

        val total = baseWater + activityBonus
        return (total * 10f).roundToInt() / 10f
    }

    fun calculateCalorieGoal(profile: UserProfile): Int {
        if (profile.weightKg <= 0f || profile.heightCm <= 0f || profile.age <= 0) {
            return 2000
        }

        val bmrMale = 10f * profile.weightKg + 6.25f * profile.heightCm - 5f * profile.age + 5f
        val bmrFemale = 10f * profile.weightKg + 6.25f * profile.heightCm - 5f * profile.age - 161f

        val bmr = when (profile.gender.trim().lowercase()) {
            "male" -> bmrMale
            "female" -> bmrFemale
            else -> (bmrMale + bmrFemale) / 2f
        }

        val activityMultiplier = when (profile.activityLevel.trim().lowercase()) {
            "sedentary" -> 1.2f
            "light" -> 1.375f
            "moderate" -> 1.55f
            "active" -> 1.725f
            "very active" -> 1.9f
            else -> 1.55f
        }

        val tdee = bmr * activityMultiplier
        // Round to nearest 50 kcal
        return ((tdee / 50f).roundToInt() * 50)
    }

    enum class HeartRateZone { PEAK, VIGOROUS, MODERATE, LIGHT, RESTING }

    fun maxHeartRate(age: Int): Int = if (age > 0) 220 - age else 190

    fun heartRateZoneFor(bpm: Int, age: Int): HeartRateZone {
        val maxHr = maxHeartRate(age)
        val pct = bpm.toFloat() / maxHr.toFloat()
        return when {
            pct >= 0.85f -> HeartRateZone.PEAK
            pct >= 0.70f -> HeartRateZone.VIGOROUS
            pct >= 0.50f -> HeartRateZone.MODERATE
            bpm > 0 -> HeartRateZone.LIGHT
            else -> HeartRateZone.RESTING
        }
    }
}
