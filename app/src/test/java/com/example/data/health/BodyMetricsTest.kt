package com.example.data.health

import com.example.data.health.BodyMetrics.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyMetricsTest {
    private fun near(expected: Float, actual: Float, tolerance: Float = 0.05f) =
        assertTrue("expected $expected but was $actual", kotlin.math.abs(expected - actual) <= tolerance)

    private fun exactly(expected: Float, actual: Float) = near(expected, actual, 0.0001f)

    @Test
    fun bmiIsWeightOverHeightSquared() {
        near(24.2f, BodyMetrics.bmi(70f, 170f))
        near(22.0f, BodyMetrics.bmi(71.3f, 180f))
        near(31.2f, BodyMetrics.bmi(100f, 179f))
    }

    @Test
    fun aMissingWeightOrHeightGivesNoBmiNeverNaNOrInfinity() {
        exactly(0f, BodyMetrics.bmi(0f, 170f))
        exactly(0f, BodyMetrics.bmi(70f, 0f))
        exactly(0f, BodyMetrics.bmi(-5f, 170f))
        exactly(0f, BodyMetrics.bmi(70f, -170f))
        assertFalse(BodyMetrics.bmi(70f, 0f).isNaN())
    }

    @Test
    fun theCategoriesChangeExactlyAtTheUsualLimits() {
        assertEquals(Category.NONE, BodyMetrics.category(0f))
        assertEquals(Category.UNDERWEIGHT, BodyMetrics.category(18.49f))
        assertEquals(Category.NORMAL, BodyMetrics.category(18.5f))
        assertEquals(Category.NORMAL, BodyMetrics.category(24.99f))
        assertEquals(Category.OVERWEIGHT, BodyMetrics.category(25f))
        assertEquals(Category.OVERWEIGHT, BodyMetrics.category(29.99f))
        assertEquals(Category.OBESE, BodyMetrics.category(30f))
    }

    @Test
    fun theLabelsArePlain() {
        assertEquals("", BodyMetrics.label(Category.NONE))
        assertEquals("Underweight", BodyMetrics.label(Category.UNDERWEIGHT))
        assertEquals("Normal", BodyMetrics.label(Category.NORMAL))
        assertEquals("Overweight", BodyMetrics.label(Category.OVERWEIGHT))
        assertEquals("Obese", BodyMetrics.label(Category.OBESE))
    }

    @Test
    fun theBarOnTheHealthTabFillsFromFifteenToThirtyFive() {
        exactly(0f, BodyMetrics.fillFraction(0f))
        exactly(0f, BodyMetrics.fillFraction(15f))
        exactly(0.5f, BodyMetrics.fillFraction(25f))
        exactly(1f, BodyMetrics.fillFraction(35f))
        exactly(1f, BodyMetrics.fillFraction(50f))
        exactly(0f, BodyMetrics.fillFraction(10f))
    }

    @Test
    fun theIdealWeightGivesABmiOfTwentyTwo() {
        near(22f, BodyMetrics.bmi(BodyMetrics.idealWeightKg(170f), 170f), 0.01f)
        near(63.6f, BodyMetrics.idealWeightKg(170f))
        exactly(0f, BodyMetrics.idealWeightKg(0f))
    }

    @Test
    fun onlySensibleWeightsAndHeightsMaySaved() {
        assertTrue(BodyMetrics.isValid(70f, 170f))
        assertTrue(BodyMetrics.isValid(20f, 50f))
        assertTrue(BodyMetrics.isValid(300f, 250f))
        assertFalse(BodyMetrics.isValid(19.9f, 170f))
        assertFalse(BodyMetrics.isValid(70f, 49f))
        assertFalse(BodyMetrics.isValid(301f, 170f))
        assertFalse(BodyMetrics.isValid(0f, 0f))
    }

    @Test
    fun theNumberAlwaysUsesADotAndShowsDashesWhenThereIsNothing() {
        assertEquals("24.2", BodyMetrics.format(24.2f))
        assertEquals("18.5", BodyMetrics.format(18.5f))
        assertEquals("--", BodyMetrics.format(0f))
        val saved = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale("bn", "BD"))
            assertEquals("a Bangla phone must still show 24.2", "24.2", BodyMetrics.format(24.2f))
            java.util.Locale.setDefault(java.util.Locale("de", "DE"))
            assertEquals("a German phone must not show 24,2", "24.2", BodyMetrics.format(24.2f))
        } finally {
            java.util.Locale.setDefault(saved)
        }
    }
}
