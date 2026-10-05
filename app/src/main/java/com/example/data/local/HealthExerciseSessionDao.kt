package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthExerciseSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<HealthExerciseSession>)

    @Query("DELETE FROM health_exercise_sessions WHERE startTime >= :fromMillis AND startTime < :toMillis")
    suspend fun deleteBetween(fromMillis: Long, toMillis: Long)

    @Query("SELECT * FROM health_exercise_sessions WHERE startTime >= :fromMillis AND startTime < :toMillis ORDER BY startTime DESC")
    fun observeBetween(fromMillis: Long, toMillis: Long): Flow<List<HealthExerciseSession>>

    @Query("SELECT MIN(startTime) FROM health_exercise_sessions")
    suspend fun earliestStart(): Long?
}
