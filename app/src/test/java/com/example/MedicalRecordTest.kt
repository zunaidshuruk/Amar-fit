package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.local.MedicalCategory
import com.example.data.local.MedicalRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Constructor

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class MedicalRecordTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testZeroArgConstructorAvailability() {
        val constructor: Constructor<MedicalRecord> = MedicalRecord::class.java.getDeclaredConstructor()
        assertNotNull(constructor)
        val defaultRecord = constructor.newInstance()
        assertEquals("", defaultRecord.title)
        assertEquals("", defaultRecord.category)
        assertEquals("", defaultRecord.details)
        assertEquals("", defaultRecord.recordDate)
        assertTrue(defaultRecord.cloudId.isNotEmpty())
    }

    @Test
    fun testMedicalCategoriesOrderAndKeys() {
        val expectedKeys = listOf(
            "ALLERGY", "CONDITION", "MEDICATION", "VACCINE",
            "PREGNANCY", "SOCIAL_HISTORY", "PROCEDURE", "VISIT", "LAB_RESULT"
        )
        val expectedDisplayNames = listOf(
            "Allergies", "Conditions", "Medications", "Vaccines",
            "Pregnancy", "Social history", "Procedures", "Visits", "Lab results"
        )

        val categories = MedicalCategory.entries
        assertEquals(9, categories.size)
        assertEquals(expectedKeys, categories.map { it.categoryKey })
        assertEquals(expectedDisplayNames, categories.map { it.displayName })
    }

    @Test
    fun testMedicalRecordInsertAndRetrieve() = runBlocking {
        val dao = db.medicalRecordDao()

        val record1 = MedicalRecord(
            cloudId = "med-1",
            category = MedicalCategory.ALLERGY.categoryKey,
            title = "Peanuts",
            details = "Anaphylactic reaction",
            recordDate = "2026-05-10"
        )
        val record2 = MedicalRecord(
            cloudId = "med-2",
            category = MedicalCategory.MEDICATION.categoryKey,
            title = "Metformin",
            details = "500mg twice daily with meals",
            recordDate = "2026-01-15"
        )

        dao.insertMedicalRecord(record1)
        dao.insertMedicalRecord(record2)

        val allRecords = dao.getAllMedicalRecords().first()
        assertEquals(2, allRecords.size)

        val allergies = allRecords.filter { it.category == MedicalCategory.ALLERGY.categoryKey }
        assertEquals(1, allergies.size)
        assertEquals("Peanuts", allergies[0].title)
        assertEquals("Anaphylactic reaction", allergies[0].details)

        val medications = allRecords.filter { it.category == MedicalCategory.MEDICATION.categoryKey }
        assertEquals(1, medications.size)
        assertEquals("Metformin", medications[0].title)

        dao.deleteMedicalRecord(record1)
        val remaining = dao.getAllMedicalRecords().first()
        assertEquals(1, remaining.size)
        assertEquals("med-2", remaining[0].cloudId)
    }
}
