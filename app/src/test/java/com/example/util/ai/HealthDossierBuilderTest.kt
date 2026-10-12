package com.example.util.ai

import com.example.data.local.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class HealthDossierBuilderTest {

    private val todayDate = "2026-10-12"

    private fun createMetric(date: String, sleep: Float = 0f, steps: Int = 0, weight: Float = 0f, bp: String = "",
                             bb: Float = 0f, ab: Float = 0f, bl: Float = 0f, al: Float = 0f, bd: Float = 0f, ad: Float = 0f) = DailyMetric(
        date = date,
        sleepHours = sleep,
        steps = steps,
        weightKg = weight,
        bloodPressure = bp,
        bloodGlucoseBeforeBreakfast = bb,
        bloodGlucoseAfterBreakfast = ab,
        bloodGlucoseBeforeLunch = bl,
        bloodGlucoseAfterLunch = al,
        bloodGlucoseBeforeDinner = bd,
        bloodGlucoseAfterDinner = ad
    )

    private fun createProfile(mgdl: Boolean = false) = UserProfile(
        age = 30,
        gender = "Male",
        heightCm = 175f,
        weightKg = 75f,
        glucoseUnitMgdl = mgdl,
        bloodGlucoseTargetMin = 4.0f,
        bloodGlucoseTargetMax = 7.0f,
        stepGoal = 8000,
        sleepGoalHours = 8.0f
    )

    @Test
    fun testCalendarWindows() {
        val metrics = listOf(
            createMetric("2026-10-02", sleep = 6.0f), // 10 days back
            createMetric("2026-10-10", sleep = 7.0f), // 2 days back
            createMetric("2026-10-12", sleep = 8.0f)  // today
        )
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val dossier = HealthDossierBuilder.build(input)
        
        val sleepSection = dossier.sections.find { it.title == "Sleep" }!!
        // 7-day window: 10-06 to 10-12. Includes 10-10 and 10-12. (7+8)/2 = 7.5
        // 30-day window: 09-13 to 10-12. Includes all three. (6+7+8)/3 = 7.0
        assertTrue(sleepSection.lines.any { it.contains("7.5 h") })
        assertTrue(sleepSection.lines.any { it.contains("7.0 h") })
    }

    @Test
    fun testSleepSectionAverages() {
        val metrics = listOf(
            createMetric("2026-10-10", sleep = 6.0f),
            createMetric("2026-10-11", sleep = 7.0f),
            createMetric("2026-10-12", sleep = 8.0f)
        )
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val dossier = HealthDossierBuilder.build(input)
        
        val sleepSection = dossier.sections.find { it.title == "Sleep" }!!
        assertTrue(sleepSection.lines.any { it.contains("7.0 h (7-day)") })
        assertTrue(sleepSection.lines.any { it.contains("Nights under 6.0 h: 0/7") })
    }

    @Test
    fun testGlucoseDinnerRiseAndFormatting() {
        val metrics = (10..12).map { day ->
            createMetric("2026-10-$day", bd = 6.0f, ad = 9.0f)
        }
        val profile = createProfile(mgdl = true)
        val input = DossierInput(profile, metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val dossier = HealthDossierBuilder.build(input)
        
        val glucoseSection = dossier.sections.find { it.title == "Glucose" }!!
        // Rise is 3.0 mmol/L. In mg/dL it's 3.0 * 18 = 54
        assertTrue(glucoseSection.lines.any { it.contains("Dinner rise") && it.contains("54 mg/dL") && it.contains("3 pairs") })
    }

    @Test
    fun testBloodPressureParsingAndAverage() {
        val metrics = listOf(
            createMetric("2026-10-10", bp = "120/80"),
            createMetric("2026-10-12", bp = "140/90")
        )
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val dossier = HealthDossierBuilder.build(input)
        
        val bpSection = dossier.sections.find { it.title == "Blood pressure" }!!
        assertTrue(bpSection.lines.any { it.contains("Average: 130/85 mmHg") })
        assertTrue(bpSection.lines.any { it.contains("Elevated (>130/80): 2 readings") })
    }

    @Test
    fun testTopFoodsOrdering() {
        val logs = listOf(
            FoodLog(date = "2026-10-10", name = "Rice"),
            FoodLog(date = "2026-10-10", name = "Rice"),
            FoodLog(date = "2026-10-11", name = "Rice"),
            FoodLog(date = "2026-10-11", name = "Egg"),
            FoodLog(date = "2026-10-12", name = "Egg")
        )
        val input = DossierInput(createProfile(), emptyList(), logs, emptyList(), emptyList(), emptyList(), todayDate)
        val dossier = HealthDossierBuilder.build(input)
        
        val nutritionSection = dossier.sections.find { it.title == "Nutrition" }!!
        assertTrue(nutritionSection.lines.any { it.contains("Rice (3), Egg (2)") })
    }

    @Test
    fun testMedicalFlags() {
        val records = listOf(
            MedicalRecord(category = "CONDITION", title = "Type 2 diabetes", recordDate = "2026-05-10"),
            MedicalRecord(category = "CONDITION", title = "Hypertension", recordDate = "2026-05-10")
        )
        val input = DossierInput(createProfile(), emptyList(), emptyList(), emptyList(), emptyList(), records, todayDate)
        val dossier = HealthDossierBuilder.build(input)
        
        assertTrue(dossier.flags.contains("diabetes"))
        assertTrue(dossier.flags.contains("hypertension"))
    }

    @Test
    fun testEmptyInputGraceful() {
        val input = DossierInput(UserProfile(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val dossier = HealthDossierBuilder.build(input)
        val fullText = dossier.toPromptText(DossierMode.FULL)
        assertTrue(fullText.contains("no sleep records", ignoreCase = true) || fullText.contains("no records", ignoreCase = true))
        assertTrue(fullText.contains("none entered", ignoreCase = true))
    }

    @Test
    fun testFullVsCompactText() {
        val metrics = listOf(createMetric("2026-10-12", sleep = 5.0f)) // Trigger a finding
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val dossier = HealthDossierBuilder.build(input)
        
        val full = dossier.toPromptText(DossierMode.FULL)
        val compact = dossier.toPromptText(DossierMode.COMPACT)
        
        assertTrue(full.contains("FINDINGS"))
        assertTrue(full.length > compact.length)
        assertTrue(compact.contains(dossier.findings.first().title))
    }

    @Test
    fun testPatterns() {
        // Pattern: with 6 paired days (3 short-sleep days with high glucose and 3 good-sleep days with lower glucose)
        val metrics = mutableListOf<DailyMetric>()
        for (i in 1..3) {
            metrics.add(createMetric("2026-10-0$i", sleep = 5.0f, bb = 9.0f))
        }
        for (i in 4..6) {
            metrics.add(createMetric("2026-10-0$i", sleep = 8.0f, bb = 6.0f))
        }
        val input = DossierInput(createProfile(), metrics, emptyList(), emptyList(), emptyList(), emptyList(), todayDate)
        val dossier = HealthDossierBuilder.build(input)
        
        assertEquals(1, dossier.patterns.size)
        assertTrue(dossier.patterns.first().contains("not proof of cause"))
    }
}
