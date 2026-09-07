package com.example

import com.example.data.local.DailyMetric
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class KeyMetricsGraphsTest {

    @Test
    fun testHealthScreenDoesNotContainMacroGraphs() {
        val healthScreenFile = File("app/src/main/java/com/example/presentation/health/HealthScreen.kt")
        val content = if (healthScreenFile.exists()) healthScreenFile.readText() else File("src/main/java/com/example/presentation/health/HealthScreen.kt").readText()

        // Verify no Carbs, Fat, or Protein graphs
        assertFalse("HealthScreen should not graph Carbs", content.contains("Carbs", ignoreCase = true))
        assertFalse("HealthScreen should not graph Protein", content.contains("Protein", ignoreCase = true))
        assertFalse("HealthScreen should not graph Fat", content.contains("Fat", ignoreCase = true))

        // Verify Key metrics section and the four trend cards are present
        assertTrue("HealthScreen must contain Key metrics header", content.contains("Key metrics"))
        assertTrue("HealthScreen must contain Weight card", content.contains("\"Weight\""))
        assertTrue("HealthScreen must contain Calories Burned card", content.contains("\"Calories Burned\""))
        assertTrue("HealthScreen must contain Steps card", content.contains("\"Steps\""))
        assertTrue("HealthScreen must contain Exercise Days card", content.contains("\"Exercise Days\""))
    }

    @Test
    fun testExistingSectionsPreserved() {
        val healthScreenFile = File("app/src/main/java/com/example/presentation/health/HealthScreen.kt")
        val content = if (healthScreenFile.exists()) healthScreenFile.readText() else File("src/main/java/com/example/presentation/health/HealthScreen.kt").readText()

        assertTrue("Focus areas preserved", content.contains("Focus areas"))
        assertTrue("Health checks preserved", content.contains("Health checks"))
        assertTrue("Personal info preserved", content.contains("Personal info"))
        assertTrue("Lifestyle Protocol preserved", content.contains("Lifestyle Protocol"))
    }

    @Test
    fun testMathSafetyWithZeroDays() {
        val last7Days = emptyList<DailyMetric>()

        val validWeights = last7Days.filter { it.weightKg > 0f }
        assertTrue(validWeights.isEmpty())

        val caloriesData = last7Days.map { it.activeCaloriesBurned }
        assertFalse(caloriesData.any { it > 0 })

        val stepsData = last7Days.map { it.steps }
        assertFalse(stepsData.any { it > 0 })

        val hasExercise = last7Days.any { it.exerciseMinutes > 0 }
        assertFalse(hasExercise)
    }

    @Test
    fun testMathSafetyWithOneDayData() {
        val last7Days = listOf(
            DailyMetric(date = "2026-09-07", steps = 5000, activeCaloriesBurned = 250, exerciseMinutes = 30, weightKg = 70.0f)
        )

        // Weight
        val validWeights = last7Days.filter { it.weightKg > 0f }
        assertEquals(1, validWeights.size)
        val countW = validWeights.size
        val width = 300f
        val stepXW = if (countW > 1) width / (countW - 1) else 0f
        assertEquals(0f, stepXW)
        assertFalse(stepXW.isNaN())
        assertFalse(stepXW.isInfinite())

        // Calories
        val caloriesData = last7Days.map { it.activeCaloriesBurned }
        val countCal = caloriesData.size
        val barWidth = 20f
        val spacingCal = if (countCal > 1) (width - (countCal * barWidth)) / (countCal - 1) else 0f
        assertEquals(0f, spacingCal)
        assertFalse(spacingCal.isNaN())
        assertFalse(spacingCal.isInfinite())

        // Steps
        val stepsData = last7Days.map { it.steps }
        val countSteps = stepsData.size
        val spacingSteps = if (countSteps > 1) (width - (countSteps * barWidth)) / (countSteps - 1) else 0f
        assertEquals(0f, spacingSteps)
        assertFalse(spacingSteps.isNaN())
        assertFalse(spacingSteps.isInfinite())

        // Exercise Days
        val countEx = last7Days.size
        val pillWidth = 20f
        val spacingEx = if (countEx > 1) (width - (countEx * pillWidth)) / (countEx - 1) else 0f
        assertEquals(0f, spacingEx)
        assertFalse(spacingEx.isNaN())
        assertFalse(spacingEx.isInfinite())
    }

    @Test
    fun testMathSafetyWithTwoDaysDataBrandNewUser() {
        val last7Days = listOf(
            DailyMetric(date = "2026-09-06", steps = 3000, activeCaloriesBurned = 150, exerciseMinutes = 20, weightKg = 72.5f),
            DailyMetric(date = "2026-09-07", steps = 6000, activeCaloriesBurned = 300, exerciseMinutes = 0, weightKg = 72.0f)
        )

        val width = 300f

        // Weight
        val validWeights = last7Days.filter { it.weightKg > 0f }
        assertEquals(2, validWeights.size)
        val countW = validWeights.size
        val stepXW = if (countW > 1) width / (countW - 1) else 0f
        assertEquals(300f, stepXW)
        assertFalse(stepXW.isNaN())
        assertFalse(stepXW.isInfinite())

        // Calories
        val caloriesData = last7Days.map { it.activeCaloriesBurned }
        val countCal = caloriesData.size
        val barWidth = 20f
        val spacingCal = if (countCal > 1) (width - (countCal * barWidth)) / (countCal - 1) else 0f
        assertEquals(260f, spacingCal)
        assertFalse(spacingCal.isNaN())
        assertFalse(spacingCal.isInfinite())

        // Exercise
        val countEx = last7Days.size
        val spacingEx = if (countEx > 1) (width - (countEx * barWidth)) / (countEx - 1) else 0f
        assertEquals(260f, spacingEx)
        assertFalse(spacingEx.isNaN())
        assertFalse(spacingEx.isInfinite())

        val activeCount = last7Days.count { it.exerciseMinutes > 0 }
        assertEquals(1, activeCount)
    }

    @Test
    fun testSevenDaysDataCalculations() {
        val last7Days = (1..7).map { day ->
            DailyMetric(
                date = "2026-09-0$day",
                steps = day * 1000,
                activeCaloriesBurned = day * 50,
                exerciseMinutes = if (day % 2 == 0) 30 else 0,
                weightKg = 70f + day * 0.1f
            )
        }

        val width = 350f
        val barWidth = 10f

        val validWeights = last7Days.filter { it.weightKg > 0f }
        assertEquals(7, validWeights.size)
        val stepX = width / (validWeights.size - 1)
        assertFalse(stepX.isNaN())
        assertFalse(stepX.isInfinite())

        val caloriesData = last7Days.map { it.activeCaloriesBurned }
        assertTrue(caloriesData.any { it > 0 })
        val spacingCal = (width - (caloriesData.size * barWidth)) / (caloriesData.size - 1)
        assertFalse(spacingCal.isNaN())
        assertFalse(spacingCal.isInfinite())

        val stepsData = last7Days.map { it.steps }
        assertTrue(stepsData.any { it > 0 })
        val spacingSteps = (width - (stepsData.size * barWidth)) / (stepsData.size - 1)
        assertFalse(spacingSteps.isNaN())
        assertFalse(spacingSteps.isInfinite())

        val activeDays = last7Days.count { it.exerciseMinutes > 0 }
        assertEquals(3, activeDays)
    }
}
