package com.example.data.nutrition

import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/** The numbers behind the Nutrition screen: intake against the calorie goal, the target range, and the macro split. */
object NutritionView {

    fun intakePercent(consumed: Int, goal: Int): Int = if (goal <= 0) 0 else (consumed * 100f / goal).roundToInt()

    data class TargetRange(val low: Int, val high: Int)

    /** The target range is the last 10% below the goal (rounded to 10 kcal) up to the goal itself. */
    fun targetRange(goal: Int): TargetRange =
        if (goal <= 0) TargetRange(0, 0) else TargetRange(((goal * 0.9f) / 10f).roundToInt() * 10, goal)

    enum class CalorieStatus { NOTHING_LOGGED, BELOW_RANGE, IN_RANGE, OVER }

    fun status(consumed: Int, goal: Int): CalorieStatus {
        if (goal <= 0 || consumed <= 0) return CalorieStatus.NOTHING_LOGGED
        val range = targetRange(goal)
        return when {
            consumed < range.low -> CalorieStatus.BELOW_RANGE
            consumed <= range.high -> CalorieStatus.IN_RANGE
            else -> CalorieStatus.OVER
        }
    }

    fun statusText(consumed: Int, goal: Int): String = when (status(consumed, goal)) {
        CalorieStatus.NOTHING_LOGGED -> "Nothing logged yet today"
        CalorieStatus.BELOW_RANGE -> "${format(targetRange(goal).low - consumed)} kcal to reach your target range"
        CalorieStatus.IN_RANGE -> "In your target range"
        CalorieStatus.OVER -> "${format(consumed - goal)} kcal over your target"
    }

    fun macroPercentOfGoal(actualG: Float, goalG: Int): Int = if (goalG <= 0) 0 else (actualG * 100f / goalG).roundToInt().coerceIn(0, 999)

    /**
     * The shares of carbs, protein and fat in the calories (4, 4 and 9 kcal per gram) as whole percentages that always add up to 100
     * (largest remainder), or null when there is nothing to split.
     */
    fun macroSplit(carbsG: Float, proteinG: Float, fatG: Float): Triple<Int, Int, Int>? {
        val calories = floatArrayOf(carbsG.coerceAtLeast(0f) * 4f, proteinG.coerceAtLeast(0f) * 4f, fatG.coerceAtLeast(0f) * 9f)
        val total = calories.sum()
        if (total <= 0f) return null
        val exact = calories.map { it * 100f / total }
        val floors = exact.map { it.toInt() }.toMutableList()
        var left = 100 - floors.sum()
        for (i in exact.indices.sortedByDescending { exact[it] - floors[it] }) {
            if (left <= 0) break
            floors[i] += 1
            left--
        }
        return Triple(floors[0], floors[1], floors[2])
    }

    /** Calories per day for the 7 days ending on [today], oldest first; a day without a record is 0. */
    fun lastSevenDays(today: LocalDate, caloriesByDate: Map<String, Int>): List<Pair<LocalDate, Int>> =
        (6 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            day to (caloriesByDate[day.toString()] ?: 0)
        }

    fun format(value: Int): String = String.format(Locale.US, "%,d", value)
}
