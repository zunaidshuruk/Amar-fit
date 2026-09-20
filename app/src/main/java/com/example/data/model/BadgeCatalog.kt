package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

data class BadgeDefinition(
    val id: String, // must exactly match the string stored in UserProfile.badges
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val points: Int
)

val ALL_BADGES: List<BadgeDefinition> = listOf(
    BadgeDefinition(
        id = "Hydration Hero",
        displayName = "Hydration Hero",
        description = "Hit your daily water goal.",
        icon = Icons.Default.WaterDrop,
        points = 50
    ),
    BadgeDefinition(
        id = "10k Steps Master",
        displayName = "10k Steps Master",
        description = "Log 10,000 steps in a single day.",
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        points = 100
    ),
    BadgeDefinition(
        id = "Consistency Starter",
        displayName = "Consistency Starter",
        description = "Log your health data to get started.",
        icon = Icons.Default.EventAvailable,
        points = 20
    )
)
