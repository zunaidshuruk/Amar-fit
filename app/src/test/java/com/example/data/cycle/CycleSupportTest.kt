package com.example.data.cycle

import com.example.data.notifications.NotificationPrefs
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleSupportTest {

    private fun d(s: String) = LocalDate.parse(s)
    private val log = listOf(PeriodSpan(d("2026-10-01"), d("2026-10-05")))
    private fun predictOn(day: String) = CyclePredictor.predict(log, d(day))

    // ---- access
    @Test
    fun onlyFemaleProfilesCanUseTheTracker() {
        assertTrue(CycleAccess.isAvailable("Female"))
        assertTrue(CycleAccess.isAvailable("  female "))
        assertTrue(CycleAccess.isAvailable("FEMALE"))
        assertFalse(CycleAccess.isAvailable("Male"))
        assertFalse(CycleAccess.isAvailable("Other"))
        assertFalse(CycleAccess.isAvailable(""))
        assertFalse(CycleAccess.isAvailable(null))
    }

    @Test
    fun trackingNeedsTheSwitchAndTheConsent() {
        assertTrue(CycleAccess.isActive("Female", enabled = true, consentAt = 5L))
        assertFalse(CycleAccess.isActive("Female", enabled = false, consentAt = 5L))
        assertFalse(CycleAccess.isActive("Female", enabled = true, consentAt = 0L))
        assertFalse(CycleAccess.isActive("Male", enabled = true, consentAt = 5L))
    }

    // ---- notices
    private val allOn = NotificationPrefs(cyclePeriodSoon = true, cyclePeriodLate = true, cycleFertileWindow = true)

    @Test
    fun nothingIsShownByDefault() {
        assertNull(CycleNotices.forToday(predictOn("2026-10-27"), NotificationPrefs(), d("2026-10-27")))
    }

    @Test
    fun periodSoonFiresOnExactlyTheChosenDay() {
        // next period 2026-10-29, default is 2 days before
        val notice = CycleNotices.forToday(predictOn("2026-10-27"), allOn, d("2026-10-27"))!!
        assertEquals(CycleNoticeKind.PERIOD_SOON, notice.kind)
        assertTrue(notice.message, notice.message.contains("in 2 days"))
        assertTrue(notice.message.contains("estimate"))
        assertNull(CycleNotices.forToday(predictOn("2026-10-26"), allOn, d("2026-10-26")))
        assertNull(CycleNotices.forToday(predictOn("2026-10-28"), allOn, d("2026-10-28")))
    }

    @Test
    fun periodSoonWordsTodayAndTomorrow() {
        val tomorrow = CycleNotices.forToday(predictOn("2026-10-28"), allOn.copy(cyclePeriodSoonDays = 1), d("2026-10-28"))!!
        assertTrue(tomorrow.message.contains("tomorrow"))
        val today = CycleNotices.forToday(predictOn("2026-10-29"), allOn.copy(cyclePeriodSoonDays = 0), d("2026-10-29"))!!
        assertEquals("Period expected today", today.title)
    }

    @Test
    fun aLatePeriodNoticeNeverMentionsPregnancy() {
        val notice = CycleNotices.forToday(predictOn("2026-11-01"), allOn, d("2026-11-01"))!! // 3 days late
        assertEquals(CycleNoticeKind.PERIOD_LATE, notice.kind)
        assertTrue(notice.message.contains("3 days"))
        assertFalse(notice.message.lowercase().contains("pregnan"))
        assertNull(CycleNotices.forToday(predictOn("2026-11-02"), allOn, d("2026-11-02")))
    }

    @Test
    fun fertileWindowNoticeFiresOnTheFirstDayOnly() {
        // fertile window 2026-10-10 .. 2026-10-16
        val notice = CycleNotices.forToday(predictOn("2026-10-10"), allOn, d("2026-10-10"))!!
        assertEquals(CycleNoticeKind.FERTILE_START, notice.kind)
        assertTrue(notice.message.contains("not contraception"))
        assertNull(CycleNotices.forToday(predictOn("2026-10-11"), allOn, d("2026-10-11")))
    }

    @Test
    fun eachNoticeNeedsItsOwnSwitch() {
        assertNull(CycleNotices.forToday(predictOn("2026-10-27"), allOn.copy(cyclePeriodSoon = false), d("2026-10-27")))
        assertNull(CycleNotices.forToday(predictOn("2026-11-01"), allOn.copy(cyclePeriodLate = false), d("2026-11-01")))
        assertNull(CycleNotices.forToday(predictOn("2026-10-10"), allOn.copy(cycleFertileWindow = false), d("2026-10-10")))
    }

    @Test
    fun noNoticesWithoutALoggedPeriodOrWhenStale() {
        assertNull(CycleNotices.forToday(CyclePredictor.predict(emptyList(), d("2026-10-27")), allOn, d("2026-10-27")))
        assertNull(CycleNotices.forToday(CyclePredictor.predict(log, d("2027-02-01")), allOn.copy(cyclePeriodLateDays = 123), d("2027-02-01")))
    }

    // ---- AI summary
    @Test
    fun theAiSummaryIsShortAndNeverContainsLogs() {
        val text = CycleAiSummary.build(predictOn("2026-10-13"), d("2026-10-13"))!!
        assertTrue(text, text.contains("day 13 of a typical 28-day cycle"))
        assertTrue(text, text.contains("follicular phase") || text.contains("fertile window"))
        assertTrue(text, text.contains("next period expected about Oct 29"))
        val lower = text.lowercase()
        assertFalse(lower.contains("symptom"))
        assertFalse(lower.contains("mood"))
        assertFalse(lower.contains("flow"))
        assertTrue(text.length < 300)
    }

    @Test
    fun theAiSummaryMentionsALatePeriodAndIrregularity() {
        val late = CycleAiSummary.build(predictOn("2026-11-02"), d("2026-11-02"))!!
        assertTrue(late, late.contains("4 days later than expected"))
        val irregularLog = listOf("2026-05-01", "2026-05-25", "2026-06-29", "2026-07-27").map { PeriodSpan(d(it)) }
        val irregular = CycleAiSummary.build(CyclePredictor.predict(irregularLog, d("2026-08-01")), d("2026-08-01"))!!
        assertTrue(irregular, irregular.contains("vary a lot"))
    }

    @Test
    fun theAiSummaryIsNullWithoutALoggedPeriod() {
        assertNull(CycleAiSummary.build(CyclePredictor.predict(emptyList(), d("2026-10-13")), d("2026-10-13")))
    }

    // ---- symptoms and flow
    @Test
    fun symptomMasksToggleAndListIndependently() {
        var mask = 0
        mask = Symptoms.toggle(mask, 1)
        mask = Symptoms.toggle(mask, 16)
        assertEquals(listOf("Cramps", "Breast tenderness"), Symptoms.labels(mask))
        mask = Symptoms.toggle(mask, 1)
        assertEquals(listOf("Breast tenderness"), Symptoms.labels(mask))
        assertEquals(Symptoms.ALL.size, Symptoms.ALL.map { it.bit }.toSet().size)
    }

    @Test
    fun storedCodesRoundTrip() {
        FlowLevel.values().forEach { assertEquals(it, FlowLevel.fromCode(it.code)) }
        Mood.values().forEach { assertEquals(it, Mood.fromCode(it.code)) }
        assertEquals(FlowLevel.NONE, FlowLevel.fromCode(99))
        assertEquals(Mood.NOT_SET, Mood.fromCode(99))
    }

    // ---- sync planner
    private data class Item(val id: String, val stamp: Long, val removed: Boolean = false)
    private fun plan(locals: List<Item>, remote: List<Item>, deleted: Set<String> = emptySet()) =
        LwwSyncPlanner.plan(locals, remote, deleted, { it.id }, { it.stamp }, { it.removed })

    @Test
    fun theNewerSideWins() {
        val p = plan(listOf(Item("a", 5), Item("b", 9)), listOf(Item("a", 8), Item("b", 3)))
        assertEquals(listOf(Item("a", 8)), p.pull)
        assertEquals(listOf(Item("b", 9)), p.push)
    }

    @Test
    fun equalStampsChangeNothing() {
        val p = plan(listOf(Item("a", 5)), listOf(Item("a", 5)))
        assertTrue(p.pull.isEmpty() && p.push.isEmpty() && p.removeLocal.isEmpty())
    }

    @Test
    fun newItemsGoBothWays() {
        val p = plan(listOf(Item("local-only", 1)), listOf(Item("remote-only", 2)))
        assertEquals(listOf(Item("remote-only", 2)), p.pull)
        assertEquals(listOf(Item("local-only", 1)), p.push)
    }

    @Test
    fun aCloudDeletionMarkerRemovesTheLocalItem() {
        val p = plan(listOf(Item("a", 5), Item("b", 5)), emptyList(), deleted = setOf("a"))
        assertEquals(listOf("a"), p.removeLocal)
        assertEquals(listOf(Item("b", 5)), p.push)
    }

    @Test
    fun anItemRemovedHereIsNeverPushedBackOrRemovedTwice() {
        val removedLocally = Item("a", 5, removed = true)
        assertTrue(plan(listOf(removedLocally), emptyList()).push.isEmpty())
        assertTrue(plan(listOf(removedLocally), emptyList(), deleted = setOf("a")).removeLocal.isEmpty())
    }

    @Test
    fun aNewerRemoteCopyReviveAnItemRemovedHere() {
        val p = plan(listOf(Item("a", 5, removed = true)), listOf(Item("a", 9)))
        assertEquals(listOf(Item("a", 9)), p.pull)
    }

    @Test
    fun aNewerLocalRemovalIsUploadedAsTheNewestState() {
        val p = plan(listOf(Item("a", 9, removed = true)), listOf(Item("a", 5)))
        assertEquals(listOf(Item("a", 9, removed = true)), p.push)
    }
}
