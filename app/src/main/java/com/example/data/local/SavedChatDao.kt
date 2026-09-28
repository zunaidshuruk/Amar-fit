package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedChatDao {
    @Query("SELECT * FROM saved_chats ORDER BY createdAt DESC")
    fun getAllSavedChats(): Flow<List<SavedChat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: SavedChat)

    @Delete
    suspend fun deleteChat(chat: SavedChat)
}
