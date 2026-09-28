package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDietChartDao {
    @Query("SELECT * FROM saved_diet_charts ORDER BY createdAt DESC")
    fun getAllSavedCharts(): Flow<List<SavedDietChart>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChart(chart: SavedDietChart)

    @Delete
    suspend fun deleteChart(chart: SavedDietChart)
}
