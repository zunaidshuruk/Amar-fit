package com.example.util

fun cmToFeetInches(cm: Float): Pair<Int, Int> {
    val totalInches = (cm / 2.54f).toInt()
    val feet = totalInches / 12
    val inches = totalInches % 12
    return Pair(feet, inches)
}

fun feetInchesToCm(feet: Int, inches: Int): Float {
    return (feet * 30.48f) + (inches * 2.54f)
}

fun kgToLbs(kg: Float): Float {
    return kg * 2.20462f
}

fun lbsToKg(lbs: Float): Float {
    return lbs / 2.20462f
}

fun formatHeight(cm: Float, useImperial: Boolean): String {
    return if (useImperial) {
        val (ft, ins) = cmToFeetInches(cm)
        "$ft ft $ins in"
    } else {
        "${cm.toInt()} cm"
    }
}

fun formatWeight(kg: Float, useImperial: Boolean): String {
    return if (useImperial) {
        "${"%.1f".format(kgToLbs(kg))} lbs"
    } else {
        "${"%.1f".format(kg)} kg"
    }
}
