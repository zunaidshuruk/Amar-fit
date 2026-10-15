package com.example.data.nutrition

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionViewTest {

    @Test
    fun theIntakePercentFollowsTheGoal() {
        assertEquals(68, NutritionView.intakePercent(1250, 1850))
        assertEquals(0, NutritionView.intakePercent(0, 1850))
        assertEquals(100, NutritionView.intakePercent(2000, 2000))
        assertEquals(125, NutritionView.intakePercent(2500, 2000))
        assertEquals(0, NutritionView.intakePercent(500, 0))
    }

    @Test
    fun theTargetRangeIsTheLastTenPercentUpToTheGoal() {
        assertEquals(NutritionView.TargetRange(1670, 1850), NutritionView.targetRange(1850))
        assertEquals(NutritionView.TargetRange(1800, 2000), NutritionView.targetRange(2000))
        assertEquals(NutritionView.TargetRange(0, 0), NutritionView.targetRange(0))
    }

    @Test
    fun theStatusSaysWhereYouAre() {
        assertEquals(NutritionView.CalorieStatus.NOTHING_LOGGED, NutritionView.status(0, 2000))
        assertEquals(NutritionView.CalorieStatus.NOTHING_LOGGED, NutritionView.status(500, 0))
        assertEquals(NutritionView.CalorieStatus.BELOW_RANGE, NutritionView.status(1200, 2000))
        assertEquals(NutritionView.CalorieStatus.BELOW_RANGE, NutritionView.status(1799, 2000))
        assertEquals(NutritionView.CalorieStatus.IN_RANGE, NutritionView.status(1800, 2000))
        assertEquals(NutritionView.CalorieStatus.IN_RANGE, NutritionView.status(2000, 2000))
        assertEquals(NutritionView.CalorieStatus.OVER, NutritionView.status(2001, 2000))
    }

    @Test
    fun theStatusTextIsPlain() {
        assertEquals("Nothing logged yet today", NutritionView.statusText(0, 2000))
        assertEquals("600 kcal to reach your target range", NutritionView.statusText(1200, 2000))
        assertEquals("In your target range", NutritionView.statusText(1900, 2000))
        assertEquals("250 kcal over your target", NutritionView.statusText(2250, 2000))
        assertEquals("1,000 kcal over your target", NutritionView.statusText(3000, 2000))
    }

    @Test
    fun macroPercentsOfTheGoalAreSensibleNumbers() {
        assertEquals(55, NutritionView.macroPercentOfGoal(109f, 198))
        assertEquals(0, NutritionView.macroPercentOfGoal(10f, 0))
        assertEquals(0, NutritionView.macroPercentOfGoal(-5f, 100))
        assertEquals(150, NutritionView.macroPercentOfGoal(150f, 100))
        assertEquals(999, NutritionView.macroPercentOfGoal(99999f, 100))
    }

    @Test
    fun theMacroSplitAlwaysAddsUpToOneHundred() {
        val split = NutritionView.macroSplit(109f, 34f, 13.5f)!!
        assertEquals(100, split.first + split.second + split.third)
        // 436 + 136 + 121.5 kcal = 62.9%, 19.6% and 17.5%: rounded down to 62, 19, 17 and the two missing points go to the largest remainders (carbs, protein)
        assertEquals(Triple(63, 20, 17), split)
        for ((c, p, f) in listOf(Triple(1f, 1f, 1f), Triple(33f, 33f, 33f), Triple(250f, 60f, 70f), Triple(7f, 13f, 0f))) {
            val s = NutritionView.macroSplit(c, p, f)!!
            assertEquals("$c/$p/$f", 100, s.first + s.second + s.third)
        }
    }

    @Test
    fun noMacrosMeansNothingToSplit() {
        assertNull(NutritionView.macroSplit(0f, 0f, 0f))
        assertNull(NutritionView.macroSplit(-3f, 0f, 0f))
    }

    @Test
    fun aSingleMacroIsAllOfIt() {
        assertEquals(Triple(100, 0, 0), NutritionView.macroSplit(50f, 0f, 0f))
        assertEquals(Triple(0, 0, 100), NutritionView.macroSplit(0f, 0f, 20f))
    }

    @Test
    fun theWeekEndsTodayAndFillsMissingDaysWithZero() {
        val week = NutritionView.lastSevenDays(LocalDate.parse("2026-10-15"), mapOf("2026-10-15" to 1850, "2026-10-13" to 1700, "2026-09-01" to 999))
        assertEquals(7, week.size)
        assertEquals(LocalDate.parse("2026-10-09"), week.first().first)
        assertEquals(LocalDate.parse("2026-10-15") to 1850, week.last())
        assertEquals(1700, week[4].second)
        assertEquals(0, week[5].second)
        assertTrue(week.none { it.second == 999 })
    }

    @Test
    fun thousandsAreSeparated() {
        assertEquals("1,850", NutritionView.format(1850))
        assertEquals("850", NutritionView.format(850))
    }
}
