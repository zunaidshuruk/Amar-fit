package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "health_exercise_sessions", indices = [Index(value = ["startTime"])])
data class HealthExerciseSession(
    @PrimaryKey val recordId: String,
    val exerciseType: Int = 0,
    val typeLabel: String = "Workout",
    val title: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val durationMinutes: Int = 0,
    val activeCalories: Int = 0,
    val totalCalories: Int = 0,
    val distanceMeters: Float = 0f,
    val sourcePackage: String = "",
    val sourceLabel: String = "",
    val isOwnApp: Boolean = false
)
