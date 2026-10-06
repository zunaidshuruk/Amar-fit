package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseCalorieEstimatorTest {
    @Test
    fun testSessionSeconds() {
        assertEquals(210, ExerciseCalorieEstimator.sessionSeconds(3, 10, 60))
        assertEquals(60, ExerciseCalorieEstimator.sessionSeconds(1, 1, 0))
    }

    @Test
    fun testEstimateKcal() {
        assertEquals(20, ExerciseCalorieEstimator.estimateKcal("strength", 70f, 210))
        assertEquals(315, ExerciseCalorieEstimator.estimateKcal(null, 0f, 3600))
    }

    @Test
    fun testMetFor() {
        assertEquals(7.0, ExerciseCalorieEstimator.metFor("Cardio"), 0.001)
        assertEquals(6.0, ExerciseCalorieEstimator.metFor("Olympic Weightlifting"), 0.001)
    }

    @Test
    fun testWorkoutTypeKeyFor() {
        assertEquals("strength_training", ExerciseCalorieEstimator.workoutTypeKeyFor("strength"))
        assertEquals("strength_training", ExerciseCalorieEstimator.workoutTypeKeyFor("Olympic Weightlifting"))
        assertEquals("hiit", ExerciseCalorieEstimator.workoutTypeKeyFor("plyometrics"))
        assertEquals("stretching", ExerciseCalorieEstimator.workoutTypeKeyFor("stretching"))
        assertEquals(null, ExerciseCalorieEstimator.workoutTypeKeyFor("cardio"))
    }
}
