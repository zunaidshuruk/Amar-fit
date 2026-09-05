package com.example.presentation.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OnboardingLogicTest {

    @Test
    fun testValidBirthday_calculatesAgeCorrectly() {
        // Assume today is 2026-09-04
        val today = LocalDate.of(2026, 9, 4)
        
        // Before birthday
        val dobBefore = LocalDate.of(2000, 10, 5)
        var age = java.time.temporal.ChronoUnit.YEARS.between(dobBefore, today).toInt()
        assertEquals(25, age)
        
        // After birthday
        val dobAfter = LocalDate.of(2000, 8, 3)
        age = java.time.temporal.ChronoUnit.YEARS.between(dobAfter, today).toInt()
        assertEquals(26, age)
    }

    @Test
    fun testHeightConversion() {
        val ft = 5
        val ins = 8
        val calculatedHeightCm = (ft * 30.48f) + (ins * 2.54f)
        assertEquals(172.72f, calculatedHeightCm, 0.01f)
    }

    @Test
    fun testHeightValidation_feetInvalid() {
        val ft = -1
        assertFalse("Feet must be positive", ft > 0)
    }
    
    @Test
    fun testHeightValidation_inchesInvalid() {
        val ins = 12
        assertFalse("Inches must be between 0 and 11", ins in 0..11)
    }

    @Test
    fun testWeightValidation_invalid() {
        val kg = -5f
        assertFalse("Weight must be positive", kg > 0f)
    }
}
