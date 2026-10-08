package com.example.util

import java.util.Locale

object WalkingMetrics {
    fun strideMeters(heightCm: Float): Float = (if (heightCm > 0f) heightCm else 170f) * 0.415f / 100f

    fun distanceMeters(steps: Int, heightCm: Float): Float = steps.coerceAtLeast(0) * strideMeters(heightCm)

    fun calories(weightKg: Float, durationSeconds: Int): Int =
        Math.round(3.0 * (if (weightKg > 0f) weightKg else 70f) * (durationSeconds.coerceAtLeast(0) / 3600.0)).toInt()

    fun stepsPerMinute(steps: Int, durationSeconds: Int): Int =
        if (durationSeconds < 10) 0 else Math.round(steps * 60f / durationSeconds)

    fun formatDistance(meters: Float, imperial: Boolean): String =
        if (imperial) String.format(Locale.US, "%.2f mi", meters / 1609.344f)
        else String.format(Locale.US, "%.2f km", meters / 1000f)
}
