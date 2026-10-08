package com.example.presentation.mindfulness

import kotlin.math.PI
import kotlin.math.cos

enum class BreathPhaseKind(val label: String) {
    INHALE("Breathe in"),
    HOLD_IN("Hold"),
    EXHALE("Breathe out"),
    HOLD_OUT("Hold")
}

data class BreathState(
    val kind: BreathPhaseKind,
    val phaseIndex: Int,
    val secondsIntoPhase: Float,
    val phaseSeconds: Float,
    val fullness: Float,
    val cycleIndex: Int
)

data class BreathingPattern(
    val id: String,
    val name: String,
    val inhale: Int,
    val holdIn: Int,
    val exhale: Int,
    val holdOut: Int
) {
    val cycleSeconds: Int get() = inhale + holdIn + exhale + holdOut

    fun stateAt(elapsedSeconds: Float): BreathState {
        if (cycleSeconds <= 0) {
            return BreathState(BreathPhaseKind.INHALE, 0, 0f, 1f, 0f, 0)
        }
        val safeElapsed = elapsedSeconds.coerceAtLeast(0f)
        val cycleIndex = (safeElapsed / cycleSeconds).toInt()
        val t = safeElapsed % cycleSeconds

        data class PhaseSpec(val kind: BreathPhaseKind, val seconds: Int)
        val allPhases = listOf(
            PhaseSpec(BreathPhaseKind.INHALE, inhale),
            PhaseSpec(BreathPhaseKind.HOLD_IN, holdIn),
            PhaseSpec(BreathPhaseKind.EXHALE, exhale),
            PhaseSpec(BreathPhaseKind.HOLD_OUT, holdOut)
        )

        var runningTime = 0f
        var nonSkippedIndex = 0
        var resultState: BreathState? = null

        for (phase in allPhases) {
            if (phase.seconds <= 0) continue

            val phaseSec = phase.seconds.toFloat()
            val phaseEnd = runningTime + phaseSec

            if (resultState == null && (t < phaseEnd || runningTime + phaseSec >= cycleSeconds)) {
                val secondsIntoPhase = (t - runningTime).coerceIn(0f, phaseSec)
                val p = secondsIntoPhase / phaseSec

                val fullness = when (phase.kind) {
                    BreathPhaseKind.INHALE -> 0.5f - 0.5f * cos(PI.toFloat() * p)
                    BreathPhaseKind.HOLD_IN -> 1.0f
                    BreathPhaseKind.EXHALE -> 0.5f + 0.5f * cos(PI.toFloat() * p)
                    BreathPhaseKind.HOLD_OUT -> 0.0f
                }

                resultState = BreathState(
                    kind = phase.kind,
                    phaseIndex = nonSkippedIndex,
                    secondsIntoPhase = secondsIntoPhase,
                    phaseSeconds = phaseSec,
                    fullness = fullness.coerceIn(0f, 1f),
                    cycleIndex = cycleIndex
                )
            }

            runningTime += phaseSec
            nonSkippedIndex++
        }

        return resultState ?: BreathState(
            kind = BreathPhaseKind.INHALE,
            phaseIndex = 0,
            secondsIntoPhase = 0f,
            phaseSeconds = inhale.toFloat().coerceAtLeast(1f),
            fullness = 0f,
            cycleIndex = cycleIndex
        )
    }

    companion object {
        val COHERENT = BreathingPattern("coherent", "Coherent 5-5", 5, 0, 5, 0)
        val BOX = BreathingPattern("box", "Box 4-4-4-4", 4, 4, 4, 4)
        val RELAXING = BreathingPattern("relaxing", "Relaxing 4-7-8", 4, 7, 8, 0)
        val MEDITATION = BreathingPattern("meditation", "Slow", 5, 0, 5, 0)

        fun custom(inhale: Int, holdIn: Int, exhale: Int, holdOut: Int) = BreathingPattern(
            "custom",
            "Custom",
            inhale.coerceIn(2, 10),
            holdIn.coerceIn(0, 10),
            exhale.coerceIn(2, 12),
            holdOut.coerceIn(0, 10)
        )

        fun fromPrefs(id: String, customCsv: String): BreathingPattern {
            return when (id) {
                "coherent" -> COHERENT
                "box" -> BOX
                "relaxing" -> RELAXING
                "custom" -> {
                    try {
                        val parts = customCsv.split(",").map { it.trim().toInt() }
                        if (parts.size == 4) {
                            custom(parts[0], parts[1], parts[2], parts[3])
                        } else {
                            COHERENT
                        }
                    } catch (e: Exception) {
                        COHERENT
                    }
                }
                else -> COHERENT
            }
        }
    }
}

class SessionClock(private val nowMs: () -> Long) {
    private var accumulatedMs: Long = 0L
    private var lastStartMs: Long = 0L
    private var isRunning: Boolean = false

    fun start() {
        accumulatedMs = 0L
        lastStartMs = nowMs()
        isRunning = true
    }

    fun pause() {
        if (isRunning) {
            accumulatedMs += nowMs() - lastStartMs
            isRunning = false
        }
    }

    fun resume() {
        if (!isRunning) {
            lastStartMs = nowMs()
            isRunning = true
        }
    }

    fun reset() {
        accumulatedMs = 0L
        lastStartMs = 0L
        isRunning = false
    }

    fun elapsedMs(): Long {
        return if (isRunning) {
            accumulatedMs + (nowMs() - lastStartMs)
        } else {
            accumulatedMs
        }
    }
}
