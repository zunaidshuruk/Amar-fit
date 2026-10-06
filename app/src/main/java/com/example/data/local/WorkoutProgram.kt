package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_programs")
data class WorkoutProgram(
    @PrimaryKey val cloudId: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val daysJson: String = "[]",
    val nextDayIndex: Int = 0,
    val completedDays: Int = 0,
    val lastCompletedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "manual"
)
