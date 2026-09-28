package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AssistantChatMessageDao {
    @Insert
    suspend fun insert(message: AssistantChatMessage): Long

    @Query("SELECT * FROM assistant_chat_messages ORDER BY timestamp ASC, id ASC")
    suspend fun getAll(): List<AssistantChatMessage>
}
