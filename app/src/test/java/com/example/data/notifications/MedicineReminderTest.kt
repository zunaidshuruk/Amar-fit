package com.example.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicineReminderTest {
    private fun med(id: String, name: String = "Metformin", dose: String = "500 mg", minute: Int = 8 * 60, days: Int = Days.ALL, enabled: Boolean = true) =
        MedicineReminder(id, name, dose, minute, days, enabled)

    @Test
    fun thereAreNoMedicineRemindersUntilTheUserAddsSome() {
        assertTrue(NotificationPrefs().medicines.isEmpty())
        assertTrue(NotificationPrefs().medicineDiscreet)
        assertTrue(ReminderCatalog.build(NotificationPrefs()).none { it.kind == ReminderKind.MEDICINE })
    }

    @Test
    fun theLimitIsSix() {
        assertEquals(6, MedicineReminders.MAX)
        var list = emptyList<MedicineReminder>()
        for (i in 1..6) {
            assertTrue(MedicineReminders.canAdd(list))
            list = MedicineReminders.upsert(list, med("id$i", name = "Medicine $i"))
        }
        assertEquals(6, list.size)
        assertFalse(MedicineReminders.canAdd(list))
        // a seventh is refused and the list stays exactly as it was
        assertEquals(list, MedicineReminders.upsert(list, med("id7", name = "Medicine 7")))
    }

    @Test
    fun editingReplacesTheSameIdEvenWhenTheListIsFull() {
        var list = (1..6).map { med("id$it", name = "Medicine $it") }
        list = MedicineReminders.upsert(list, med("id3", name = "Insulin", dose = "10 units", minute = 21 * 60))
        assertEquals(6, list.size)
        assertEquals("Insulin", list[2].name)
        assertEquals(21 * 60, list[2].minuteOfDay)
    }

    @Test
    fun aReminderWithoutANameIsNotSaved() {
        val list = listOf(med("a"))
        assertEquals(list, MedicineReminders.upsert(list, med("b", name = "   ")))
    }

    @Test
    fun removingFreesASlot() {
        val list = (1..6).map { med("id$it") }
        val smaller = MedicineReminders.remove(list, "id2")
        assertEquals(5, smaller.size)
        assertTrue(MedicineReminders.canAdd(smaller))
        assertEquals(list.map { it.id }.filter { it != "id2" }, smaller.map { it.id })
    }

    @Test
    fun textIsTrimmedAndCapped() {
        val cleaned = MedicineReminders.clean(med("a", name = "  " + "x".repeat(80) + "  ", dose = " 5 mg "))
        assertEquals(40, cleaned.name.length)
        assertEquals("5 mg", cleaned.dose)
    }

    @Test
    fun timeAndDaysCanNeverBeInvalid() {
        assertEquals(24 * 60 - 1, MedicineReminders.clean(med("a", minute = 99999)).minuteOfDay)
        assertEquals(0, MedicineReminders.clean(med("a", minute = -5)).minuteOfDay)
        assertEquals(Days.ALL, MedicineReminders.clean(med("a", days = 0)).days) // no day chosen means every day
        assertEquals(Days.MON or Days.THU, MedicineReminders.clean(med("a", days = Days.MON or Days.THU)).days)
    }

    @Test
    fun theTextSaysWhatToTake() {
        assertEquals("Take Metformin (500 mg)", MedicineReminders.describe(med("a")))
        assertEquals("Take Vitamin D", MedicineReminders.describe(med("a", name = "Vitamin D", dose = "")))
    }

    @Test
    fun eachReminderBecomesOneAlarmAtItsOwnTimeAndDays() {
        val prefs = NotificationPrefs(
            medicines = listOf(
                med("a", name = "Metformin", minute = 8 * 60),
                med("b", name = "Vitamin D", dose = "", minute = 13 * 60, days = Days.MON or Days.WED),
                med("c", name = "Aspirin", enabled = false)
            )
        )
        val specs = ReminderCatalog.build(prefs).filter { it.kind == ReminderKind.MEDICINE }
        assertEquals(listOf("medicine_0", "medicine_1"), specs.map { it.id })
        assertEquals(listOf(8 * 60, 13 * 60), specs.map { it.minuteOfDay })
        assertEquals(Days.MON or Days.WED, specs[1].days)
        assertEquals("Time for your medicine", specs[0].title)
        assertEquals("Take Metformin (500 mg)", specs[0].message)
        assertEquals("Take Vitamin D", specs[1].message)
        assertTrue(specs.all { it.channel == Channels.MEDICINE })
    }

    @Test
    fun allSixCanBeArmedWithTheirOwnCancellableCodes() {
        val prefs = NotificationPrefs(medicines = (1..6).map { med("id$it", name = "M$it") }, waterReminders = WaterReminderPref(true))
        val specs = ReminderCatalog.build(prefs)
        assertEquals(6, specs.count { it.kind == ReminderKind.MEDICINE })
        val codes = specs.map { it.requestCode }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(ReminderCatalog.allRequestCodes().containsAll(codes))
        assertEquals(ReminderCatalog.allRequestCodes().size, ReminderCatalog.allRequestCodes().toSet().size)
    }

    @Test
    fun onlyTheFirstSixAreEverArmedEvenIfMoreAreStored() {
        val prefs = NotificationPrefs(medicines = (1..9).map { med("id$it", name = "M$it") })
        assertEquals(6, ReminderCatalog.build(prefs).count { it.kind == ReminderKind.MEDICINE })
    }

    @Test
    fun medicineRemindersAreNotSilencedByQuietHoursButByTheMasterSwitch() {
        val prefs = NotificationPrefs(medicines = listOf(med("a")), quietStartMinute = 22 * 60, quietEndMinute = 9 * 60)
        val spec = ReminderCatalog.build(prefs).single { it.kind == ReminderKind.MEDICINE }
        assertTrue("a dose at 8 AM inside quiet hours must still be shown", ReminderDecider.shouldShow(spec, TodaySnapshot(), prefs, 8 * 60))
        assertFalse(ReminderDecider.shouldShow(spec, TodaySnapshot(), prefs.copy(masterEnabled = false), 8 * 60))
        // other reminders are still silenced
        val water = ReminderCatalog.build(NotificationPrefs(waterReminders = WaterReminderPref(true), quietStartMinute = 22 * 60, quietEndMinute = 9 * 60))
            .first { it.kind == ReminderKind.WATER }
        assertFalse(ReminderDecider.shouldShow(water, TodaySnapshot(), NotificationPrefs(quietStartMinute = 22 * 60, quietEndMinute = 9 * 60), 8 * 60))
    }
}
