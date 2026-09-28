package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkoutExercise(
    val name: String,
    val sets: Int? = null,
    val reps: String? = null,
    val durationSeconds: Int? = null,
    val restSeconds: Int = 30,
    val youtubeSearchQuery: String,
    val metValue: Double = 3.5
)

@JsonClass(generateAdapter = true)
data class WorkoutPlan(
    val title: String,
    val warmup: List<WorkoutExercise> = emptyList(),
    val mainExercises: List<WorkoutExercise> = emptyList(),
    val cooldown: List<WorkoutExercise> = emptyList()
)
