package com.example.data.local

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LibraryExercise(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val equipment: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val level: String = "",
    val instructions: List<String> = emptyList(),
    val images: List<String> = emptyList()
)
