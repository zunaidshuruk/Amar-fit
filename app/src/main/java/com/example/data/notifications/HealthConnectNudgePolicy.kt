package com.example.data.notifications

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * When to ask the person to connect Health Connect: once on the first login, then again every [INTERVAL_DAYS] days (counted in calendar days)
 * for as long as it is not connected. It never nags when Health Connect cannot work on the phone, and stops by itself once connected.
 */
object HealthConnectNudgePolicy {
    const val INTERVAL_DAYS = 3
    const val TITLE = "Connect Health Connect"
    const val MESSAGE = "Bring in your steps, sleep and heart rate for more accurate tracking. It takes a minute."

    fun shouldNudge(enabled: Boolean, available: Boolean, connected: Boolean, lastNudge: LocalDate?, today: LocalDate): Boolean {
        if (!enabled || !available || connected) return false
        if (lastNudge == null) return true
        return ChronoUnit.DAYS.between(lastNudge, today) >= INTERVAL_DAYS
    }
}
