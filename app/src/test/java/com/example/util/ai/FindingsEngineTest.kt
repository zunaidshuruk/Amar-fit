package com.example.util.ai

import com.example.data.local.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FindingsEngineTest {

    private val todayDate = "2026-10-12"

    private fun createMetric(date: String, sleep: Float = 0f, steps: Int = 0, cal: Int = 0, bp: String = "",
                             bb: Float = 0f, ab: Float = 0f, bl: Float = 0f, al: Float = 0f, bd: Float = 0f, ad: Float = 0f) = DailyMetric(
        date = date,
        sleepHours = sleep,
        steps = steps,
        caloriesConsumed = cal,
        bloodPressure = bp,
        bloodGlucoseBeforeBreakfast = bb,
        bloodGlucoseAfterBreakfast = ab,
        bloodGlucoseBeforeLunch = bl,
        bloodGlucoseAfterLunch = al,
        bloodGlucoseBeforeDinner = bd,
        bloodGlucoseAfterDinner = ad
    )

    private fun createProfile() = UserProfile(
        dailyCalorieLimit = 2000,
        stepGoal = 10000,
        sleepGoalHours = 8.0f,
        bloodGlucoseTargetMax = 7.0f,
        bloodGlucoseTargetMin = 4.0f
    )

    @Test
    fun testSleepFindingSeverity() {
        val metrics = listOf(createMetric("2026-10-12", sleep = 5.0f)) // goal is 8.0
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val findings = FindingsEngine.compute(input)
        
        val f = findings.find { it.topic == FindingTopic.SLEEP }!!
        assertEquals(2, f.severity)
        assertTrue(f.title.contains("5.0 h"))
    }

    @Test
    fun testActivityStepsFinding() {
        val metrics = listOf(createMetric("2026-10-12", steps = 3000)) // goal is 10000
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val findings = FindingsEngine.compute(input)
        
        val f = findings.find { it.tag == "steps_low_avg" }!!
        assertEquals(2, f.severity)
    }

    @Test
    fun testNutritionHighCalorieSeverity() {
        val metrics = listOf(createMetric("2026-10-12", cal = 2650)) // limit is 2000. 2650 is 32% over
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val findings = FindingsEngine.compute(input)
        
        val f = findings.find { it.tag == "kcal_high_30" }!!
        assertEquals(3, f.severity)
    }

    @Test
    fun testGlucoseDinnerRiseSeverity() {
        val metrics = (10..12).map { day ->
            createMetric("2026-10-$day", bd = 6.0f, ad = 9.5f) // rise 3.5
        }
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val findings = FindingsEngine.compute(input)
        
        val f = findings.find { it.tag == "glucose_rise_dinner" }!!
        assertEquals(3, f.severity)
    }

    @Test
    fun testBloodPressureHighAverageSeverity() {
        val metrics = (10..12).map { day ->
            createMetric("2026-10-$day", bp = "142/92")
        }
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val findings = FindingsEngine.compute(input)
        
        val f = findings.find { it.tag == "bp_avg_high_2" }!!
        assertEquals(3, f.severity)
    }

    @Test
    fun testBloodPressureCrisisSeverity() {
        val metrics = listOf(
            createMetric("2026-10-10", bp = "120/80"),
            createMetric("2026-10-11", bp = "120/80"),
            createMetric("2026-10-12", bp = "185/125")
        )
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val findings = FindingsEngine.compute(input)
        
        val f = findings.find { it.tag == "bp_crisis" }!!
        assertEquals(3, f.severity)
        assertTrue(f.evidence.contains("Seek medical advice promptly"))
    }

    @Test
    fun testGlucoseHypoSeverity() {
        val metrics = listOf(createMetric("2026-10-12", bb = 3.4f))
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val findings = FindingsEngine.compute(input)
        
        val f = findings.find { it.tag == "glucose_hypo" }!!
        assertEquals(3, f.severity)
    }

    @Test
    fun testFindingsSortingAndLimit() {
        val metrics = listOf(
            createMetric("2026-10-12", bb = 3.4f), // sev 3
            createMetric("2026-10-12", cal = 2650), // sev 3
            createMetric("2026-10-12", sleep = 5.0f) // sev 2
        )
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val findings = FindingsEngine.compute(input)
        
        assertTrue(findings.size >= 3)
        assertTrue(findings[0].severity >= findings[1].severity)
        assertTrue(findings[1].severity >= findings[2].severity)
        assertTrue(findings.size <= 14)
    }

    @Test
    fun testEmptyInputGaps() {
        val input = DossierInput(UserProfile(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val findings = FindingsEngine.compute(input)
        
        assertTrue(findings.all { it.topic == FindingTopic.DATA })
        assertTrue(findings.any { it.tag == "data_gap_glucose" })
    }

    // ---- weekend (Friday and Saturday) against the other days: the 1.5 h rule

    /** 30 nights ending on [todayDate]; Friday and Saturday get [weekend] hours, every other day [other]. */
    private fun thirtyNights(weekend: Float, other: Float, skipWeekends: Boolean = false): List<DailyMetric> {
        val today = LocalDate.parse(todayDate)
        return (0..29).map { today.minusDays(it.toLong()) }.mapNotNull { d ->
            val isWeekend = d.dayOfWeek == java.time.DayOfWeek.FRIDAY || d.dayOfWeek == java.time.DayOfWeek.SATURDAY
            if (isWeekend && skipWeekends) null else createMetric(d.toString(), sleep = if (isWeekend) weekend else other)
        }
    }

    private fun weekendFinding(metrics: List<DailyMetric>) =
        FindingsEngine.compute(DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate))
            .find { it.tag == "sleep_weekend_gap" }

    @Test
    fun weekendSleepingMuchLongerIsReported() {
        val f = weekendFinding(thirtyNights(weekend = 9.0f, other = 6.5f))!!
        assertEquals(FindingTopic.SLEEP, f.topic)
        assertEquals(1, f.severity)
        assertEquals("You sleep much longer on weekends", f.title)
        assertTrue(f.evidence, f.evidence.contains("9.0 h") && f.evidence.contains("6.5 h") && f.evidence.contains("2.5 h"))
    }

    @Test
    fun weekendSleepingMuchLessIsReported() {
        val f = weekendFinding(thirtyNights(weekend = 5.0f, other = 7.0f))!!
        assertEquals("You sleep much less on weekends", f.title)
    }

    @Test
    fun aGapUnderOneAndAHalfHoursIsNotReported() {
        assertNull(weekendFinding(thirtyNights(weekend = 7.9f, other = 6.6f))) // 1.3 h
    }

    @Test
    fun exactlyOneAndAHalfHoursIsReported() {
        assertNotNull(weekendFinding(thirtyNights(weekend = 8.0f, other = 6.5f)))
    }

    @Test
    fun needsThreeWeekendNightsAndThreeOtherNights() {
        assertNull(weekendFinding(thirtyNights(weekend = 9.0f, other = 6.0f, skipWeekends = true)))
        // only two weekend nights logged
        val few = thirtyNights(weekend = 9.0f, other = 6.0f).filter { m ->
            val d = LocalDate.parse(m.date).dayOfWeek
            !(d == java.time.DayOfWeek.FRIDAY || d == java.time.DayOfWeek.SATURDAY) || m.date >= "2026-10-08"
        }
        assertNull(weekendFinding(few))
    }

    @Test
    fun nightsOlderThanThirtyDaysAreIgnored() {
        val old = (40..60).map { createMetric(LocalDate.parse(todayDate).minusDays(it.toLong()).toString(), sleep = 9.0f) }
        assertNull(weekendFinding(old))
    }
}
