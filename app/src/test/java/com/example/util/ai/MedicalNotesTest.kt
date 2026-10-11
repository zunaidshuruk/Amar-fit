package com.example.util.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicalNotesTest {

    @Test
    fun testNeedsDoctorNote() {
        assertFalse(MedicalNotes.needsDoctorNote("I had breakfast"))
        assertFalse(MedicalNotes.needsDoctorNote("avoid fast food"))
        assertTrue(MedicalNotes.needsDoctorNote("try a 24 hour fast"))
        assertTrue(MedicalNotes.needsDoctorNote("apple cider vinegar tonic"))
        assertTrue(MedicalNotes.needsDoctorNote("type 2 diabetes friendly drink"))
    }

    @Test
    fun testAppendDoctorNote() {
        val answer = MedicalNotes.appendDoctorNote("Try a 24 hour fast.")
        assertTrue(answer.endsWith("*${MedicalNotes.DOCTOR_LINE}*"))

        val answer2 = MedicalNotes.appendDoctorNote(answer)
        assertEquals(answer, answer2)

        val recipeText = "Golden milk recipe:\n- Turmeric\n- Milk\n\nYOUTUBE_SEARCH: Golden Milk"
        val withDoctorNote = MedicalNotes.appendDoctorNote(recipeText, userText = "turmeric and fasting", beforeFinalMarker = true)
        val expectedSuffix = "*${MedicalNotes.DOCTOR_LINE}*\n\nYOUTUBE_SEARCH: Golden Milk"
        assertTrue(withDoctorNote.endsWith(expectedSuffix))

        val errText = MedicalNotes.appendDoctorNote("Error: timeout", force = true)
        assertEquals("Error: timeout", errText)

        val ordinary = "Drink some water."
        val forced = MedicalNotes.appendDoctorNote(ordinary, force = true)
        assertTrue(forced.endsWith("*${MedicalNotes.DOCTOR_LINE}*"))
    }
}
