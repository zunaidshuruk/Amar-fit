package com.example.util.ai

import com.example.data.local.CoachTopic
import com.example.data.local.coachTopics
import org.junit.Assert.assertEquals
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
        assertTrue(review.contains("## Track and discuss"))
        assertTrue(review.contains("- Re-check these numbers again in a week."))
    }

    @Test
    fun testFindingSpecificProtocolMatching() {
        val sleepFinding = Finding(
            topic = FindingTopic.SLEEP,
            severity = 2,
            tag = "short_sleep",
            title = "Sleep averaged 5.0 h against your 8.0 h goal",
            evidence = "5.0 h avg"
        )
        val dossier = Dossier(
            todayDate = "2026-10-12",
            profileLine = "User age 30",
            sections = listOf(DossierSection("SLEEP", listOf("Sleep 5.0 h"))),
            findings = listOf(sleepFinding),
            patterns = emptyList(),
            medicalLines = emptyList(),
            flags = emptySet()
        )

        val customTopics = listOf(
            CoachTopic("Sleep Initiation", "The Power Down Hour: 20m tasks, 20m hygiene", "Shifts body to relaxation"),
            CoachTopic("Gut Health", "Fermented Foods", "Supports gut flora"),
            CoachTopic("Metabolic Health", "Walk After Meals", "Blunts glucose spikes"),
            CoachTopic("Hormones", "Morning Sunlight", "Sets circadian clock"),
            CoachTopic("Heart", "Cacao Tonic", "Provides flavanols")
        )

        val review = OfflineReview.build(dossier, customTopics)

        assertTrue(review.contains("## Support from the KardIQ protocol"))
        assertTrue(review.contains("The Power Down Hour"))
        assertFalse(review.contains("Fermented Foods"))
    }

    @Test
    fun testMaxThreeSupportEntries() {
        val sleepFinding = Finding(
            topic = FindingTopic.SLEEP,
            severity = 2,
            tag = "short_sleep",
            title = "Sleep averaged 5.0 h against your 8.0 h goal",
            evidence = "5.0 h avg"
        )
        val dossier = Dossier(
            todayDate = "2026-10-12",
            profileLine = "User age 30",
            sections = listOf(DossierSection("SLEEP", listOf("Sleep 5.0 h"))),
            findings = listOf(sleepFinding),
            patterns = emptyList(),
            medicalLines = emptyList(),
            flags = emptySet()
        )

        val review = OfflineReview.build(dossier, coachTopics)

        val occurrences = review.split("(for:").size - 1
        assertTrue(occurrences <= 3)
    }

    @Test
    fun testSeverityThreeYieldsDiscussWithDoctorAndDoctorNote() {
        val sev3Finding = Finding(
            topic = FindingTopic.GLUCOSE,
            severity = 3,
            tag = "high_glucose",
            title = "Fasting glucose elevated",
            evidence = "140 mg/dL"
        )
        val dossier = Dossier(
            todayDate = "2026-10-12",
            profileLine = "User age 30",
            sections = listOf(DossierSection("GLUCOSE", listOf("Glucose 140 mg/dL"))),
            findings = listOf(sev3Finding),
            patterns = emptyList(),
            medicalLines = emptyList(),
            flags = emptySet()
        )

        val review = OfflineReview.build(dossier, coachTopics)

        assertTrue(review.contains("- Discuss with your doctor: Fasting glucose elevated"))
        assertTrue(review.contains(MedicalNotes.DOCTOR_LINE))
    }

    @Test
    fun testDataFindingYieldsAddReadings() {
        val dataFinding = Finding(
            topic = FindingTopic.DATA,
            severity = 1,
            tag = "missing_bp",
            title = "No blood pressure readings in 30 days",
            evidence = "0 readings"
        )
        val dossier = Dossier(
            todayDate = "2026-10-12",
            profileLine = "User age 30",
            sections = emptyList(),
            findings = listOf(dataFinding),
            patterns = emptyList(),
            medicalLines = emptyList(),
            flags = emptySet()
        )

        val review = OfflineReview.build(dossier, emptyList())

        assertTrue(review.contains("- Add readings: No blood pressure readings in 30 days"))
    }

    @Test
    fun testSleepOnlyDossierNoFlagsNoDoctorNote() {
        val sleepFinding = Finding(
            topic = FindingTopic.SLEEP,
            severity = 2,
            tag = "short_sleep",
            title = "Sleep duration 5.5 h",
            evidence = "5.5 h"
        )
        val dossier = Dossier(
            todayDate = "2026-10-12",
            profileLine = "User age 30",
            sections = listOf(DossierSection("SLEEP", listOf("Sleep 5.5 h"))),
            findings = listOf(sleepFinding),
            patterns = emptyList(),
            medicalLines = emptyList(),
            flags = emptySet()
        )

        val safeTopics = listOf(
            CoachTopic("Sleep Initiation", "The Power Down Hour", "Shifts body to relaxation")
        )

        val review = OfflineReview.build(dossier, safeTopics)

        assertFalse(review.contains(MedicalNotes.DOCTOR_LINE))
    }

    @Test
    fun testDiabetesFlagYieldsDoctorNote() {
        val sleepFinding = Finding(
            topic = FindingTopic.SLEEP,
            severity = 2,
            tag = "short_sleep",
            title = "Sleep duration 5.5 h",
            evidence = "5.5 h"
        )
        val dossier = Dossier(
            todayDate = "2026-10-12",
            profileLine = "User age 30",
            sections = listOf(DossierSection("SLEEP", listOf("Sleep 5.5 h"))),
            findings = listOf(sleepFinding),
            patterns = emptyList(),
            medicalLines = emptyList(),
            flags = setOf("diabetes")
        )

        val review = OfflineReview.build(dossier, coachTopics)

        assertTrue(review.contains(MedicalNotes.DOCTOR_LINE))
    }

    @Test
    fun testGeneralInvariants() {
        val dossier = Dossier(
            todayDate = "2026-10-12",
            profileLine = "User age 30",
            sections = emptyList(),
            findings = emptyList(),
            patterns = emptyList(),
            medicalLines = emptyList(),
            flags = emptySet()
        )

        val review = OfflineReview.build(dossier, coachTopics)

        assertTrue(review.contains("## Track and discuss"))
        assertTrue(review.contains("- Re-check these numbers again in a week."))
        assertFalse(review.contains("Gemini"))
    }
}
