package com.example.data.sync

import com.example.data.local.DailyMetric
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricMergerTest {

    @Test
    fun testCountersTakeHigher() {
        val a = DailyMetric(date = "2026-01-01", steps = 21)
        val b = DailyMetric(date = "2026-01-01", steps = 9209)
        
        val mergedAB = MetricMerger.merge(a, b)
        assertEquals(9209, mergedAB.steps)
        
        val mergedBA = MetricMerger.merge(b, a)
        assertEquals(9209, mergedBA.steps)
    }

    @Test
    fun testZeroNeverOverwritesData() {
        val local = DailyMetric(date = "2026-01-01", bloodGlucoseMorning = 0f)
        val remote = DailyMetric(date = "2026-01-01", bloodGlucoseMorning = 7.2f)
        
        val merged = MetricMerger.merge(local, remote)
        assertEquals(7.2f, merged.bloodGlucoseMorning, 0.001f)
        
        val mergedReverse = MetricMerger.merge(remote, local)
        assertEquals(7.2f, mergedReverse.bloodGlucoseMorning, 0.001f)
    }

    @Test
    fun testNewerReadingWins() {
        val local = DailyMetric(date = "2026-01-01", weightKg = 80f, updatedAt = 10L)
        val remote = DailyMetric(date = "2026-01-01", weightKg = 82f, updatedAt = 20L)
        
        val merged = MetricMerger.merge(local, remote)
        assertEquals(82f, merged.weightKg, 0.001f)
        
        val localNewer = DailyMetric(date = "2026-01-01", weightKg = 80f, updatedAt = 20L)
        val remoteOlder = DailyMetric(date = "2026-01-01", weightKg = 82f, updatedAt = 10L)
        
        val merged2 = MetricMerger.merge(localNewer, remoteOlder)
        assertEquals(80f, merged2.weightKg, 0.001f)
    }

    @Test
    fun testTieFavoursLocal() {
        val local = DailyMetric(date = "2026-01-01", weightKg = 80f, updatedAt = 10L)
        val remote = DailyMetric(date = "2026-01-01", weightKg = 82f, updatedAt = 10L)
        
        val merged = MetricMerger.merge(local, remote)
        assertEquals(80f, merged.weightKg, 0.001f)
    }

    @Test
    fun testHeartRateMinMax() {
        val m1 = DailyMetric(date = "2026-01-01", heartRateMin = 0, heartRateMax = 0)
        val m2 = DailyMetric(date = "2026-01-01", heartRateMin = 55, heartRateMax = 120)
        
        val merged1 = MetricMerger.merge(m1, m2)
        assertEquals(55, merged1.heartRateMin)
        assertEquals(120, merged1.heartRateMax)
        
        val m3 = DailyMetric(date = "2026-01-01", heartRateMin = 60, heartRateMax = 110)
        val m4 = DailyMetric(date = "2026-01-01", heartRateMin = 55, heartRateMax = 130)
        
        val merged2 = MetricMerger.merge(m3, m4)
        assertEquals(55, merged2.heartRateMin)
        assertEquals(130, merged2.heartRateMax)
    }

    @Test
    fun testBloodPressureBundle() {
        val local = DailyMetric(date = "2026-01-01", bloodPressure = "", bloodPressureBodyPosition = "Not set", updatedAt = 10L)
        val remote = DailyMetric(date = "2026-01-01", bloodPressure = "120/80", bloodPressureBodyPosition = "Sitting", bloodPressureArmLocation = "Left", updatedAt = 20L)
        
        val merged = MetricMerger.merge(local, remote)
        assertEquals("120/80", merged.bloodPressure)
        assertEquals("Sitting", merged.bloodPressureBodyPosition)
        assertEquals("Left", merged.bloodPressureArmLocation)
    }

    @Test
    fun testSpecimenSource() {
        val m1 = DailyMetric(date = "2026-01-01", bloodGlucoseSpecimenSource = "Not set")
        val m2 = DailyMetric(date = "2026-01-01", bloodGlucoseSpecimenSource = "Capillary")
        
        assertEquals("Capillary", MetricMerger.merge(m1, m2).bloodGlucoseSpecimenSource)
    }

    @Test
    fun testSkinTemp() {
        val m1 = DailyMetric(date = "2026-01-01", skinTemperatureCelsius = 0f)
        val m2 = DailyMetric(date = "2026-01-01", skinTemperatureCelsius = -0.4f)
        
        assertEquals(-0.4f, MetricMerger.merge(m1, m2).skinTemperatureCelsius, 0.001f)
    }

    @Test
    fun testUpdatedAtIsMax() {
        val m1 = DailyMetric(date = "2026-01-01", updatedAt = 100L)
        val m2 = DailyMetric(date = "2026-01-01", updatedAt = 200L)
        
        assertEquals(200L, MetricMerger.merge(m1, m2).updatedAt)
    }

    @Test
    fun testIdempotency() {
        val m = DailyMetric(date = "2026-01-01", steps = 1000, weightKg = 70f, updatedAt = 500L)
        val merged = MetricMerger.merge(m, m)
        assertEquals(m, merged)
    }

    @Test
    fun testDifferentDatesReturnLocal() {
        val local = DailyMetric(date = "2026-01-01", steps = 100)
        val remote = DailyMetric(date = "2026-01-02", steps = 200)
        
        val merged = MetricMerger.merge(local, remote)
        assertEquals(local, merged)
    }
}
