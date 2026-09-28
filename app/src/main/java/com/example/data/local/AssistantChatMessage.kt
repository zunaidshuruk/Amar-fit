package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistant_chat_messages")
data class AssistantChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
