package com.example.data.notifications

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectNudgeTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun nudge(
        enabled: Boolean = true, available: Boolean = true, connected: Boolean = false,
        last: String? = null, today: String = "2026-10-14"
    ) = HealthConnectNudgePolicy.shouldNudge(enabled, available, connected, last?.let { d(it) }, d(today))

    @Test
    fun theFirstLoginIsNudgedImmediately() {
        assertTrue(nudge(last = null))
    }

    @Test
    fun afterwardsItWaitsThreeCalendarDays() {
        assertFalse(nudge(last = "2026-10-14", today = "2026-10-14"))
        assertFalse(nudge(last = "2026-10-14", today = "2026-10-15"))
        assertFalse(nudge(last = "2026-10-14", today = "2026-10-16"))
        assertTrue(nudge(last = "2026-10-14", today = "2026-10-17"))
        assertTrue(nudge(last = "2026-10-14", today = "2026-10-25"))
    }

    @Test
    fun itStopsOnceConnected() {
        assertFalse(nudge(connected = true, last = null))
        assertFalse(nudge(connected = true, last = "2026-09-01"))
    }

    @Test
    fun itNeverNagsWhenHealthConnectCannotWorkOrTheSwitchIsOff() {
        assertFalse(nudge(available = false, last = null))
        assertFalse(nudge(enabled = false, last = null))
        assertFalse(nudge(enabled = false, last = "2026-01-01"))
    }

    @Test
    fun theIntervalIsThreeDays() {
        assertEquals(3, HealthConnectNudgePolicy.INTERVAL_DAYS)
    }

    @Test
    fun theNudgeIsOnByDefaultAndHasOneDailyCheck() {
        assertTrue(NotificationPrefs().healthConnectNudge)
        val spec = ReminderCatalog.build(NotificationPrefs()).single { it.kind == ReminderKind.HEALTH_CONNECT }
        assertEquals(10 * 60, spec.minuteOfDay)
        assertEquals(Days.ALL, spec.days)
        assertEquals("settings", spec.route)
        assertTrue(ReminderCatalog.build(NotificationPrefs(healthConnectNudge = false)).none { it.kind == ReminderKind.HEALTH_CONNECT })
    }

    @Test
    fun theNudgeObeysQuietHoursAndTheMasterSwitch() {
        val spec = ReminderCatalog.build(NotificationPrefs()).single { it.kind == ReminderKind.HEALTH_CONNECT }
        assertTrue(ReminderDecider.shouldShow(spec, TodaySnapshot(), NotificationPrefs(), 10 * 60))
        assertFalse(ReminderDecider.shouldShow(spec, TodaySnapshot(), NotificationPrefs(masterEnabled = false), 10 * 60))
        assertFalse(ReminderDecider.shouldShow(spec, TodaySnapshot(), NotificationPrefs(quietStartMinute = 22 * 60, quietEndMinute = 11 * 60), 10 * 60))
    }
}
