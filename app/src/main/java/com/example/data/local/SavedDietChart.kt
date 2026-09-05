package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "saved_diet_charts")
data class SavedDietChart(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cloudId: String = UUID.randomUUID().toString(),
    val name: String,
    val chartContent: String,
    val shoppingList: String,
    val createdAt: Long = System.currentTimeMillis()
)
