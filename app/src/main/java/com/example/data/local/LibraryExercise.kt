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

fun findLibraryExerciseByName(
    exerciseName: String,
    allExercises: List<LibraryExercise>
): LibraryExercise? {
    val trimmedName = exerciseName.trim()
    if (trimmedName.isEmpty() || allExercises.isEmpty()) return null

    // 1. Exact case-insensitive match on name first
    val exactMatch = allExercises.firstOrNull { it.name.trim().equals(trimmedName, ignoreCase = true) }
    if (exactMatch != null) return exactMatch

    // 2. Normalized substring fallback (strip punctuation, lowercase, check either contains the other)
    fun normalize(str: String): String {
        return str.lowercase()
            .replace(Regex("[^a-z0-9]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    val normalizedTarget = normalize(trimmedName)
    if (normalizedTarget.isEmpty()) return null

    return allExercises.firstOrNull { exercise ->
        val normalizedExerciseName = normalize(exercise.name)
        normalizedExerciseName.isNotEmpty() && (
            normalizedExerciseName.contains(normalizedTarget) || normalizedTarget.contains(normalizedExerciseName)
        )
    }
}

