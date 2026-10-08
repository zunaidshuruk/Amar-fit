package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WalkingMetricsTest {

    @Test
    fun testDistanceCalculations() {
        val steps = 1000
        val distMeters = WalkingMetrics.calculateDistanceMeters(steps)
        val distKm = WalkingMetrics.calculateDistanceKm(steps)
        
        assertEquals(762.0f, distMeters, 0.1f)
        assertEquals(0.762f, distKm, 0.001f)
    }

    @Test
    fun testCalorieCalculations() {
        val steps = 2000
        val calories = WalkingMetrics.calculateCalories(steps)
        assertEquals(80.0f, calories, 0.1f)
    }

    @Test
    fun testPaceAndFormatting() {
        val steps = 1312
        val elapsedSeconds = 600
        val paceSecs = WalkingMetrics.calculatePaceSecondsPerKm(steps, elapsedSeconds)
        
        assertEquals(600, paceSecs)
        
        val formattedPace = WalkingMetrics.formatPace(600)
        assertEquals("10'00\" /km", formattedPace)
        
        val formattedDistM = WalkingMetrics.formatDistance(0.5f)
        assertEquals("500 m", formattedDistM)

        val formattedDistKm = WalkingMetrics.formatDistance(1.5f)
        assertEquals("1.50 km", formattedDistKm)
    }

    @Test
    fun testZeroValues() {
        assertEquals(0f, WalkingMetrics.calculateDistanceMeters(0), 0.001f)
        assertEquals(0f, WalkingMetrics.calculateCalories(0), 0.001f)
        assertEquals(0, WalkingMetrics.calculatePaceSecondsPerKm(0, 0))
        assertEquals("--'--\" /km", WalkingMetrics.formatPace(0))
    }
}
