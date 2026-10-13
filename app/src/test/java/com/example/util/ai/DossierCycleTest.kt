package com.example.util.ai

import com.example.data.local.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DossierCycleTest {

    private fun input(summary: String?) = DossierInput(
        profile = UserProfile(name = "A", gender = "Female"),
        metrics = emptyList(),
        foodLogs = emptyList(),
        exerciseSessions = emptyList(),
        activityEvents = emptyList(),
        medicalRecords = emptyList(),
        todayDate = "2026-10-13",
        cycleSummary = summary
    )

    private val summary = "Cycle tracker (estimates from the user's own logs): day 13 of a typical 28-day cycle, fertile window; next period expected about Oct 29 (in 16 days); cycle lengths are fairly regular."

    @Test
    fun theCycleLineIsInBothVersionsOfTheDossierWhenGiven() {
        val dossier = HealthDossierBuilder.build(input(summary))
        assertTrue(dossier.toPromptText(DossierMode.FULL).contains("CYCLE: $summary"))
        assertTrue(dossier.toPromptText(DossierMode.COMPACT).contains("CYCLE: $summary"))
    }

    @Test
    fun withoutASummaryTheDossierSaysNothingAboutTheCycle() {
        val dossier = HealthDossierBuilder.build(input(null))
        assertFalse(dossier.toPromptText(DossierMode.FULL).contains("CYCLE"))
        assertFalse(dossier.toPromptText(DossierMode.COMPACT).contains("CYCLE"))
    }
}
