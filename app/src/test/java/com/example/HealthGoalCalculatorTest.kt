package com.example

import com.example.data.health.HealthGoalCalculator
import com.example.data.local.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthGoalCalculatorTest {

    @Test
    fun testStepGoalCalculations() {
        val sedentaryProfile = UserProfile(activityLevel = "Sedentary")
        assertEquals(8000, HealthGoalCalculator.calculateStepGoal(sedentaryProfile))

        val lightProfile = UserProfile(activityLevel = "Light")
        assertEquals(9000, HealthGoalCalculator.calculateStepGoal(lightProfile))

        val moderateProfile = UserProfile(activityLevel = "Moderate")
        assertEquals(10000, HealthGoalCalculator.calculateStepGoal(moderateProfile))

        val activeProfile = UserProfile(activityLevel = "Active")
        assertEquals(12000, HealthGoalCalculator.calculateStepGoal(activeProfile))

        val veryActiveProfile = UserProfile(activityLevel = "Very Active")
        assertEquals(14000, HealthGoalCalculator.calculateStepGoal(veryActiveProfile))

        val unknownProfile = UserProfile(activityLevel = "")
        assertEquals(10000, HealthGoalCalculator.calculateStepGoal(unknownProfile))
    }

    @Test
    fun testSleepGoalCalculations() {
        assertEquals(8f, HealthGoalCalculator.calculateSleepGoalHours(UserProfile(age = 0)), 0.01f)
        assertEquals(9f, HealthGoalCalculator.calculateSleepGoalHours(UserProfile(age = 15)), 0.01f)
        assertEquals(8f, HealthGoalCalculator.calculateSleepGoalHours(UserProfile(age = 25)), 0.01f)
        assertEquals(7.5f, HealthGoalCalculator.calculateSleepGoalHours(UserProfile(age = 70)), 0.01f)
    }

    @Test
    fun testWaterGoalCalculations() {
        // Flat default when weightKg <= 0
        assertEquals(2.5f, HealthGoalCalculator.calculateWaterGoalLiters(UserProfile(weightKg = 0f)), 0.01f)

        // 70kg Moderate -> (70 * 33) / 1000 = 2.31 -> rounded 2.3
        assertEquals(2.3f, HealthGoalCalculator.calculateWaterGoalLiters(UserProfile(weightKg = 70f, activityLevel = "Moderate")), 0.01f)

        // 70kg Active -> 2.31 + 0.3 = 2.61 -> rounded 2.6
        assertEquals(2.6f, HealthGoalCalculator.calculateWaterGoalLiters(UserProfile(weightKg = 70f, activityLevel = "Active")), 0.01f)

        // 70kg Very Active -> 2.31 + 0.5 = 2.81 -> rounded 2.8
        assertEquals(2.8f, HealthGoalCalculator.calculateWaterGoalLiters(UserProfile(weightKg = 70f, activityLevel = "Very Active")), 0.01f)
    }

    @Test
    fun testCalorieGoalCalculations() {
        // Fallback default
        assertEquals(2000, HealthGoalCalculator.calculateCalorieGoal(UserProfile(weightKg = 0f, heightCm = 0f, age = 0)))

        // Male: 70kg, 175cm, 30y, Moderate (1.55)
        // BMR = 10*70 + 6.25*175 - 5*30 + 5 = 700 + 1093.75 - 150 + 5 = 1648.75
        // TDEE = 1648.75 * 1.55 = 2555.5625 -> rounded to nearest 50 = 2550
        val maleProfile = UserProfile(gender = "Male", weightKg = 70f, heightCm = 175f, age = 30, activityLevel = "Moderate")
        assertEquals(2550, HealthGoalCalculator.calculateCalorieGoal(maleProfile))

        // Female: 60kg, 165cm, 30y, Light (1.375)
        // BMR = 10*60 + 6.25*165 - 5*30 - 161 = 600 + 1031.25 - 150 - 161 = 1320.25
        // TDEE = 1320.25 * 1.375 = 1815.34 -> rounded to nearest 50 = 1800
        val femaleProfile = UserProfile(gender = "Female", weightKg = 60f, heightCm = 165f, age = 30, activityLevel = "Light")
        assertEquals(1800, HealthGoalCalculator.calculateCalorieGoal(femaleProfile))
    }
}
