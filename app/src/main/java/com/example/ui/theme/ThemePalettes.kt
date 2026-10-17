package com.example.ui.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * One predefined color set the user can pick under Settings > App theme. Every palette is dark (the app is dark-only) and keeps the same roles:
 * [accent] = the signature color (buttons, rings, selected states), [onAccent] = text on it, [secondary] = muted text and outlines,
 * [alert] = warnings and "over target" (always a warm amber, so it stays different from the accent), [text] = main text.
 * Colors are ARGB Longs so the contrast of every pair can be tested without Compose (ThemePalettesTest).
 */
data class ThemePalette(
    val id: String,
    val name: String,
    val background: Long,
    val surface: Long,
    val surfaceVariant: Long,
    val accent: Long,
    val onAccent: Long,
    val secondary: Long,
    val alert: Long,
    val text: Long = 0xFFE0E0E0
)

object ThemePalettes {
    const val DEFAULT_ID = "chartreuse"

    val all: List<ThemePalette> = listOf(
        ThemePalette("chartreuse", "Chartreuse", 0xFF00272B, 0xFF0C3135, 0xFF163A3E, 0xFFBBE800, 0xFF00272B, 0xFFCDD2D7, 0xFFF5A623),
        ThemePalette("ocean", "Ocean", 0xFF0A1929, 0xFF112A44, 0xFF1B3A5C, 0xFF4FC3F7, 0xFF0A1929, 0xFFCFD8DC, 0xFFFFB74D),
        ThemePalette("violet", "Violet", 0xFF170F2B, 0xFF221840, 0xFF2E2256, 0xFFBB99FF, 0xFF170F2B, 0xFFD1C4E9, 0xFFFFCA28),
        ThemePalette("rose", "Rose", 0xFF2A0F1A, 0xFF3A1626, 0xFF4A1F33, 0xFFFF8FAB, 0xFF2A0F1A, 0xFFF3D4DC, 0xFFFFD166),
        ThemePalette("mint", "Mint", 0xFF052B24, 0xFF0A3A31, 0xFF11493E, 0xFF5EEAD4, 0xFF052B24, 0xFFCFE8E3, 0xFFF5A623),
        ThemePalette("graphite", "Graphite", 0xFF121212, 0xFF1E1E1E, 0xFF2A2A2A, 0xFFE6E6E6, 0xFF121212, 0xFFB8BEC4, 0xFFF5A623)
    )

    val default: ThemePalette = all.first { it.id == DEFAULT_ID }

    /** The palette with this id; an unknown, blank or missing id (for example from a newer app version) gives the default. */
    fun byId(id: String?): ThemePalette = all.firstOrNull { it.id == id } ?: default
}

/** WCAG contrast and hue, used to keep every palette readable. */
object ColorMath {
    private fun channel(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF).toInt()
        val g = ((argb shr 8) and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    /** Contrast ratio from 1 (identical) to 21 (black on white). */
    fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Hue in degrees (0 to 360); 0 for greys. */
    fun hue(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val d = mx - mn
        if (d == 0.0) return 0.0
        val h = when (mx) {
            r -> ((g - b) / d) % 6
            g -> (b - r) / d + 2
            else -> (r - g) / d + 4
        } * 60
        return if (h < 0) h + 360 else h
    }

    /** Smallest distance between two hues on the colour wheel, in degrees. */
    fun hueGap(a: Long, b: Long): Double {
        val d = abs(hue(a) - hue(b))
        return min(d, 360 - d)
    }
}
