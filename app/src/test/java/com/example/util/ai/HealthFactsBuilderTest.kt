package com.example.util.ai

import com.example.data.local.DailyMetric
import com.example.data.local.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthFactsBuilderTest {

    @Test
    fun testDetectTopics() {
        val topics = HealthFactsBuilder.detectTopics("how did I sleep this week?")
        assertEquals(setOf(HealthTopic.SLEEP), topics)
    }

    @Test
    fun testIsPersonalDataQuestion() {
        assertTrue(HealthFactsBuilder.isPersonalDataQuestion("how did I sleep this week?"))
        assertFalse(HealthFactsBuilder.isPersonalDataQuestion("how can I sleep better"))
    }

    @Test
    fun testBuildSleep() {
        val profile = UserProfile(age = 30, gender = "Male")
        val metrics = listOf(
            DailyMetric(date = "2026-10-01", sleepHours = 6.0f),
            DailyMetric(date = "2026-10-02", sleepHours = 7.0f),
            DailyMetric(date = "2026-10-03", sleepHours = 8.0f)
        )
        val result = HealthFactsBuilder.build(setOf(HealthTopic.SLEEP), profile, metrics, "2026-10-03")
        assertTrue(result.contains("7.0 h"))
        assertTrue(result.contains("8.0 h"))
    }

    @Test
    fun testBuildGlucoseMgdl() {
        val profile = UserProfile(age = 30, gender = "Male", glucoseUnitMgdl = true)
        val metrics = listOf(
            DailyMetric(date = "2026-10-01", bloodGlucoseMorning = 7.0f)
        )
        val result = HealthFactsBuilder.build(setOf(HealthTopic.GLUCOSE), profile, metrics, "2026-10-01")
        assertTrue(result.contains("126"))
        assertTrue(result.contains("mg/dL"))
    }

    @Test
    fun testTopicWithoutData() {
        val profile = UserProfile(age = 30, gender = "Male")
        val result = HealthFactsBuilder.build(setOf(HealthTopic.WATER), profile, emptyList(), "2026-10-01")
        assertTrue(result.contains("no records in the last 30 days"))
    }

    @Test
    fun testBuildBloodPressureAverage() {
        val profile = UserProfile(age = 30, gender = "Male")
        val metrics = listOf(
            DailyMetric(date = "2026-10-01", bloodPressure = "120/80"),
            DailyMetric(date = "2026-10-02", bloodPressure = "130/90")
        )
        val result = HealthFactsBuilder.build(setOf(HealthTopic.BLOOD_PRESSURE), profile, metrics, "2026-10-02")
        assertTrue(result.contains("125/85"))
    }
}
