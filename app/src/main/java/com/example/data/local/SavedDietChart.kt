package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_diet_charts")
data class SavedDietChart(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val chartContent: String,
    val shoppingList: String,
    val createdAt: Long = System.currentTimeMillis()
)
