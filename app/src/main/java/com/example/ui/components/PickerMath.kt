package com.example.ui.components

import kotlin.math.roundToInt

/** Conversions of the height and weight wheels. Values are rounded to the nearest whole unit (they used to be cut off, which made the value drift each time the picker was opened). */
object PickerMath {
    const val LB_PER_KG = 2.20462f

    fun cmToFtIn(cm: Float): Pair<Int, Int> {
        val totalInches = (cm / 2.54f).roundToInt()
        return Pair((totalInches / 12).coerceIn(3, 8), (totalInches % 12).coerceIn(0, 11))
    }

    fun ftInToCm(ft: Int, inches: Int): Float = ft * 30.48f + inches * 2.54f

    fun kgToLb(kg: Float): Int = (kg * LB_PER_KG).roundToInt().coerceIn(44, 660)

    fun lbToKg(lb: Int): Float = lb / LB_PER_KG

    fun kgToStLb(kg: Float): Pair<Int, Int> {
        val totalLb = (kg * LB_PER_KG).roundToInt()
        return Pair((totalLb / 14).coerceIn(3, 47), (totalLb % 14).coerceIn(0, 13))
    }

    fun stLbToKg(st: Int, lb: Int): Float = (st * 14f + lb) / LB_PER_KG

    fun cmToWheel(cm: Float): Int = cm.roundToInt().coerceIn(50, 250)

    fun kgToWheel(kg: Float): Int = kg.roundToInt().coerceIn(20, 300)
}
