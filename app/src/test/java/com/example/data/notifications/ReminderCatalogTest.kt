package com.example.data.notifications

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderCatalogTest {

    private val everythingOn = NotificationPrefs(
        waterReminders = WaterReminderPref(true),
        workoutReminder = ReminderPref(true, 18 * 60, Days.WEEKDAYS),
        bedtimeReminder = ReminderPref(true, 22 * 60),
        mindfulMoment = ReminderPref(true, 7 * 60 + 30),
        weeklyCheckIn = ReminderPref(true, 19 * 60, Days.SUN),
        glucoseRemindersEnabled = true,
        weighIn = ReminderPref(true, 8 * 60, Days.MON)
    )

    @Test
    fun defaultsGiveExactlyTheThreeMealReminders() {
        val specs = ReminderCatalog.build(NotificationPrefs())
        assertEquals(listOf("meal_breakfast", "meal_lunch", "meal_dinner"), specs.map { it.id })
        assertEquals(listOf(8 * 60, 13 * 60 + 30, 20 * 60), specs.map { it.minuteOfDay })
        assertTrue(specs.all { it.route == "scanner" && it.channel == Channels.MEALS })
    }

    @Test
    fun theMealSwitchTurnsAllThreeOff() {
        assertTrue(ReminderCatalog.build(NotificationPrefs(mealRemindersEnabled = false)).isEmpty())
    }

    @Test
    fun oneMealCanBeSwitchedOff() {
        val specs = ReminderCatalog.build(NotificationPrefs(lunch = ReminderPref(false, 13 * 60 + 30)))
        assertEquals(listOf("meal_breakfast", "meal_dinner"), specs.map { it.id })
    }

    @Test
    fun waterRemindersRunEveryTwoHoursByDefault() {
        assertEquals(listOf(540, 660, 780, 900, 1020, 1140, 1260), ReminderCatalog.waterTimes(WaterReminderPref(true)))
        assertEquals(listOf(540, 720, 900, 1080, 1260), ReminderCatalog.waterTimes(WaterReminderPref(true, everyHours = 3)))
        // out of range intervals are kept between one and six hours
        assertEquals(ReminderCatalog.waterTimes(WaterReminderPref(true, everyHours = 1)).size, 13)
        assertEquals(ReminderCatalog.waterTimes(WaterReminderPref(true, everyHours = 99)), listOf(540, 900, 1260))
    }

    @Test
    fun everyReminderHasItsOwnRequestCodeAndIsCancellable() {
        val specs = ReminderCatalog.build(everythingOn)
        val codes = specs.map { it.requestCode }
        assertEquals("request codes must be unique", codes.size, codes.toSet().size)
        assertTrue(ReminderCatalog.allRequestCodes().containsAll(codes))
        assertEquals(ReminderCatalog.allRequestCodes().size, ReminderCatalog.allRequestCodes().toSet().size)
        assertEquals(3 + 7 + 1 + 1 + 1 + 1 + 6 + 1, specs.size)
    }

    @Test
    fun glucoseSlotsNeedTheGroupSwitchAndTheirOwnSwitch() {
        assertTrue(ReminderCatalog.build(NotificationPrefs(glucoseRemindersEnabled = false)).none { it.kind == ReminderKind.GLUCOSE })
        val oneOff = NotificationPrefs(
            glucoseRemindersEnabled = true,
            glucose = NotificationPrefs.defaultGlucoseSlots().mapIndexed { i, p -> if (i == 1) p.copy(enabled = false) else p }
        )
        assertEquals(listOf("glucose_0", "glucose_2", "glucose_3", "glucose_4", "glucose_5"),
            ReminderCatalog.build(oneOff).filter { it.kind == ReminderKind.GLUCOSE }.map { it.id })
    }

    @Test
    fun routesPointAtExistingScreens() {
        val routes = ReminderCatalog.build(everythingOn).map { it.route }.toSet()
        assertEquals(setOf("scanner", "today", "fitness", "sleep", "mindfulness_timer", "health", "glucoselog", "weightlog"), routes)
    }

    // ---- next trigger ----
    private val zone = ZoneId.of("Asia/Dhaka")
    private fun millis(y: Int, m: Int, d: Int, h: Int, min: Int) =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun nextTriggerIsLaterTodayWhenTheTimeHasNotPassed() {
        // Wednesday 2026-10-14 07:00, reminder at 08:00 every day
        val now = millis(2026, 10, 14, 7, 0)
        assertEquals(millis(2026, 10, 14, 8, 0), ReminderPlanner.nextTrigger(8 * 60, Days.ALL, now, zone))
    }

    @Test
    fun nextTriggerIsTomorrowWhenTheTimeHasPassed() {
        val now = millis(2026, 10, 14, 9, 0)
        assertEquals(millis(2026, 10, 15, 8, 0), ReminderPlanner.nextTrigger(8 * 60, Days.ALL, now, zone))
    }

    @Test
    fun nextTriggerHonoursTheChosenDays() {
        // Wednesday 2026-10-14 10:00, Sunday-only reminder at 19:00 -> Sunday 2026-10-18
        val now = millis(2026, 10, 14, 10, 0)
        assertEquals(millis(2026, 10, 18, 19, 0), ReminderPlanner.nextTrigger(19 * 60, Days.SUN, now, zone))
        // Monday-Friday workout at 18:00 asked on Friday evening after 18:00 -> Monday
        val fri = millis(2026, 10, 16, 19, 0)
        assertEquals(millis(2026, 10, 19, 18, 0), ReminderPlanner.nextTrigger(18 * 60, Days.WEEKDAYS, fri, zone))
    }

    @Test
    fun nextTriggerIsNullWhenNoDayIsChosen() {
        assertNull(ReminderPlanner.nextTrigger(8 * 60, 0, millis(2026, 10, 14, 10, 0), zone))
    }

    @Test
    fun aTriggerExactlyNowIsNotRepeated() {
        val now = millis(2026, 10, 14, 8, 0)
        assertEquals(millis(2026, 10, 15, 8, 0), ReminderPlanner.nextTrigger(8 * 60, Days.ALL, now, zone))
    }
}
