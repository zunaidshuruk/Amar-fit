package com.example.data.sync

import com.example.data.local.DailyMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadPolicyTest {

    private val base = DailyMetric(date = "2026-10-12", steps = 100, waterLiters = 0.5f, updatedAt = 1)

    @Test
    fun walkingCountersAreAPassiveChange() {
        val next = base.copy(steps = 250, distanceMeters = 180f, activeCaloriesBurned = 9, heartRate = 88, heartRateMax = 99, updatedAt = 2)
        assertTrue(UploadPolicy.isPassiveOnlyChange(base, next))
    }

    @Test
    fun userEnteredDataIsNotPassive() {
        assertFalse(UploadPolicy.isPassiveOnlyChange(base, base.copy(bloodGlucoseBeforeBreakfast = 6.1f, updatedAt = 2)))
        assertFalse(UploadPolicy.isPassiveOnlyChange(base, base.copy(waterLiters = 1.0f, updatedAt = 2)))
        assertFalse(UploadPolicy.isPassiveOnlyChange(base, base.copy(weightKg = 80f, updatedAt = 2)))
        assertFalse(UploadPolicy.isPassiveOnlyChange(base, base.copy(sleepHours = 7f, updatedAt = 2)))
    }

    @Test
    fun aMixedChangeIsNotPassive() {
        assertFalse(UploadPolicy.isPassiveOnlyChange(base, base.copy(steps = 500, waterLiters = 1.5f, updatedAt = 2)))
    }

    @Test
    fun firstWriteGoesOutImmediately() {
        assertEquals(0L, UploadPolicy.delayUntilNextWrite(now = 1_000_000L, lastWrite = 0L))
    }

    @Test
    fun walkingWritesAreSpacedThirtyMinutesApart() {
        val last = 10_000_000L
        val tenMin = 10L * 60L * 1000L
        assertEquals(20L * 60L * 1000L, UploadPolicy.delayUntilNextWrite(last + tenMin, last))
        assertEquals(0L, UploadPolicy.delayUntilNextWrite(last + UploadPolicy.PASSIVE_INTERVAL_MS, last))
        assertEquals(0L, UploadPolicy.delayUntilNextWrite(last + 45L * 60L * 1000L, last))
    }
}
