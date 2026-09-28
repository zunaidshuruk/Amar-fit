package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "youtube_video_cache")
data class YoutubeVideoCache(
    @PrimaryKey val query: String,
    val videoId: String,
    val cachedAt: Long = System.currentTimeMillis()
)
