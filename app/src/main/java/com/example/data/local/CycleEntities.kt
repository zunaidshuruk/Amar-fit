package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One period. [endDate] is empty while it is still going or the end was not logged.
 * A deleted period is only marked [removed] (and kept for 30 days) so that the deletion reaches the user's other phones.
 * Firestore-mapped, so every field has a default and there is a no-argument constructor.
 */
@Entity(tableName = "period_logs")
data class PeriodLog(
    @PrimaryKey val cloudId: String = UUID.randomUUID().toString(),
    val startDate: String = "",
    val endDate: String = "",
    val removed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this(UUID.randomUUID().toString(), "", "", false, System.currentTimeMillis())
}

/** What was logged on one day: flow (FlowLevel code), symptoms (bit mask) and mood (Mood code). One row per date. */
@Entity(tableName = "cycle_days")
data class CycleDayLog(
    @PrimaryKey val date: String = "",
    val flow: Int = 0,
    val symptoms: Int = 0,
    val mood: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", 0, 0, 0, System.currentTimeMillis())
}
