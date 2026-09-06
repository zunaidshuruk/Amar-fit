package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_events")
data class ActivityEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
