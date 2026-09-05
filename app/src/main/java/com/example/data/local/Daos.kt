package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)
}

@Dao
interface MetricsDao {
    @Query("SELECT * FROM daily_metrics WHERE date = :date")
    fun getMetricsForDate(date: String): Flow<DailyMetric?>

    @Query("SELECT * FROM daily_metrics ORDER BY date DESC LIMIT :limit")
    fun getMetricsHistory(limit: Int): Flow<List<DailyMetric>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: DailyMetric)
    
    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY id DESC")
    fun getFoodLogsForDate(date: String): Flow<List<FoodLog>>
    @Query("SELECT * FROM food_logs ORDER BY date DESC LIMIT 100")
    fun getRecentFoodLogs(): Flow<List<FoodLog>>
    @Delete
    suspend fun deleteFoodLog(foodLog: FoodLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(foodLog: FoodLog)
    @Update
    suspend fun updateFoodLog(foodLog: FoodLog)

    @Query("DELETE FROM food_logs WHERE date < :cutoffDate")
    suspend fun deleteOldLogs(cutoffDate: String)
}
