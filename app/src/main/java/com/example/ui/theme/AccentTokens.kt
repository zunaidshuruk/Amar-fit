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
        // Steps / positive-progress domain (teal-green)
        return if (isDark) {
            AccentColors(bg = Color(0xFF123832), onBg = Color(0xFF5FE0C4))
        } else {
            AccentColors(bg = Emerald50, onBg = Emerald700)
        }
    }

    fun caloriesAccent(isDark: Boolean): AccentColors {
        // Orange domain - muted dark bg with soft light tint
        return if (isDark) {
            AccentColors(bg = Color(0xFF382014), onBg = Color(0xFFFFB27D))
        } else {
            AccentColors(bg = Orange50, onBg = Orange700)
        }
    }

    fun waterAccent(isDark: Boolean): AccentColors {
        // Blue / cardio / water domain
        return if (isDark) {
            AccentColors(bg = Color(0xFF152A52), onBg = Color(0xFF8FB8FF))
        } else {
            AccentColors(bg = BlueBg, onBg = Blue700)
        }
    }

    fun streakAccent(isDark: Boolean): AccentColors {
        // Orange domain - muted dark bg with soft light tint
        return if (isDark) {
            AccentColors(bg = Color(0xFF351C0C), onBg = Color(0xFFFFB076))
        } else {
            AccentColors(bg = OrangeBg, onBg = Orange700)
        }
    }
    
    fun pointsAccent(isDark: Boolean): AccentColors {
        // Indigo domain - deep muted purple-indigo
        return if (isDark) {
            AccentColors(bg = Color(0xFF221F45), onBg = Color(0xFFA5B4FC))
        } else {
            AccentColors(bg = IndigoBg, onBg = Indigo700)
        }
    }
    
    fun badgesAccent(isDark: Boolean): AccentColors {
        // Emerald domain - deep muted dark green
        return if (isDark) {
            AccentColors(bg = Color(0xFF0F3324), onBg = Color(0xFF6EE7B7))
        } else {
            AccentColors(bg = Emerald50, onBg = Emerald700)
        }
    }

    fun glucoseAccent(isDark: Boolean): AccentColors {
        // Indigo domain - deep muted indigo
        return if (isDark) {
            AccentColors(bg = Color(0xFF1C2248), onBg = Color(0xFFA5B4FC))
        } else {
            AccentColors(bg = Indigo50, onBg = Indigo700)
        }
    }

    fun weightAccent(isDark: Boolean): AccentColors {
        // Orange domain - muted warm amber/orange
        return if (isDark) {
            AccentColors(bg = Color(0xFF382014), onBg = Color(0xFFFFB27D))
        } else {
            AccentColors(bg = Orange50, onBg = Orange700)
        }
    }

    fun bmiAccent(isDark: Boolean): AccentColors {
        return if (isDark) {
            AccentColors(bg = Color(0xFF123832), onBg = Color(0xFF5FE0C4))
        } else {
            AccentColors(bg = Emerald50, onBg = Emerald700)
        }
    }

    fun heartRateAccent(isDark: Boolean): AccentColors {
        // Red domain - muted dark wine/red bg with soft light rose onBg
        return if (isDark) {
            AccentColors(bg = Color(0xFF3E1719), onBg = Color(0xFFFFA6A6))
        } else {
            AccentColors(bg = Red50, onBg = Red700)
        }
    }

    fun bloodPressureAccent(isDark: Boolean): AccentColors {
        // Blue domain - deep muted blue
        return if (isDark) {
            AccentColors(bg = Color(0xFF152A52), onBg = Color(0xFF8FB8FF))
        } else {
            AccentColors(bg = BlueBg, onBg = Blue700)
        }
    }

    fun mindfulnessAccent(isDark: Boolean): AccentColors {
        // Mindfulness / calm domain (purple)
        return if (isDark) {
            AccentColors(bg = Color(0xFF332352), onBg = Color(0xFFC9B6F5))
        } else {
            AccentColors(bg = Color(0xFFF3E8FF), onBg = Color(0xFF7E22CE))
        }
    }

    fun foodLogAccent(isDark: Boolean): AccentColors = stepsAccent(isDark)

    fun mealPlanAccent(isDark: Boolean): AccentColors = waterAccent(isDark)

    fun dietChartAccent(isDark: Boolean): AccentColors = pointsAccent(isDark)

    fun recipeAccent(isDark: Boolean): AccentColors = heartRateAccent(isDark)

    fun sleepAccent(isDark: Boolean): AccentColors = mindfulnessAccent(isDark)

    fun coachAccent(isDark: Boolean): AccentColors = waterAccent(isDark)
}
