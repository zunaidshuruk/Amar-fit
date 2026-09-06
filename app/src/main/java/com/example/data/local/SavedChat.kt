package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "saved_chats")
data class SavedChat(
    @PrimaryKey val cloudId: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: String,
    val createdAt: Long = System.currentTimeMillis()
)
