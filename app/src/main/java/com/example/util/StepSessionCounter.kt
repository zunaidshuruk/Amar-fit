package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StepSessionCounter : SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var isCounterSensor = true

    private var initialSensorSteps: Int = -1
    private var isListening = false
    private var isPaused = false

    private val _sessionSteps = MutableStateFlow(0)
    val sessionSteps: StateFlow<Int> = _sessionSteps.asStateFlow()

    fun start(context: Context) {
        reset()
        isListening = true
        isPaused = false

        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        sensorManager = sm

        var sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor != null) {
            stepSensor = sensor
            isCounterSensor = true
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            if (sensor != null) {
                stepSensor = sensor
                isCounterSensor = false
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun stop() {
        isListening = false
        isPaused = false
        sensorManager?.unregisterListener(this)
        sensorManager = null
        stepSensor = null
    }

    fun reset() {
        initialSensorSteps = -1
        _sessionSteps.value = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isListening || isPaused || event == null) return

        if (isCounterSensor) {
            val totalSteps = event.values.firstOrNull()?.toInt() ?: return
            if (initialSensorSteps < 0) {
                initialSensorSteps = totalSteps
            }
            val currentSession = (totalSteps - initialSensorSteps).coerceAtLeast(0)
            _sessionSteps.value = currentSession
        } else {
            val detected = event.values.firstOrNull()?.toInt() ?: 0
            if (detected > 0) {
                _sessionSteps.value += detected
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
