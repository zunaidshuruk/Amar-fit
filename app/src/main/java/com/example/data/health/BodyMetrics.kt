package com.example.data.health

import java.util.Locale

/** The one place where BMI and the weight and height limits are worked out (it used to be written out separately in three screens). */
object BodyMetrics {
    const val MIN_WEIGHT_KG = 20f
    const val MAX_WEIGHT_KG = 300f
    const val MIN_HEIGHT_CM = 50f
    const val MAX_HEIGHT_CM = 250f

    enum class Category { NONE, UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE }

    /** BMI = kg / (m x m); 0 when either value is missing. */
    fun bmi(weightKg: Float, heightCm: Float): Float {
        if (weightKg <= 0f || heightCm <= 0f) return 0f
        val meters = heightCm / 100f
        return weightKg / (meters * meters)
    }

    fun category(bmi: Float): Category = when {
        bmi <= 0f -> Category.NONE
        bmi < 18.5f -> Category.UNDERWEIGHT
        bmi < 25f -> Category.NORMAL
        bmi < 30f -> Category.OVERWEIGHT
        else -> Category.OBESE
    }

    fun label(category: Category): String = when (category) {
        Category.NONE -> ""
        Category.UNDERWEIGHT -> "Underweight"
        Category.NORMAL -> "Normal"
        Category.OVERWEIGHT -> "Overweight"
        Category.OBESE -> "Obese"
    }

    /** How far along the 15 to 35 scale of the bar on the Health tab the BMI is, 0 to 1. */
    fun fillFraction(bmi: Float): Float = if (bmi > 0f) ((bmi - 15f) / (35f - 15f)).coerceIn(0f, 1f) else 0f

    /** The weight that gives a BMI of 22 at this height. */
    fun idealWeightKg(heightCm: Float): Float = if (heightCm > 0f) 22f * (heightCm / 100f) * (heightCm / 100f) else 0f

    fun isValid(weightKg: Float, heightCm: Float): Boolean =
        weightKg in MIN_WEIGHT_KG..MAX_WEIGHT_KG && heightCm in MIN_HEIGHT_CM..MAX_HEIGHT_CM

    /** "24.2", or "--" when there is nothing to show; always with a dot, whatever the phone's language. */
    fun format(bmi: Float): String = if (bmi > 0f) String.format(Locale.US, "%.1f", bmi) else "--"
}
