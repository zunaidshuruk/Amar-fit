package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem>? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchItem(
    val id: YouTubeResourceId? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeResourceId(
    val videoId: String? = null
)

interface YouTubeApiService {
    @GET("youtube/v3/search")
    suspend fun searchVideo(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 1,
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
        @Query("safeSearch") safeSearch: String = "strict",
        @Query("q") query: String,
        @Query("key") apiKey: String
    ): YouTubeSearchResponse
}
