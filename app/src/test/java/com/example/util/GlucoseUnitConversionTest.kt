package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlucoseUnitConversionTest {
    @Test
    fun testGlucoseUnitConversion() {
        assertEquals("126 mg/dL", formatGlucose(7.0f, true))
        assertEquals("7.0 mmol/L", formatGlucose(7.04f, false))
        assertTrue(Math.abs(mmolToMgdl(5.5f) - 99.1f) < 0.1f)
        assertTrue(Math.abs(glucoseFromInput(126f, true) - 6.99f) < 0.02f)
        assertTrue(Math.abs(mgdlToMmol(mmolToMgdl(6.3f)) - 6.3f) < 0.001f)
        assertEquals("mmol/L", glucoseUnitLabel(false))
        assertEquals("mg/dL", glucoseUnitLabel(true))
    }
}
