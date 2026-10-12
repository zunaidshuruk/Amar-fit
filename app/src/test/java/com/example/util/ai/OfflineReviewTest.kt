package com.example.util.ai

import com.example.data.local.CoachTopic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineReviewTest {

    @Test
    fun testEmptyDossierBuild() {
        val dossier = Dossier(
            todayDate = "2026-10-12",
            profileLine = "User age 30",
            sections = emptyList(),
            findings = emptyList(),
            patterns = emptyList(),
            medicalLines = emptyList(),
            flags = emptySet()
        )
        val review = OfflineReview.build(dossier, emptyList())

        assertTrue(review.contains("## Offline health review (computed on your phone)"))
        assertTrue(review.contains("*Your last 30 days, from your own records."))
        assertTrue(review.contains("## Snapshot"))
        assertTrue(review.contains("- No records in the last 30 days yet."))
        assertTrue(review.contains("## What your data shows"))
        assertTrue(review.contains("- Nothing stands out yet; keep logging."))
        assertFalse(review.contains("## Patterns"))
        assertFalse(review.contains("## Support from the KardIQ protocol"))
    }

    @Test
    fun testDossierWithFindingsAndSeverity() {
        val findings = listOf(
            Finding(
                topic = FindingTopic.GLUCOSE,
                severity = 3,
                tag = "high_glucose",
                title = "Fasting glucose elevated",
                evidence = "Average 140 mg/dL"
            ),
            Finding(
                topic = FindingTopic.SLEEP,
                severity = 2,
                tag = "short_sleep",
                title = "Sleep duration below target",
                evidence = "Average 5.5 hours"
            ),
            Finding(
                topic = FindingTopic.ACTIVITY,
                severity = 1,
                tag = "goal_met_steps",
                title = "Daily step goal met",
                evidence = "Average 8,500 steps"
            ),
            Finding(
                topic = FindingTopic.HYDRATION,
                severity = 1,
                tag = "water_intake",
                title = "Water intake tracked",
                evidence = "Average 2.1 L"
            )
        )
        val dossier = Dossier(
            todayDate = "2026-10-12",
            profileLine = "User age 30",
            sections = listOf(
                DossierSection("SLEEP & RECOVERY", listOf("Sleep 7-day avg: 5.5 h", "Resting HR avg: 65 bpm")),
                DossierSection("PROFILE", listOf("Age 30"))
            ),
            findings = findings,
            patterns = listOf("Sleep drops on weekdays", "Steps higher on weekends"),
            medicalLines = emptyList(),
            flags = emptySet()
        )

        val topics = listOf(
            CoachTopic("Insomnia (Middle of Night Waking)", "4-7-8 Breathing Technique", "Lowers heart rate")
        )

        val review = OfflineReview.build(dossier, topics)

        assertTrue(review.contains("- **Important:** Fasting glucose elevated. Average 140 mg/dL"))
        assertTrue(review.contains("- **Attention:** Sleep duration below target. Average 5.5 hours"))
        assertTrue(review.contains("- **Good news:** Daily step goal met. Average 8,500 steps"))
        assertTrue(review.contains("- **Info:** Water intake tracked. Average 2.1 L"))

        assertTrue(review.contains("## Patterns"))
        assertTrue(review.contains("- Sleep drops on weekdays"))
        assertTrue(review.contains("- Steps higher on weekends"))

        assertTrue(review.contains("## Support from the KardIQ protocol"))
        assertTrue(review.contains("Insomnia (Middle of Night Waking): 4-7-8 Breathing Technique"))

        // Severity 3 finding forces doctor note
        assertTrue(review.contains(MedicalNotes.DOCTOR_LINE))
    }
}
