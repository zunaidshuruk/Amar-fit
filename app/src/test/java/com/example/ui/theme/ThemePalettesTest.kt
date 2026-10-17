package com.example.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePalettesTest {
    private val all = ThemePalettes.all

    @Test
    fun thereAreSeveralPalettesWithUniqueIdsAndNames() {
        assertTrue(all.size >= 6)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertEquals(all.size, all.map { it.name }.toSet().size)
        assertTrue(all.all { it.id.isNotBlank() && it.id == it.id.lowercase() && it.name.isNotBlank() })
    }

    @Test
    fun theDefaultIsTheCurrentChartreuseLook() {
        assertEquals("chartreuse", ThemePalettes.DEFAULT_ID)
        val d = ThemePalettes.default
        assertEquals(0xFF00272BL, d.background)
        assertEquals(0xFFBBE800L, d.accent)
        assertEquals(0xFFF5A623L, d.alert)
        assertEquals(0xFFCDD2D7L, d.secondary)
    }

    @Test
    fun anUnknownOrMissingIdGivesTheDefault() {
        assertEquals(ThemePalettes.default, ThemePalettes.byId(null))
        assertEquals(ThemePalettes.default, ThemePalettes.byId(""))
        assertEquals(ThemePalettes.default, ThemePalettes.byId("does-not-exist"))
        assertEquals("ocean", ThemePalettes.byId("ocean").id)
    }

    @Test
    fun everyColorIsFullyOpaque() {
        for (p in all) for (c in listOf(p.background, p.surface, p.surfaceVariant, p.accent, p.onAccent, p.secondary, p.alert, p.text)) {
            assertEquals("${p.id} has a see-through color", 0xFFL, (c shr 24) and 0xFF)
        }
    }

    @Test
    fun theAccentAndTheTextOnItAreEasyToRead() {
        for (p in all) {
            assertTrue("${p.id}: accent on background ${ColorMath.contrast(p.accent, p.background)}", ColorMath.contrast(p.accent, p.background) >= 7.0)
            assertTrue("${p.id}: text on accent ${ColorMath.contrast(p.onAccent, p.accent)}", ColorMath.contrast(p.onAccent, p.accent) >= 7.0)
            assertTrue("${p.id}: accent on cards ${ColorMath.contrast(p.accent, p.surfaceVariant)}", ColorMath.contrast(p.accent, p.surfaceVariant) >= 4.5)
        }
    }

    @Test
    fun theMainAndMutedTextAreEasyToReadOnEveryBackground() {
        for (p in all) {
            for ((label, bg) in listOf("background" to p.background, "surface" to p.surface, "cards" to p.surfaceVariant)) {
                assertTrue("${p.id}: text on $label", ColorMath.contrast(p.text, bg) >= 7.0)
                assertTrue("${p.id}: muted text on $label", ColorMath.contrast(p.secondary, bg) >= 4.5)
            }
        }
    }

    @Test
    fun theWarningColorIsReadableAndNeverTooCloseToTheAccent() {
        for (p in all) {
            assertTrue("${p.id}: warning on cards ${ColorMath.contrast(p.alert, p.surfaceVariant)}", ColorMath.contrast(p.alert, p.surfaceVariant) >= 4.5)
            assertTrue("${p.id}: warning on background", ColorMath.contrast(p.alert, p.background) >= 4.5)
            // colour-blind safety: the warning must not drift towards the accent's hue (the grey Graphite accent has no hue)
            if (p.id != "graphite") assertTrue("${p.id}: hue gap ${ColorMath.hueGap(p.accent, p.alert)}", ColorMath.hueGap(p.accent, p.alert) >= 25.0)
        }
    }

    @Test
    fun theLayersGetLighterFromBackgroundToCards() {
        for (p in all) {
            assertTrue("${p.id}: surface lighter than background", ColorMath.luminance(p.surface) > ColorMath.luminance(p.background))
            assertTrue("${p.id}: cards lighter than surface", ColorMath.luminance(p.surfaceVariant) > ColorMath.luminance(p.surface))
        }
    }

    private fun near(expected: Double, actual: Double, tolerance: Double) =
        assertTrue("expected $expected but was $actual", kotlin.math.abs(expected - actual) <= tolerance)

    @Test
    fun theColorMathMatchesKnownValues() {
        near(21.0, ColorMath.contrast(0xFFFFFFFFL, 0xFF000000L), 0.01)
        near(1.0, ColorMath.contrast(0xFF336699L, 0xFF336699L), 0.001)
        near(0.0, ColorMath.hue(0xFF808080L), 0.001)
        near(120.0, ColorMath.hue(0xFF00FF00L), 0.5)
        near(240.0, ColorMath.hue(0xFF0000FFL), 0.5)
        near(180.0, ColorMath.hueGap(0xFFFF0000L, 0xFF00FFFFL), 0.5)
        assertTrue(ColorMath.hueGap(0xFFFF0000L, 0xFFFF5500L) < 25.0) // red to orange is a small gap
    }
}
