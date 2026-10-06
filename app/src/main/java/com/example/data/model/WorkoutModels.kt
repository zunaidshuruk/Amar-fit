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
    val cooldown: List<WorkoutExercise> = emptyList(),
    val workoutType: String? = null,
    val rounds: Int = 1,
    val roundRestSeconds: Int = 60
)

@JsonClass(generateAdapter = true)
data class ProgramDay(
    val label: String = "",
    val isRestDay: Boolean = false,
    val plan: WorkoutPlan? = null
)

object ProgramDaysJson {
    private val adapter by lazy {
        com.example.data.remote.RetrofitClient.moshi.adapter<List<ProgramDay>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, ProgramDay::class.java)
        )
    }
    fun parse(json: String): List<ProgramDay> = try { adapter.fromJson(json) ?: emptyList() } catch (e: Exception) { emptyList() }
    fun toJson(days: List<ProgramDay>): String = adapter.toJson(days)
}

@JsonClass(generateAdapter = true)
data class GeneratedProgram(
    val title: String = "",
    val days: List<ProgramDay> = emptyList()
)
