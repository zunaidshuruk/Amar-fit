package com.example.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleReminderTest {

    @Test
    fun cycleRemindersAreOffByDefault() {
        val specs = ReminderCatalog.build(NotificationPrefs())
        assertTrue(specs.none { it.kind == ReminderKind.CYCLE || it.kind == ReminderKind.CYCLE_LOG })
        val p = NotificationPrefs()
        assertFalse(p.cyclePeriodSoon)
        assertFalse(p.cyclePeriodLate)
        assertFalse(p.cycleFertileWindow)
        assertFalse(p.cycleLogReminder.enabled)
        assertEquals(2, p.cyclePeriodSoonDays)
        assertEquals(3, p.cyclePeriodLateDays)
        assertEquals(9 * 60, p.cycleCheckMinute)
    }

    @Test
    fun anyCycleNoticeSwitchArmsOneDailyCheck() {
        for (p in listOf(
            NotificationPrefs(cyclePeriodSoon = true),
            NotificationPrefs(cyclePeriodLate = true),
            NotificationPrefs(cycleFertileWindow = true),
            NotificationPrefs(cyclePeriodSoon = true, cyclePeriodLate = true, cycleFertileWindow = true)
        )) {
            val checks = ReminderCatalog.build(p).filter { it.kind == ReminderKind.CYCLE }
            assertEquals(1, checks.size)
            assertEquals("cycle_check", checks[0].id)
            assertEquals(Days.ALL, checks[0].days)
            assertEquals("cycle", checks[0].route)
            assertEquals(Channels.CYCLE, checks[0].channel)
        }
    }

    @Test
    fun theDailyCheckRunsAtTheChosenTime() {
        val spec = ReminderCatalog.build(NotificationPrefs(cyclePeriodSoon = true, cycleCheckMinute = 7 * 60 + 30)).first { it.kind == ReminderKind.CYCLE }
        assertEquals(7 * 60 + 30, spec.minuteOfDay)
    }

    @Test
    fun theLogReminderIsItsOwnSwitchAndTime() {
        val specs = ReminderCatalog.build(NotificationPrefs(cycleLogReminder = ReminderPref(true, 21 * 60)))
        val log = specs.single { it.kind == ReminderKind.CYCLE_LOG }
        assertEquals("cycle_log", log.id)
        assertEquals(21 * 60, log.minuteOfDay)
        assertTrue(specs.none { it.kind == ReminderKind.CYCLE })
    }

    @Test
    fun cycleAlarmsHaveTheirOwnCancellableRequestCodes() {
        val specs = ReminderCatalog.build(
            NotificationPrefs(cyclePeriodSoon = true, cycleLogReminder = ReminderPref(true, 20 * 60), waterReminders = WaterReminderPref(true))
        )
        val codes = specs.map { it.requestCode }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(ReminderCatalog.allRequestCodes().containsAll(codes))
        assertEquals(ReminderCatalog.allRequestCodes().size, ReminderCatalog.allRequestCodes().toSet().size)
    }

    @Test
    fun theLogReminderIsSkippedWhenTodayIsAlreadyLogged() {
        val spec = ReminderCatalog.build(NotificationPrefs(cycleLogReminder = ReminderPref(true, 20 * 60))).single { it.kind == ReminderKind.CYCLE_LOG }
        val prefs = NotificationPrefs()
        assertTrue(ReminderDecider.shouldShow(spec, TodaySnapshot(cycleLoggedToday = false), prefs, 20 * 60))
        assertFalse(ReminderDecider.shouldShow(spec, TodaySnapshot(cycleLoggedToday = true), prefs, 20 * 60))
    }

    @Test
    fun theDailyCheckRespectsQuietHoursAndTheMasterSwitch() {
        val spec = ReminderCatalog.build(NotificationPrefs(cyclePeriodSoon = true)).single { it.kind == ReminderKind.CYCLE }
        assertTrue(ReminderDecider.shouldShow(spec, TodaySnapshot(), NotificationPrefs(), 9 * 60))
        assertFalse(ReminderDecider.shouldShow(spec, TodaySnapshot(), NotificationPrefs(masterEnabled = false), 9 * 60))
        assertFalse(ReminderDecider.shouldShow(spec, TodaySnapshot(), NotificationPrefs(quietStartMinute = 22 * 60, quietEndMinute = 10 * 60), 9 * 60))
    }

    @Test
    fun theFastingMakeUpReminderIsOffByDefaultAndHasItsOwnCode() {
        assertTrue(ReminderCatalog.build(NotificationPrefs()).none { it.kind == ReminderKind.FASTING })
        val spec = ReminderCatalog.build(NotificationPrefs(fastingMakeUp = ReminderPref(true, 20 * 60, Days.SUN or Days.WED))).single { it.kind == ReminderKind.FASTING }
        assertEquals("fasting_makeup", spec.id)
        assertEquals(Days.SUN or Days.WED, spec.days)
        assertEquals("cycle", spec.route)
        assertEquals(Channels.CYCLE, spec.channel)
        val codes = ReminderCatalog.build(NotificationPrefs(fastingMakeUp = ReminderPref(true), cyclePeriodSoon = true, cycleLogReminder = ReminderPref(true))).map { it.requestCode }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(ReminderCatalog.allRequestCodes().containsAll(codes))
    }

    @Test
    fun theFastingReminderObeysQuietHoursAndTheMasterSwitch() {
        val spec = ReminderCatalog.build(NotificationPrefs(fastingMakeUp = ReminderPref(true))).single { it.kind == ReminderKind.FASTING }
        assertTrue(ReminderDecider.shouldShow(spec, TodaySnapshot(), NotificationPrefs(), 20 * 60))
        assertFalse(ReminderDecider.shouldShow(spec, TodaySnapshot(), NotificationPrefs(masterEnabled = false), 20 * 60))
        assertFalse(ReminderDecider.shouldShow(spec, TodaySnapshot(), NotificationPrefs(quietStartMinute = 19 * 60, quietEndMinute = 7 * 60), 20 * 60))
    }
}
