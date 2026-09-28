package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FoodChatMessageDao {
    @Insert
    suspend fun insert(message: FoodChatMessage): Long

    @Query("SELECT * FROM food_chat_messages ORDER BY timestamp ASC, id ASC")
    suspend fun getAll(): List<FoodChatMessage>
}
