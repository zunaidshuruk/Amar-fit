package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "assistant_chat_sessions")
data class AssistantChatSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val preview: String = "",
    val messageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
