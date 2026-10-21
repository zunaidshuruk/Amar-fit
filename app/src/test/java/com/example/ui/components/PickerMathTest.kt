package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerMathTest {
    private fun near(expected: Float, actual: Float, tolerance: Float) =
        assertTrue("expected $expected but was $actual", kotlin.math.abs(expected - actual) <= tolerance)

    @Test
    fun heightsRoundToTheNearestInchNotDown() {
        assertEquals(Pair(5, 7), PickerMath.cmToFtIn(170f))   // 66.93 in is 5 ft 7 in, not 5 ft 6 in
        assertEquals(Pair(5, 11), PickerMath.cmToFtIn(180f))  // 70.87 in
        assertEquals(Pair(6, 0), PickerMath.cmToFtIn(182.88f))
    }

    @Test
    fun heightGoesThereAndBackWithinHalfAnInch() {
        for (cm in listOf(150f, 160f, 170.5f, 175f, 183f, 190f)) {
            val (ft, inch) = PickerMath.cmToFtIn(cm)
            near(cm, PickerMath.ftInToCm(ft, inch), 1.3f)
        }
    }

    @Test
    fun poundsRoundToTheNearestPound() {
        assertEquals(154, PickerMath.kgToLb(70f))
        assertEquals(160, PickerMath.kgToLb(72.5f))
        assertEquals(44, PickerMath.kgToLb(10f)) // kept inside the wheel
    }

    @Test
    fun weightGoesThereAndBackWithinHalfAPound() {
        for (kg in listOf(45f, 60f, 72.5f, 80f, 99.9f, 120f)) {
            near(kg, PickerMath.lbToKg(PickerMath.kgToLb(kg)), 0.25f)
            val (st, lb) = PickerMath.kgToStLb(kg)
            near(kg, PickerMath.stLbToKg(st, lb), 0.25f)
        }
    }

    @Test
    fun stonesAndPoundsAddUp() {
        assertEquals(Pair(11, 0), PickerMath.kgToStLb(69.85f))
        assertEquals(Pair(11, 5), PickerMath.kgToStLb(72.0f)) // 72 kg is 158.7 lb: 11 st (154 lb) and 5 lb
    }

    @Test
    fun theWheelsRoundAndStayInTheirRange() {
        assertEquals(73, PickerMath.kgToWheel(72.5f))
        assertEquals(171, PickerMath.cmToWheel(170.6f))
        assertEquals(20, PickerMath.kgToWheel(5f))
        assertEquals(300, PickerMath.kgToWheel(999f))
        assertEquals(50, PickerMath.cmToWheel(10f))
        assertEquals(250, PickerMath.cmToWheel(400f))
    }
}
