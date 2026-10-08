package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepSessionCounter(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()

    private var baseline: Float = -1f
    private var pausedSteps: Float = 0f
    private var valueAtPause: Float = -1f
    private var lastValue: Float = -1f
    private var isPaused = false

    fun isAvailable(): Boolean = stepSensor != null

    fun start() {
        try {
            _steps.value = 0
            baseline = -1f
            pausedSteps = 0f
            valueAtPause = -1f
            lastValue = -1f
            isPaused = false
            stepSensor?.let { sensor ->
                sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (_: Exception) {}
    }

    fun pause() {
        try {
            isPaused = true
            if (lastValue >= 0f) {
                valueAtPause = lastValue
            }
        } catch (_: Exception) {}
    }

    fun resume() {
        try {
            if (isPaused) {
                if (valueAtPause >= 0f && lastValue >= valueAtPause) {
                    pausedSteps += (lastValue - valueAtPause)
                }
                valueAtPause = -1f
                isPaused = false
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            sensorManager?.unregisterListener(this)
            _steps.value = 0
            baseline = -1f
            pausedSteps = 0f
            valueAtPause = -1f
            lastValue = -1f
            isPaused = false
        } catch (_: Exception) {}
    }

    fun unregisterKeepingBaseline() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
    }

    fun reRegister() {
        try {
            stepSensor?.let { sensor ->
                sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        try {
            if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
            val rawValue = event.values.firstOrNull() ?: return
            if (rawValue < 0f) return

            lastValue = rawValue

            if (baseline < 0f) {
                baseline = rawValue
            } else if (rawValue < baseline) {
                baseline = rawValue
                pausedSteps = 0f
            }

            if (isPaused) {
                return
            }

            if (valueAtPause >= 0f) {
                if (rawValue >= valueAtPause) {
                    pausedSteps += (rawValue - valueAtPause)
                }
                valueAtPause = -1f
            }

            val currentSteps = (rawValue - baseline - pausedSteps).toInt().coerceAtLeast(0)
            _steps.value = currentSteps
        } catch (_: Exception) {}
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
