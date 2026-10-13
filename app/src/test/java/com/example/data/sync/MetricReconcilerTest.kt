package com.example.data.sync

import com.example.data.local.DailyMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricReconcilerTest {

    private fun m(date: String, steps: Int = 0, glucose: Float = 0f, updatedAt: Long = 0L) =
        DailyMetric(date = date, steps = steps, bloodGlucoseBeforeBreakfast = glucose, updatedAt = updatedAt)

    @Test
    fun higherRemoteStepsAreApplied() {
        val plan = MetricReconciler.plan(listOf(m("2026-10-12", steps = 21)), listOf(m("2026-10-12", steps = 9209)))
        assertEquals(1, plan.applyFromRemote.size)
        assertEquals(9209, plan.applyFromRemote[0].steps)
        assertTrue("local 21 must not be pushed over the cloud's 9209", plan.pushDates.isEmpty())
    }

    @Test
    fun higherLocalStepsArePushed() {
        val plan = MetricReconciler.plan(listOf(m("2026-10-12", steps = 9209)), listOf(m("2026-10-12", steps = 21)))
        assertTrue(plan.applyFromRemote.isEmpty())
        assertEquals(listOf("2026-10-12"), plan.pushDates)
    }

    @Test
    fun identicalRowsMoveNothing() {
        val row = m("2026-10-12", steps = 500, glucose = 6.5f, updatedAt = 10)
        val plan = MetricReconciler.plan(listOf(row), listOf(row))
        assertTrue(plan.applyFromRemote.isEmpty())
        assertTrue(plan.pushDates.isEmpty())
    }

    @Test
    fun localOnlyDataIsPushedButEmptyRowsAreNot() {
        val plan = MetricReconciler.plan(
            listOf(m("2026-10-10", steps = 3000), m("2026-10-11")),
            emptyList()
        )
        assertEquals(listOf("2026-10-10"), plan.pushDates)
    }

    @Test
    fun remoteOnlyDataIsApplied() {
        val plan = MetricReconciler.plan(emptyList(), listOf(m("2026-10-09", steps = 100), m("2026-10-08")))
        assertEquals(listOf("2026-10-09"), plan.applyFromRemote.map { it.date })
        assertTrue(plan.pushDates.isEmpty())
    }

    @Test
    fun readingMissingInCloudIsPushedAndReadingMissingLocallyIsPulled() {
        // phone has a glucose reading the cloud lacks; cloud has steps the phone lacks
        val local = m("2026-10-12", steps = 0, glucose = 7.2f, updatedAt = 5)
        val remote = m("2026-10-12", steps = 800, glucose = 0f, updatedAt = 4)
        val plan = MetricReconciler.plan(listOf(local), listOf(remote))
        assertEquals(1, plan.applyFromRemote.size)
        assertEquals(listOf("2026-10-12"), plan.pushDates)
    }

    @Test
    fun afterApplyingRemoteNothingNeedsPushingBack() {
        val local = m("2026-10-12", steps = 10)
        val remote = m("2026-10-12", steps = 50, glucose = 6.0f, updatedAt = 9)
        val plan = MetricReconciler.plan(listOf(local), listOf(remote))
        assertEquals(1, plan.applyFromRemote.size)
        assertTrue(plan.pushDates.isEmpty())
    }

    @Test
    fun isEmptyIgnoresUpdatedAt() {
        assertTrue(MetricReconciler.isEmpty(m("2026-10-12", updatedAt = 99)))
        assertEquals(false, MetricReconciler.isEmpty(m("2026-10-12", steps = 1)))
    }
}
