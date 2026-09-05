package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Accent Tokens mapping the semantic domain to a robust light/dark pair.
// These are used for the distinct stat tiles in Redesign Sprint 2.

data class AccentColors(
    val bg: Color,
    val onBg: Color
)

object AccentTokens {
    @Composable
    fun stepsAccent(): AccentColors {
        val isDark = isSystemInDarkTheme()
        // Emerald domain
        return if (isDark) {
            AccentColors(bg = Color(0xFF064E3B), onBg = Color(0xFF6EE7B7)) // Emerald900, Emerald300
        } else {
            AccentColors(bg = Emerald50, onBg = Emerald700)
        }
    }

    @Composable
    fun caloriesAccent(): AccentColors {
        val isDark = isSystemInDarkTheme()
        // Orange domain
        return if (isDark) {
            AccentColors(bg = Color(0xFF7C2D12), onBg = Color(0xFFFDBA74)) // Orange900, Orange300
        } else {
            AccentColors(bg = Orange50, onBg = Orange700)
        }
    }

    @Composable
    fun waterAccent(): AccentColors {
        val isDark = isSystemInDarkTheme()
        // Blue domain
        return if (isDark) {
            AccentColors(bg = Color(0xFF1E3A8A), onBg = Color(0xFF93C5FD)) // Blue900, Blue300
        } else {
            AccentColors(bg = BlueBg, onBg = Blue700)
        }
    }

    @Composable
    fun streakAccent(): AccentColors {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            AccentColors(bg = Color(0xFF451A03), onBg = Orange500)
        } else {
            AccentColors(bg = OrangeBg, onBg = Orange700)
        }
    }
    
    @Composable
    fun pointsAccent(): AccentColors {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            AccentColors(bg = Color(0xFF1E1B4B), onBg = Indigo500) // Indigo900 deeper, Indigo500
        } else {
            AccentColors(bg = IndigoBg, onBg = Indigo700)
        }
    }
    
    @Composable
    fun badgesAccent(): AccentColors {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            AccentColors(bg = Color(0xFF022C22), onBg = Emerald500) // Deeper emerald
        } else {
            AccentColors(bg = Emerald50, onBg = Emerald700)
        }
    }
}
