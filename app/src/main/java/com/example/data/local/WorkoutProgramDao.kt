package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutProgramDao {
    @Query("SELECT * FROM workout_programs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<WorkoutProgram>>

    @Query("SELECT * FROM workout_programs")
    suspend fun getAllOnce(): List<WorkoutProgram>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(program: WorkoutProgram)

    @Query("DELETE FROM workout_programs WHERE cloudId = :cloudId")
    suspend fun delete(cloudId: String)
}
