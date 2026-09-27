package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: FoodChatMessage): Long

    @Query("SELECT * FROM food_chat_messages ORDER BY timestamp ASC, id ASC")
    fun getAllMessagesFlow(): Flow<List<FoodChatMessage>>

    @Query("SELECT * FROM food_chat_messages ORDER BY timestamp ASC, id ASC")
    suspend fun getAllMessages(): List<FoodChatMessage>

    @Query("DELETE FROM food_chat_messages")
    suspend fun clearAll()
}
