package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Accent Tokens mapping the semantic domain to a robust light/dark pair.
// These are used for the distinct stat tiles in Redesign Sprint 2.

data class AccentColors(
    val bg: Color,
    val onBg: Color
)

object AccentTokens {
    fun stepsAccent(isDark: Boolean): AccentColors {
        // Emerald domain
        return if (isDark) {
            AccentColors(bg = Color(0xFF064E3B), onBg = Color(0xFF6EE7B7)) // Emerald900, Emerald300
        } else {
            AccentColors(bg = Emerald50, onBg = Emerald700)
        }
    }

    fun caloriesAccent(isDark: Boolean): AccentColors {
        // Orange domain
        return if (isDark) {
            AccentColors(bg = Color(0xFF7C2D12), onBg = Color(0xFFFDBA74)) // Orange900, Orange300
        } else {
            AccentColors(bg = Orange50, onBg = Orange700)
        }
    }

    fun waterAccent(isDark: Boolean): AccentColors {
        // Blue domain
        return if (isDark) {
            AccentColors(bg = Color(0xFF1E3A8A), onBg = Color(0xFF93C5FD)) // Blue900, Blue300
        } else {
            AccentColors(bg = BlueBg, onBg = Blue700)
        }
    }

    fun streakAccent(isDark: Boolean): AccentColors {
        return if (isDark) {
            AccentColors(bg = Color(0xFF451A03), onBg = Orange500)
        } else {
            AccentColors(bg = OrangeBg, onBg = Orange700)
        }
    }
    
    fun pointsAccent(isDark: Boolean): AccentColors {
        return if (isDark) {
            AccentColors(bg = Color(0xFF1E1B4B), onBg = Indigo500) // Indigo900 deeper, Indigo500
        } else {
            AccentColors(bg = IndigoBg, onBg = Indigo700)
        }
    }
    
    fun badgesAccent(isDark: Boolean): AccentColors {
        return if (isDark) {
            AccentColors(bg = Color(0xFF022C22), onBg = Emerald500) // Deeper emerald
        } else {
            AccentColors(bg = Emerald50, onBg = Emerald700)
        }
    }

    fun glucoseAccent(isDark: Boolean): AccentColors {
        return if (isDark) {
            AccentColors(bg = Color(0xFF1E1B4B), onBg = Indigo500)
        } else {
            AccentColors(bg = Indigo50, onBg = Indigo700)
        }
    }

    fun weightAccent(isDark: Boolean): AccentColors {
        return if (isDark) {
            AccentColors(bg = Color(0xFF7C2D12), onBg = Color(0xFFFDBA74))
        } else {
            AccentColors(bg = Orange50, onBg = Orange700)
        }
    }

    fun bmiAccent(isDark: Boolean): AccentColors {
        return if (isDark) {
            AccentColors(bg = Color(0xFF064E3B), onBg = Color(0xFF6EE7B7))
        } else {
            AccentColors(bg = Emerald50, onBg = Emerald700)
        }
    }

    fun heartRateAccent(isDark: Boolean): AccentColors {
        return if (isDark) {
            AccentColors(bg = Red900, onBg = Color(0xFFFCA5A5))
        } else {
            AccentColors(bg = Red50, onBg = Red700)
        }
    }

    fun bloodPressureAccent(isDark: Boolean): AccentColors {
        return if (isDark) {
            AccentColors(bg = Blue900, onBg = Color(0xFF93C5FD))
        } else {
            AccentColors(bg = BlueBg, onBg = Blue700)
        }
    }

    fun foodLogAccent(isDark: Boolean): AccentColors = stepsAccent(isDark)

    fun mealPlanAccent(isDark: Boolean): AccentColors = waterAccent(isDark)

    fun dietChartAccent(isDark: Boolean): AccentColors = pointsAccent(isDark)

    fun recipeAccent(isDark: Boolean): AccentColors = heartRateAccent(isDark)
}
