package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MedicalCategory(val categoryKey: String, val displayName: String) {
    ALLERGY("ALLERGY", "Allergies"),
    CONDITION("CONDITION", "Conditions"),
    MEDICATION("MEDICATION", "Medications"),
    VACCINE("VACCINE", "Vaccines"),
    PREGNANCY("PREGNANCY", "Pregnancy"),
    SOCIAL_HISTORY("SOCIAL_HISTORY", "Social history"),
    PROCEDURE("PROCEDURE", "Procedures"),
    VISIT("VISIT", "Visits"),
    LAB_RESULT("LAB_RESULT", "Lab results");

    companion object {
        fun fromKey(key: String): MedicalCategory? = entries.firstOrNull { it.categoryKey.equals(key, ignoreCase = true) }
    }
}

@Entity(tableName = "medical_records")
data class MedicalRecord(
    @PrimaryKey val cloudId: String = UUID.randomUUID().toString(),
    val category: String = "",
    val title: String = "",
    val details: String = "",
    val recordDate: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(UUID.randomUUID().toString(), "", "", "", "", System.currentTimeMillis())
}
