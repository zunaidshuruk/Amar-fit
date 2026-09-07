package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalRecordDao {
    @Query("SELECT * FROM medical_records ORDER BY createdAt DESC")
    fun getAllMedicalRecords(): Flow<List<MedicalRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalRecord(record: MedicalRecord)

    @Delete
    suspend fun deleteMedicalRecord(record: MedicalRecord)
}
