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

const val MGDL_PER_MMOL = 18.0182f
fun mmolToMgdl(mmol: Float): Float = mmol * MGDL_PER_MMOL
fun mgdlToMmol(mgdl: Float): Float = mgdl / MGDL_PER_MMOL
fun glucoseForDisplay(mmol: Float, useMgdl: Boolean): Float = if (useMgdl) mmolToMgdl(mmol) else mmol
fun glucoseFromInput(value: Float, useMgdl: Boolean): Float = if (useMgdl) mgdlToMmol(value) else value
fun glucoseUnitLabel(useMgdl: Boolean): String = if (useMgdl) "mg/dL" else "mmol/L"
fun formatGlucoseNumber(mmol: Float, useMgdl: Boolean): String = if (useMgdl) Math.round(mmolToMgdl(mmol)).toString() else String.format(java.util.Locale.US, "%.1f", mmol)
fun formatGlucose(mmol: Float, useMgdl: Boolean): String = formatGlucoseNumber(mmol, useMgdl) + " " + glucoseUnitLabel(useMgdl)
