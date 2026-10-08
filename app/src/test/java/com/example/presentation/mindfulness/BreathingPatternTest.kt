package com.example.presentation.mindfulness

import org.junit.Assert.assertEquals
import org.junit.Test

class BreathingPatternTest {

    @Test
    fun testCoherentPattern() {
        val pattern = BreathingPattern.COHERENT
        assertEquals(10, pattern.cycleSeconds)

        val s0 = pattern.stateAt(0f)
        assertEquals(BreathPhaseKind.INHALE, s0.kind)
        assertEquals(0f, s0.fullness, 0.01f)
        assertEquals(0, s0.cycleIndex)

        val s25 = pattern.stateAt(2.5f)
        assertEquals(0.5f, s25.fullness, 0.01f)

        val s5 = pattern.stateAt(5f)
        assertEquals(BreathPhaseKind.EXHALE, s5.kind)
        assertEquals(1f, s5.fullness, 0.01f)

        val s75 = pattern.stateAt(7.5f)
        assertEquals(0.5f, s75.fullness, 0.01f)

        val s10 = pattern.stateAt(10f)
        assertEquals(BreathPhaseKind.INHALE, s10.kind)
        assertEquals(1, s10.cycleIndex)
    }

    @Test
    fun testBoxPattern() {
        val pattern = BreathingPattern.BOX
        assertEquals(16, pattern.cycleSeconds)

        val s5 = pattern.stateAt(5f)
        assertEquals(BreathPhaseKind.HOLD_IN, s5.kind)
        assertEquals(1f, s5.fullness, 0.01f)

        val s13 = pattern.stateAt(13f)
        assertEquals(BreathPhaseKind.HOLD_OUT, s13.kind)
        assertEquals(0f, s13.fullness, 0.01f)
    }

    @Test
    fun testRelaxingPattern() {
        val pattern = BreathingPattern.RELAXING

        val s5 = pattern.stateAt(5f)
        assertEquals(BreathPhaseKind.HOLD_IN, s5.kind)

        val s12 = pattern.stateAt(12f)
        assertEquals(BreathPhaseKind.EXHALE, s12.kind)
    }

    @Test
    fun testCustomClampingAndParsing() {
        val customPattern = BreathingPattern.custom(1, 20, 1, 20)
        assertEquals(2, customPattern.inhale)
        assertEquals(10, customPattern.holdIn)
        assertEquals(2, customPattern.exhale)
        assertEquals(10, customPattern.holdOut)

        assertEquals(BreathingPattern.COHERENT, BreathingPattern.fromPrefs("custom", "garbage"))
    }

    @Test
    fun testSessionClockWithFakeNow() {
        var fakeTime = 0L
        val clock = SessionClock { fakeTime }

        clock.start() // fakeTime = 0
        assertEquals(0L, clock.elapsedMs())

        fakeTime = 3000L
        clock.pause()
        assertEquals(3000L, clock.elapsedMs())

        fakeTime += 10000L // fakeTime = 13000L, 10s pass while paused
        assertEquals(3000L, clock.elapsedMs())

        clock.resume() // resumed at fakeTime = 13000L
        fakeTime += 2000L // fakeTime = 15000L
        val totalElapsed = 3000L + 2000L // 5000L
        assertEquals(totalElapsed, clock.elapsedMs())
    }
}
