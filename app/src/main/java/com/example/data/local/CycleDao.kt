package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Query("SELECT * FROM period_logs WHERE removed = 0 ORDER BY startDate ASC")
    fun observePeriods(): Flow<List<PeriodLog>>

    /** Everything including periods marked removed (needed to sync deletions). */
    @Query("SELECT * FROM period_logs")
    suspend fun getAllPeriodsIncludingRemoved(): List<PeriodLog>

    @Query("SELECT * FROM period_logs WHERE cloudId = :id")
    suspend fun getPeriod(id: String): PeriodLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeriod(period: PeriodLog)

    @Query("DELETE FROM period_logs WHERE removed = 1 AND updatedAt < :before")
    suspend fun purgeRemovedBefore(before: Long)

    @Query("DELETE FROM period_logs")
    suspend fun clearPeriods()

    @Query("SELECT * FROM cycle_days ORDER BY date ASC")
    fun observeDays(): Flow<List<CycleDayLog>>

    @Query("SELECT * FROM cycle_days")
    suspend fun getAllDays(): List<CycleDayLog>

    @Query("SELECT * FROM cycle_days WHERE date = :date")
    suspend fun getDay(date: String): CycleDayLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(day: CycleDayLog)

    @Query("DELETE FROM cycle_days")
    suspend fun clearDays()

    @Query("SELECT * FROM fasting_days WHERE type > 0 ORDER BY date ASC")
    fun observeFasting(): Flow<List<FastingLog>>

    @Query("SELECT * FROM fasting_days")
    suspend fun getAllFasting(): List<FastingLog>

    @Query("SELECT * FROM fasting_days WHERE date = :date")
    suspend fun getFasting(date: String): FastingLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFasting(entry: FastingLog)

    @Query("DELETE FROM fasting_days")
    suspend fun clearFasting()
}
