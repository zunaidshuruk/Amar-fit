package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {

    @Test
    fun verifyDesignTokens() {
        // Verify approved Sprint 1 hex codes are correct
        assertEquals("Background color should match #F8F9FA", Color(0xFFF8F9FA), Background)
        assertEquals("Surface color should match #FFFFFF", Color(0xFFFFFFFF), Surface)
        assertEquals("Primary color should match #1B4528", Color(0xFF1B4528), Primary)
        assertEquals("Secondary color should match #2ECC71", Color(0xFF2ECC71), Secondary)
        assertEquals("TextPrimary color should match #1E1E1E", Color(0xFF1E1E1E), TextPrimary)
    }
}
