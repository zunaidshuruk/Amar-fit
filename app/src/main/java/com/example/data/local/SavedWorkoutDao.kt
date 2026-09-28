package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedWorkoutDao {
    @Query("SELECT * FROM saved_workouts ORDER BY createdAt DESC")
    fun getAllSavedWorkouts(): Flow<List<SavedWorkout>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: SavedWorkout)

    @Delete
    suspend fun deleteWorkout(workout: SavedWorkout)
}
