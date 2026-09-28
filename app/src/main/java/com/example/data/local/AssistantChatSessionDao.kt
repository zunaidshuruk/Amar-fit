package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantChatSessionDao {
    @Query("SELECT * FROM assistant_chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<AssistantChatSession>>

    @Query("SELECT * FROM assistant_chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): AssistantChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: AssistantChatSession)

    @Query("DELETE FROM assistant_chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM assistant_chat_sessions")
    suspend fun clearAll()
}
