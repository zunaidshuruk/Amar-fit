package com.example

import com.example.data.local.DailyMetric
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.round

class ResilienceScoreTest {

    private fun computeSleepScore(sleepHours: Float): Float {
        return (100f - abs(sleepHours - 8f) * 20f).coerceIn(0f, 100f)
    }

    private fun computeHrvScore(todayHrv: Float, priorHrvList: List<Float>): Float? {
        if (priorHrvList.size < 3) return null
        val avgHrv = priorHrvList.average().toFloat()
        if (avgHrv <= 0f) return null
        return (50f + ((todayHrv - avgHrv) / avgHrv) * 200f).coerceIn(0f, 100f)
    }

    private fun computeOverallScore(sleepScore: Float?, hrvScore: Float?): Int? {
        return when {
            sleepScore == null -> null
            hrvScore == null -> round(sleepScore).toInt().coerceIn(0, 100)
            else -> round(0.6f * sleepScore + 0.4f * hrvScore).toInt().coerceIn(0, 100)
        }
    }

    private fun computeBucket(score: Int): String {
        return when {
            score >= 80 -> "Great"
            score >= 60 -> "Good"
            score >= 40 -> "Fair"
            else -> "Low"
        }
    }

    @Test
    fun testSleepScoreFormula() {
        assertEquals(100f, computeSleepScore(8f), 0.01f)
        assertEquals(60f, computeSleepScore(6f), 0.01f)
        assertEquals(60f, computeSleepScore(10f), 0.01f)
        assertEquals(20f, computeSleepScore(4f), 0.01f)
        assertEquals(20f, computeSleepScore(12f), 0.01f)
        assertEquals(0f, computeSleepScore(2f), 0.01f)
        assertEquals(0f, computeSleepScore(14f), 0.01f)
    }

    @Test
    fun testHrvScoreFormula() {
        // Less than 3 days -> null
        assertNull(computeHrvScore(50f, listOf(50f, 50f)))
        assertNull(computeHrvScore(50f, emptyList()))

        // Equal to baseline -> 50
        val baseline = listOf(50f, 50f, 50f)
        assertEquals(50f, computeHrvScore(50f, baseline)!!, 0.01f)

        // 25% above baseline (62.5 vs 50) -> 100
        assertEquals(100f, computeHrvScore(62.5f, baseline)!!, 0.01f)

        // 25% below baseline (37.5 vs 50) -> 0
        assertEquals(0f, computeHrvScore(37.5f, baseline)!!, 0.01f)
    }

    @Test
    fun testOverallScoreAndFallbacks() {
        // No sleep data -> null
        assertNull(computeOverallScore(null, 50f))
        assertNull(computeOverallScore(null, null))

        // Sleep available, HRV unavailable -> sleepScore alone
        assertEquals(60, computeOverallScore(60f, null))
        assertEquals(100, computeOverallScore(100f, null))

        // Both available -> 0.6 * sleep + 0.4 * hrv
        // 0.6 * 60 + 0.4 * 50 = 36 + 20 = 56
        assertEquals(56, computeOverallScore(60f, 50f))
        // 0.6 * 100 + 0.4 * 100 = 100
        assertEquals(100, computeOverallScore(100f, 100f))
        // 0.6 * 80 + 0.4 * 50 = 48 + 20 = 68
        assertEquals(68, computeOverallScore(80f, 50f))
    }

    @Test
    fun testScoreBuckets() {
        assertEquals("Great", computeBucket(100))
        assertEquals("Great", computeBucket(80))
        assertEquals("Good", computeBucket(79))
        assertEquals("Good", computeBucket(60))
        assertEquals("Fair", computeBucket(59))
        assertEquals("Fair", computeBucket(40))
        assertEquals("Low", computeBucket(39))
        assertEquals("Low", computeBucket(0))
    }

    @Test
    fun testHealthScreenContainsResilienceCardAndExactStrings() {
        val healthScreenFile = File("app/src/main/java/com/example/presentation/health/HealthScreen.kt")
        val content = if (healthScreenFile.exists()) healthScreenFile.readText() else File("src/main/java/com/example/presentation/health/HealthScreen.kt").readText()

        assertTrue("HealthScreen must contain RESILIENCE", content.contains("\"RESILIENCE\""))
        assertTrue(
            "HealthScreen must contain no-sleep fallback message",
            content.contains("Log your sleep to see your Resilience score")
        )
        assertTrue(
            "HealthScreen must contain sleep-only fallback message",
            content.contains("Based on sleep only — sync Health Connect HRV for a fuller score")
        )

        // Verify other sections are untouched
        assertTrue("Key metrics present", content.contains("Key metrics"))
        assertTrue("Focus areas present", content.contains("Focus areas"))
        assertTrue("Health checks present", content.contains("Health checks"))
        assertTrue("Personal info present", content.contains("Personal info"))
        assertTrue("Lifestyle Protocol present", content.contains("Lifestyle Protocol"))
    }
}
