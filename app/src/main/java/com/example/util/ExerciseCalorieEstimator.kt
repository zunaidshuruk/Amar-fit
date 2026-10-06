package com.example.util

object ExerciseCalorieEstimator {
    const val SECONDS_PER_REP = 3
    const val DEFAULT_REST_SECONDS = 60

    fun metFor(category: String?): Double = when (category?.trim()?.lowercase()) {
        "strength" -> 5.0
        "powerlifting", "olympic weightlifting", "strongman" -> 6.0
        "plyometrics" -> 8.0
        "cardio" -> 7.0
        "stretching" -> 2.3
        else -> 4.5
    }

    fun sessionSeconds(sets: Int, reps: Int, restSeconds: Int = DEFAULT_REST_SECONDS): Int {
        val s = sets.coerceIn(1, 20)
        val r = reps.coerceIn(1, 200)
        val rest = restSeconds.coerceIn(0, 600)
        return (s * r * SECONDS_PER_REP + (s - 1) * rest).coerceAtLeast(60)
    }

    fun estimateKcal(category: String?, weightKg: Float, durationSeconds: Int): Int {
        val weight = if (weightKg > 0f) weightKg.toDouble() else 70.0
        val hours = durationSeconds.coerceAtLeast(0) / 3600.0
        return kotlin.math.round(metFor(category) * weight * hours).toInt().coerceAtLeast(0)
    }

    fun workoutTypeKeyFor(category: String?): String? = when (category?.trim()?.lowercase()) {
        "strength", "powerlifting", "olympic weightlifting", "strongman" -> "strength_training"
        "plyometrics" -> "hiit"
        "stretching" -> "stretching"
        else -> null
    }
}
