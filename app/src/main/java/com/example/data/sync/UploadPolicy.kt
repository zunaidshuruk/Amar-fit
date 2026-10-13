package com.example.data.sync

import com.example.data.local.DailyMetric

/**
 * Decides when a changed daily row is uploaded to the cloud.
 * Walking updates (steps, distance, active calories, heart rate) arrive every few seconds while the app is open;
 * they are uploaded at most once every [PASSIVE_INTERVAL_MS] (30 minutes). Anything the user enters
 * (glucose, weight, water, food, sleep, ...) is uploaded straight away.
 */
object UploadPolicy {
    const val PASSIVE_INTERVAL_MS = 30L * 60L * 1000L

    /** True when the only differences between [old] and [new] are the walking / heart rate counters. */
    fun isPassiveOnlyChange(old: DailyMetric, new: DailyMetric): Boolean {
        val normalized = old.copy(
            steps = new.steps,
            distanceMeters = new.distanceMeters,
            activeCaloriesBurned = new.activeCaloriesBurned,
            heartRate = new.heartRate,
            heartRateMin = new.heartRateMin,
            heartRateMax = new.heartRateMax,
            updatedAt = new.updatedAt
        )
        return normalized == new
    }

    /** Milliseconds to wait before the next walking upload is allowed (0 = upload now). */
    fun delayUntilNextWrite(now: Long, lastWrite: Long, interval: Long = PASSIVE_INTERVAL_MS): Long {
        val elapsed = now - lastWrite
        return if (lastWrite <= 0L || elapsed >= interval) 0L else interval - elapsed
    }
}
