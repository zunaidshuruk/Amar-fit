package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface YoutubeVideoCacheDao {
    @Query("SELECT * FROM youtube_video_cache WHERE `query` = :query LIMIT 1")
    suspend fun getByQuery(query: String): YoutubeVideoCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: YoutubeVideoCache)
}
