package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WalkingMetricsTest {

    @Test
    fun testStrideMeters() {
        assertEquals(0.7055f, WalkingMetrics.strideMeters(170f), 0.001f)
    }

    @Test
    fun testDistanceMeters() {
        assertEquals(705.5f, WalkingMetrics.distanceMeters(1000, 170f), 0.5f)
    }

    @Test
    fun testCalories() {
        assertEquals(210, WalkingMetrics.calories(70f, 3600))
        assertEquals(105, WalkingMetrics.calories(0f, 1800))
    }

    @Test
    fun testStepsPerMinute() {
        assertEquals(120, WalkingMetrics.stepsPerMinute(600, 300))
        assertEquals(0, WalkingMetrics.stepsPerMinute(5, 5))
    }

    @Test
    fun testFormatDistance() {
        assertEquals("1.00 km", WalkingMetrics.formatDistance(1000f, false))
        assertEquals("1.00 mi", WalkingMetrics.formatDistance(1609.344f, true))
    }
}
