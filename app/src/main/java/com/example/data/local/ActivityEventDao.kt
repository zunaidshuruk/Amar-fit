package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ActivityEvent): Long

    @Query("SELECT * FROM activity_events WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    fun getTodayEvents(startOfDay: Long, endOfDay: Long): Flow<List<ActivityEvent>>

    @Query("SELECT * FROM activity_events WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getEventsSince(startOfDay: Long): Flow<List<ActivityEvent>>

    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<ActivityEvent>>
}
