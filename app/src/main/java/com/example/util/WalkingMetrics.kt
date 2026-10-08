package com.example.util

object WalkingMetrics {
    const val DEFAULT_STRIDE_LENGTH_METERS = 0.762f
    const val CALORIES_PER_STEP = 0.04f

    fun calculateDistanceMeters(steps: Int, strideLengthMeters: Float = DEFAULT_STRIDE_LENGTH_METERS): Float {
        if (steps <= 0) return 0f
        return steps * strideLengthMeters
    }

    fun calculateDistanceKm(steps: Int, strideLengthMeters: Float = DEFAULT_STRIDE_LENGTH_METERS): Float {
        return calculateDistanceMeters(steps, strideLengthMeters) / 1000f
    }

    fun calculateCalories(steps: Int): Float {
        if (steps <= 0) return 0f
        return steps * CALORIES_PER_STEP
    }

    fun calculatePaceSecondsPerKm(steps: Int, elapsedSeconds: Int, strideLengthMeters: Float = DEFAULT_STRIDE_LENGTH_METERS): Int {
        val distKm = calculateDistanceKm(steps, strideLengthMeters)
        if (distKm <= 0f || elapsedSeconds <= 0) return 0
        return (elapsedSeconds / distKm).toInt()
    }

    fun formatPace(paceSecondsPerKm: Int): String {
        if (paceSecondsPerKm <= 0) return "--'--\" /km"
        val mins = paceSecondsPerKm / 60
        val secs = paceSecondsPerKm % 60
        return String.format(java.util.Locale.US, "%d'%02d\" /km", mins, secs)
    }

    fun formatDistance(distanceKm: Float): String {
        return if (distanceKm < 1.0f) {
            String.format(java.util.Locale.US, "%.0f m", distanceKm * 1000f)
        } else {
            String.format(java.util.Locale.US, "%.2f km", distanceKm)
        }
    }
}
