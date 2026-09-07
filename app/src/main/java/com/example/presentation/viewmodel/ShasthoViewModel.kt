package com.example.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DailyMetric
import com.example.data.local.UserProfile
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.request.ReadRecordsRequest

import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Volume

import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

import java.util.Locale

class ShasthoViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val savedWorkoutSessionKeys = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun syncDataOnLogin(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncDataOnLogin()
            val profile = repository.userProfile.firstOrNull()
            val hasValidProfile = profile != null && profile.onboardingCompleted
            withContext(Dispatchers.Main) {
                onComplete(hasValidProfile)
            }
        }
    }

    fun deleteAccount(onComplete: (com.example.data.repository.DeleteAccountResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.deleteAccount()
            if (result is com.example.data.repository.DeleteAccountResult.Success) {
                database.clearAllTables()
                _weeklyInsights.value = null
                _chatHistory.value = listOf(
                    ChatMessage("Hi! I'm Amar-Fit AI. How can I help you?", false)
                )
                _scanResult.value = null
                _coachAdvice.value = null
                _workoutPlan.value = null
                _dietChart.value = null
                _shoppingList.value = null
                _premiumRecipe.value = null
                withContext(Dispatchers.Main) {
                    onComplete(com.example.data.repository.DeleteAccountResult.Success)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onComplete(result)
                }
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            database.clearAllTables()
            _weeklyInsights.value = null
            _chatHistory.value = listOf(
                ChatMessage("Hi! I'm Amar-Fit AI. How can I help you?", false)
            )
            _scanResult.value = null
            _coachAdvice.value = null
            _workoutPlan.value = null
            _dietChart.value = null
            _shoppingList.value = null
            _premiumRecipe.value = null
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private val repository = AppRepository(
        database.userDao(),
        database.metricsDao(),
        database.savedDietChartDao(),
        database.savedWorkoutDao(),
        database.savedChatDao(),
        database.activityEventDao(),
        database.youtubeVideoCacheDao(),
        database.medicalRecordDao()
    )

    private val startOfDayMillis: Long
        get() = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

    private val endOfDayMillis: Long
        get() = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis

    val todayActivityEvents: StateFlow<List<com.example.data.local.ActivityEvent>> = repository
        .getTodayActivityEvents(startOfDayMillis, endOfDayMillis)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun logActivityEvent(type: String, description: String) {
        viewModelScope.launch {
            repository.logActivityEvent(type, description)
        }
    }

    val userProfile = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    
    val todayMetrics = repository.getMetricsForDate(todayDateString).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val metricsHistory = repository.getMetricsHistory(90).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val recentFoodLogs = repository.getRecentFoodLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private val _weeklyInsights = MutableStateFlow<String?>(null)
    val weeklyInsights: StateFlow<String?> = _weeklyInsights.asStateFlow()
    
    private val _isLoadingInsights = MutableStateFlow(false)
    val isLoadingInsights: StateFlow<Boolean> = _isLoadingInsights.asStateFlow()
    
    fun fetchWeeklyInsights() {
        viewModelScope.launch {
            _isLoadingInsights.value = true
            val logs = recentFoodLogs.value
            val insights = repository.generateNutritionalInsights(logs)
            _weeklyInsights.value = insights
            _isLoadingInsights.value = false
        }
    }

    val todayFoodLogs = repository.getFoodLogsForDate(todayDateString).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allFoodLogs = repository.getRecentFoodLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Hello! I am Amar-Fit AI, your universal health bot. Ask me anything about fitness, wellness, nutrition, or lifestyle!", false))
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()
    
    private val _isLoadingChat = MutableStateFlow(false)
    val isLoadingChat: StateFlow<Boolean> = _isLoadingChat.asStateFlow()
    
    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun analyzeFoodText(foodText: String, mealType: String = "Snack") {
        viewModelScope.launch {
            _isScanning.value = true
            val result = repository.analyzeFoodText(foodText)
            try {
                val jsonString = result.substringAfter("{").substringBeforeLast("}")
                val json = org.json.JSONObject("{$jsonString}")
                val parsedName = json.optString("name", foodText)
                val parsedCategory = json.optString("category", "Manual Entry")
                val parsedCalories = json.optInt("calories", 0)
                val parsedDescription = json.optString("description", "")
                if (parsedCalories > 0) {
                    logScannedFood(parsedName, parsedCategory, parsedCalories, parsedDescription, mealType = mealType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isScanning.value = false
        }
    }

    fun analyzeImage(base64Image: String) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanResult.value = null
            
            val result = repository.analyzeFoodImage(base64Image)
            
            _scanResult.value = result
            _isScanning.value = false
        }
    }
    
    fun clearScanResult() {
        _scanResult.value = null
    }


    fun updateSettings(isDarkMode: Boolean, notificationsEnabled: Boolean, remindersEnabled: Boolean, selectedLanguage: String) {
        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            val updated = current.copy(
                isDarkMode = isDarkMode,
                notificationsEnabled = notificationsEnabled,
                remindersEnabled = remindersEnabled,
                selectedLanguage = selectedLanguage
            )
            repository.saveUserProfile(updated)
        }
    }

    fun updateTodayTileSlots(slots: List<String>) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            val joined = slots.take(3).joinToString(",")
            val updated = current.copy(todayTileSlots = joined)
            repository.saveUserProfile(updated)
        }
    }

    suspend fun saveProfile(profile: UserProfile): Boolean {
        return try {
            repository.saveUserProfile(profile)
            // Initialize today's metrics if not exist
            if (todayMetrics.value == null) {
                repository.saveMetrics(DailyMetric(date = todayDateString))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun setSleep(hours: Float) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(sleepHours = hours)
            repository.saveMetrics(updated)
            repository.logActivityEvent("sleep", "Logged ${hours}h sleep")

            // Write to Health Connect
            try {
                if (hours > 0f) {
                    val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
                    val endInstant = Instant.now()
                    val startInstant = endInstant.minusSeconds((hours * 3600f).toLong().coerceAtLeast(1L))
                    val zoneOffset = ZoneId.systemDefault().rules.getOffset(endInstant)
                    val sleepSession = SleepSessionRecord(
                        startTime = startInstant,
                        startZoneOffset = zoneOffset,
                        endTime = endInstant,
                        endZoneOffset = zoneOffset,
                        title = "Sleep"
                    )
                    healthConnectClient.insertRecords(listOf(sleepSession))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addWater(amountLiters: Float) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(waterLiters = current.waterLiters + amountLiters)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("water", "Logged ${amountLiters}L water")

            // Write to Health Connect
            try {
                if (amountLiters > 0f) {
                    val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
                    val now = Instant.now()
                    val zoneOffset = ZoneId.systemDefault().rules.getOffset(now)
                    val hydrationRecord = HydrationRecord(
                        startTime = now,
                        startZoneOffset = zoneOffset,
                        endTime = now,
                        endZoneOffset = zoneOffset,
                        volume = Volume.liters(amountLiters.toDouble())
                    )
                    healthConnectClient.insertRecords(listOf(hydrationRecord))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun saveCompletedWorkoutSession(
        planTitle: String,
        startTime: Instant,
        endTime: Instant,
        totalElapsedSeconds: Int,
        caloriesBurned: Double?
    ) = withContext(Dispatchers.IO) {
        val sessionKey = "${planTitle}_${startTime.toEpochMilli()}"
        if (savedWorkoutSessionKeys.contains(sessionKey)) {
            return@withContext
        }
        savedWorkoutSessionKeys.add(sessionKey)

        // 1. Save locally first:
        // Update today's DailyMetric locally: ADD (not overwrite) the session's total elapsed minutes
        // to exerciseMinutes and the session's computed MET-based calorie total to activeCaloriesBurned.
        // If calorie total is null, skip only the calorie increment, still record the duration.
        val durationSeconds = if (totalElapsedSeconds > 0) {
            totalElapsedSeconds
        } else {
            java.time.Duration.between(startTime, endTime).seconds.toInt().coerceAtLeast(0)
        }
        val elapsedMinutes = (durationSeconds / 60).coerceAtLeast(if (durationSeconds > 0) 1 else 0)
        val calIncrement = caloriesBurned?.let { kotlin.math.round(it).toInt().coerceAtLeast(0) }

        val current = todayMetrics.value ?: repository.getMetricsForDate(todayDateString).firstOrNull() ?: DailyMetric(date = todayDateString)
        val updated = current.copy(
            exerciseMinutes = (current.exerciseMinutes + elapsedMinutes).coerceAtLeast(0),
            activeCaloriesBurned = if (calIncrement != null) (current.activeCaloriesBurned + calIncrement).coerceAtLeast(0) else current.activeCaloriesBurned
        )
        repository.saveMetrics(updated)
        repository.checkAndAwardBadges(updated)
        val logMsg = if (calIncrement != null) {
            "Completed workout '$planTitle' (${elapsedMinutes}m, ~${calIncrement} kcal)"
        } else {
            "Completed workout '$planTitle' (${elapsedMinutes}m)"
        }
        repository.logActivityEvent("workout", logMsg)

        // 2. Health Connect write in its own try-catch (can never block or fail the local save)
        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
            val effectiveEndTime = if (endTime.isAfter(startTime)) endTime else startTime.plusSeconds(durationSeconds.toLong().coerceAtLeast(1L))
            val startOffset = ZoneId.systemDefault().rules.getOffset(startTime)
            val endOffset = ZoneId.systemDefault().rules.getOffset(effectiveEndTime)
            val exerciseSession = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = startOffset,
                endTime = effectiveEndTime,
                endZoneOffset = endOffset,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
                title = planTitle
            )
            healthConnectClient.insertRecords(listOf(exerciseSession))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun setBloodGlucoseMorning(value: Float) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(bloodGlucoseMorning = value)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("glucose", "Logged blood glucose: ${value} mmol/L")
        }
    }

    fun setBloodGlucoseNight(value: Float) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(bloodGlucoseNight = value)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("glucose", "Logged blood glucose: ${value} mmol/L")
        }
    }
    
    fun setWeightAndHeight(weight: Float, height: Float) {
        viewModelScope.launch {
            val currentProfile = userProfile.value ?: return@launch
            val updatedProfile = currentProfile.copy(weightKg = weight, heightCm = height)
            repository.saveUserProfile(updatedProfile)
            
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(weightKg = weight)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("weight", "Logged weight: ${weight}kg")
        }
    }
    
    fun setWeight(value: Float) {
        viewModelScope.launch {
            val currentProfile = userProfile.value ?: return@launch
            val updatedProfile = currentProfile.copy(weightKg = value)
            repository.saveUserProfile(updatedProfile)
            
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(weightKg = value)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("weight", "Logged weight: ${value}kg")
        }
    }
    
    fun setBloodPressure(value: String) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(bloodPressure = value)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("blood_pressure", "Logged blood pressure: ${value}")
        }
    }
    
    
    @OptIn(ExperimentalFeatureAvailabilityApi::class)
    fun syncWithHealthConnect(context: Context) {
        viewModelScope.launch {
            try {
                if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return@launch
                val healthConnectClient = HealthConnectClient.getOrCreate(context)
                
                // Set time range for today
                val zdt = ZonedDateTime.now(ZoneId.systemDefault())
                val startOfDay = zdt.toLocalDate().atStartOfDay(zdt.zone).toInstant()
                val endOfDay = zdt.toLocalDate().plusDays(1).atStartOfDay(zdt.zone).toInstant()
                val timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                
                // 1. Correct Total Steps using Aggregate (auto-deduplicates from multiple sources)
                val stepAggregate = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = timeRangeFilter
                    )
                )
                val totalSteps = stepAggregate[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0

                // 2. Sleep Tracking using Aggregate
                val sleepAggregate = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                        timeRangeFilter = timeRangeFilter
                    )
                )
                val totalSleepDuration = sleepAggregate[SleepSessionRecord.SLEEP_DURATION_TOTAL]
                var sleepHours = 0f
                if (totalSleepDuration != null) {
                    sleepHours = totalSleepDuration.toMinutes() / 60f
                } else {
                    // Fallback to readRecords if aggregate is empty
                    val sleepResponse = healthConnectClient.readRecords(
                        ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter)
                    )
                    var fallbackMinutes = 0L
                    for (record in sleepResponse.records) {
                        fallbackMinutes += java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                    }
                    sleepHours = fallbackMinutes / 60f
                }

                // 3. Latest Blood Pressure
                var bloodPressure = ""
                val bpResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(BloodPressureRecord::class, timeRangeFilter)
                )
                if (bpResponse.records.isNotEmpty()) {
                    val latest = bpResponse.records.maxByOrNull { it.time }
                    if (latest != null) {
                        bloodPressure = "${latest.systolic.inMillimetersOfMercury.toInt()}/${latest.diastolic.inMillimetersOfMercury.toInt()}"
                    }
                }

                // 4. Latest Blood Glucose
                var bloodGlucose = 0f
                val bgResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(BloodGlucoseRecord::class, timeRangeFilter)
                )
                if (bgResponse.records.isNotEmpty()) {
                    val latest = bgResponse.records.maxByOrNull { it.time }
                    if (latest != null) {
                        bloodGlucose = latest.level.inMilligramsPerDeciliter.toFloat()
                    }
                }

                // 5. Latest Heart Rate
                var heartRate = 0
                val hrResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter)
                )
                if (hrResponse.records.isNotEmpty()) {
                    val latestRecord = hrResponse.records.maxByOrNull { it.startTime }
                    if (latestRecord != null && latestRecord.samples.isNotEmpty()) {
                        heartRate = latestRecord.samples.last().beatsPerMinute.toInt()
                    }
                }

                // 6. Distance using Aggregate
                val distanceAggregate = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = timeRangeFilter
                    )
                )
                val totalDistance = distanceAggregate[DistanceRecord.DISTANCE_TOTAL]?.inMeters?.toFloat() ?: 0f

                // 7. Exercise Session Duration
                val exerciseAggregate = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
                        timeRangeFilter = timeRangeFilter
                    )
                )
                val totalExerciseDuration = exerciseAggregate[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]
                var exerciseMinutes = 0
                if (totalExerciseDuration != null) {
                    exerciseMinutes = totalExerciseDuration.toMinutes().toInt()
                } else {
                    // Fallback to readRecords if aggregate is empty
                    val exerciseResponse = healthConnectClient.readRecords(
                        ReadRecordsRequest(ExerciseSessionRecord::class, timeRangeFilter)
                    )
                    var fallbackMinutes = 0L
                    for (record in exerciseResponse.records) {
                        fallbackMinutes += java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                    }
                    exerciseMinutes = fallbackMinutes.toInt()
                }

                // 8. Nutrition Calories
                val nutritionAggregate = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(NutritionRecord.ENERGY_TOTAL),
                        timeRangeFilter = timeRangeFilter
                    )
                )
                val totalEnergy = nutritionAggregate[NutritionRecord.ENERGY_TOTAL]
                var externalNutritionCalories = 0
                if (totalEnergy != null) {
                    externalNutritionCalories = totalEnergy.inKilocalories.toInt()
                }

                // 9. Active Calories Burned using Aggregate
                var activeCaloriesBurned = 0
                try {
                    val activeCaloriesAggregate = healthConnectClient.aggregate(
                        AggregateRequest(
                            metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                            timeRangeFilter = timeRangeFilter
                        )
                    )
                    val totalActiveCalories = activeCaloriesAggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                    if (totalActiveCalories != null) {
                        activeCaloriesBurned = totalActiveCalories.inKilocalories.toInt()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 10. Latest Heart Rate Variability (HRV Rmssd)
                var heartRateVariability = 0f
                try {
                    val hrvResponse = healthConnectClient.readRecords(
                        ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, timeRangeFilter)
                    )
                    if (hrvResponse.records.isNotEmpty()) {
                        val latestHrv = hrvResponse.records.maxByOrNull { it.time }
                        if (latestHrv != null) {
                            heartRateVariability = latestHrv.heartRateVariabilityMillis.toFloat()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 11. Latest Oxygen Saturation (SpO2)
                var oxygenSaturation = 0f
                try {
                    val spo2Response = healthConnectClient.readRecords(
                        ReadRecordsRequest(OxygenSaturationRecord::class, timeRangeFilter)
                    )
                    if (spo2Response.records.isNotEmpty()) {
                        val latestSpo2 = spo2Response.records.maxByOrNull { it.time }
                        if (latestSpo2 != null) {
                            oxygenSaturation = latestSpo2.percentage.value.toFloat()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 12. Latest Skin Temperature
                var skinTemperatureCelsius = 0f
                try {
                    val skinTempResponse = healthConnectClient.readRecords(
                        ReadRecordsRequest(SkinTemperatureRecord::class, timeRangeFilter)
                    )
                    if (skinTempResponse.records.isNotEmpty()) {
                        val latestSkinTemp = skinTempResponse.records.maxByOrNull { it.startTime }
                        val baseline = latestSkinTemp?.baseline
                        if (baseline != null) {
                            skinTemperatureCelsius = baseline.inCelsius.toFloat()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 13. Latest Respiratory Rate (Breathing Rate)
                var respiratoryRate = 0f
                try {
                    val respResponse = healthConnectClient.readRecords(
                        ReadRecordsRequest(RespiratoryRateRecord::class, timeRangeFilter)
                    )
                    if (respResponse.records.isNotEmpty()) {
                        val latestResp = respResponse.records.maxByOrNull { it.time }
                        if (latestResp != null) {
                            respiratoryRate = latestResp.rate.toFloat()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 14. Mindfulness Session Duration (Feature-Gated)
                var mindfulnessMinutes = 0
                try {
                    val isMindfulnessAvailable = healthConnectClient.features.getFeatureStatus(
                        HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION
                    ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE

                    if (isMindfulnessAvailable) {
                        val mindfulnessAggregate = healthConnectClient.aggregate(
                            AggregateRequest(
                                metrics = setOf(MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL),
                                timeRangeFilter = timeRangeFilter
                            )
                        )
                        val totalMindfulnessDuration = mindfulnessAggregate[MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL]
                        if (totalMindfulnessDuration != null) {
                            mindfulnessMinutes = totalMindfulnessDuration.toMinutes().toInt()
                        } else {
                            val mindfulnessResponse = healthConnectClient.readRecords(
                                ReadRecordsRequest(MindfulnessSessionRecord::class, timeRangeFilter)
                            )
                            var fallbackMinutes = 0L
                            for (record in mindfulnessResponse.records) {
                                fallbackMinutes += java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                            }
                            mindfulnessMinutes = fallbackMinutes.toInt()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
                val updated = current.copy(
                    steps = if (totalSteps > 0) totalSteps else current.steps,
                    sleepHours = if (sleepHours > 0f) sleepHours else current.sleepHours,
                    bloodPressure = if (bloodPressure.isNotEmpty()) bloodPressure else current.bloodPressure,
                    bloodGlucoseMorning = if (bloodGlucose > 0f) bloodGlucose else current.bloodGlucoseMorning,
                    heartRate = if (heartRate > 0) heartRate else current.heartRate,
                    distanceMeters = if (totalDistance > 0f) totalDistance else current.distanceMeters,
                    exerciseMinutes = if (exerciseMinutes > 0) exerciseMinutes else current.exerciseMinutes,
                    externalNutritionCalories = if (externalNutritionCalories > 0) externalNutritionCalories else current.externalNutritionCalories,
                    activeCaloriesBurned = if (activeCaloriesBurned > 0) activeCaloriesBurned else current.activeCaloriesBurned,
                    heartRateVariability = if (heartRateVariability > 0f) heartRateVariability else current.heartRateVariability,
                    oxygenSaturation = if (oxygenSaturation > 0f) oxygenSaturation else current.oxygenSaturation,
                    skinTemperatureCelsius = if (skinTemperatureCelsius > 0f) skinTemperatureCelsius else current.skinTemperatureCelsius,
                    respiratoryRate = if (respiratoryRate > 0f) respiratoryRate else current.respiratoryRate,
                    mindfulnessMinutes = if (mindfulnessMinutes > 0) mindfulnessMinutes else current.mindfulnessMinutes
                )
                repository.saveMetrics(updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

        fun setSteps(steps: Int) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(steps = steps)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
        }
    }

    fun addSteps(steps: Int) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(steps = current.steps + steps)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
        }
    }
    
    fun checkNutritionalDeficiencies(context: android.content.Context) {
        viewModelScope.launch {
            val sharedPrefs = context.getSharedPreferences("ShasthoPrefs", android.content.Context.MODE_PRIVATE)
            val lastCheck = sharedPrefs.getLong("last_deficiency_check", 0L)
            val now = System.currentTimeMillis()
            if (now - lastCheck > 86400000L) {
                val profile = userProfile.filterNotNull().first()
                val logs = repository.getRecentFoodLogs().first()
                if (logs.size >= 5) {
                    val deficiencyAlert = repository.checkChronicDeficiency(logs, profile)
                    if (deficiencyAlert != null) {
                        com.example.presentation.notifications.NotificationHelper.showNotification(
                            context = context,
                            title = "Nutritional Alert",
                            message = deficiencyAlert,
                            notificationId = 400
                        )
                    }
                    sharedPrefs.edit().putLong("last_deficiency_check", now).apply()
                }
            }
        }
    }


    fun deleteFoodLog(foodLog: com.example.data.local.FoodLog) {
        viewModelScope.launch {
            repository.deleteFoodLog(foodLog)
        }
    }

    fun updateFoodLog(foodLog: com.example.data.local.FoodLog) {
        viewModelScope.launch {
            repository.updateFoodLog(foodLog)
        }
    }

    fun logScannedFood(
        name: String, 
        category: String, 
        calories: Int, 
        description: String,
        time: String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
        mealType: String = "Snack"
    ) {
        viewModelScope.launch {
            val foodLog = com.example.data.local.FoodLog(
                date = todayDateString,
                name = name,
                category = category,
                calories = calories,
                description = description,
                time = time,
                mealType = mealType
            )
            repository.saveFoodLog(foodLog)
            
            // Also add calories to today's metrics
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(caloriesConsumed = current.caloriesConsumed + calories)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("food", "Logged $name")

            // Write to Health Connect
            try {
                if (calories > 0) {
                    val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
                    val now = Instant.now()
                    val zoneOffset = ZoneId.systemDefault().rules.getOffset(now)
                    val nutritionRecord = NutritionRecord(
                        startTime = now,
                        startZoneOffset = zoneOffset,
                        endTime = now,
                        endZoneOffset = zoneOffset,
                        energy = Energy.kilocalories(calories.toDouble()),
                        name = name
                    )
                    healthConnectClient.insertRecords(listOf(nutritionRecord))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            val newUserMsg = ChatMessage(message, true)
            _chatHistory.value = _chatHistory.value + newUserMsg
            _isLoadingChat.value = true

            val placeholderIndex = _chatHistory.value.size
            _chatHistory.value = _chatHistory.value + ChatMessage("", false)

            var accumulated = ""
            try {
                repository.getChatResponseStream(_chatHistory.value.dropLast(1), userProfile.value).collect { chunk ->
                    accumulated += chunk
                    _chatHistory.value = _chatHistory.value.toMutableList().also {
                        it[placeholderIndex] = ChatMessage(accumulated, false)
                    }
                }
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value.toMutableList().also {
                    it[placeholderIndex] = ChatMessage("Sorry, I couldn't process that. Please try again.", false)
                }
            }
            _isLoadingChat.value = false
        }
    }
    
    private val _coachAdvice = MutableStateFlow<String?>(null)
    val coachAdvice: StateFlow<String?> = _coachAdvice.asStateFlow()
    
    private val _isLoadingCoach = MutableStateFlow(false)
    val isLoadingCoach: StateFlow<Boolean> = _isLoadingCoach.asStateFlow()
    
    fun clearCoachAdvice() {
        _coachAdvice.value = null
    }

    fun requestCoachAdvice(topic: String, habit: String, benefits: String) {
        viewModelScope.launch {
            _isLoadingCoach.value = true
            _coachAdvice.value = ""
            var accumulated = ""
            try {
                repository.generateCoachAdviceStream(topic, habit, benefits).collect { chunk ->
                    accumulated += chunk
                    _coachAdvice.value = accumulated
                }
            } catch (e: Exception) {
                _coachAdvice.value = "Sorry, I couldn't generate advice right now. Please try again."
            }
            _isLoadingCoach.value = false
        }
    }
    
    private val _workoutPlan = MutableStateFlow<String?>(null)
    val workoutPlan: StateFlow<String?> = _workoutPlan.asStateFlow()
    
    private val _isLoadingWorkout = MutableStateFlow(false)
    val isLoadingWorkout: StateFlow<Boolean> = _isLoadingWorkout.asStateFlow()

    private val _structuredWorkoutPlan = MutableStateFlow<com.example.data.model.WorkoutPlan?>(null)
    val structuredWorkoutPlan: StateFlow<com.example.data.model.WorkoutPlan?> = _structuredWorkoutPlan.asStateFlow()

    private val _rawStructuredWorkoutJson = MutableStateFlow<String>("")
    val rawStructuredWorkoutJson: StateFlow<String> = _rawStructuredWorkoutJson.asStateFlow()

    private val _isLoadingStructuredWorkout = MutableStateFlow(false)
    val isLoadingStructuredWorkout: StateFlow<Boolean> = _isLoadingStructuredWorkout.asStateFlow()

    private val _structuredWorkoutError = MutableStateFlow<String?>(null)
    val structuredWorkoutError: StateFlow<String?> = _structuredWorkoutError.asStateFlow()
    
    fun clearWorkoutPlan() {
        _workoutPlan.value = null
    }

    fun clearStructuredWorkoutPlan() {
        _structuredWorkoutPlan.value = null
        _rawStructuredWorkoutJson.value = ""
        _structuredWorkoutError.value = null
    }

    suspend fun resolveYoutubeVideoId(searchQuery: String): String? {
        return repository.resolveYoutubeVideoId(searchQuery)
    }

    fun generateAIStructuredWorkout() {
        viewModelScope.launch {
            _isLoadingStructuredWorkout.value = true
            _structuredWorkoutError.value = null
            _structuredWorkoutPlan.value = null
            _rawStructuredWorkoutJson.value = ""
            val result = repository.generateStructuredWorkout(userProfile.value)
            result.onSuccess { plan ->
                _structuredWorkoutPlan.value = plan
                try {
                    val adapter = com.example.data.remote.RetrofitClient.moshi.adapter(com.example.data.model.WorkoutPlan::class.java)
                    _rawStructuredWorkoutJson.value = adapter.toJson(plan)
                } catch (e: Exception) {
                    _rawStructuredWorkoutJson.value = ""
                }
            }.onFailure { error ->
                _structuredWorkoutError.value = error.message ?: "Could not generate structured workout. Please try again."
            }
            _isLoadingStructuredWorkout.value = false
        }
    }
    
    fun generateAIWorkout() {
        viewModelScope.launch {
            _isLoadingWorkout.value = true
            _workoutPlan.value = ""
            var accumulated = ""
            try {
                repository.generateWorkoutStream(userProfile.value).collect { chunk ->
                    accumulated += chunk
                    _workoutPlan.value = accumulated
                }
            } catch (e: Exception) {
                _workoutPlan.value = "Sorry, I couldn't generate a workout right now. Please try again."
            }
            _isLoadingWorkout.value = false
        }
    }



    private val _syncErrorEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val syncErrorEvent: SharedFlow<String> = _syncErrorEvent.asSharedFlow()

    val savedDietCharts: StateFlow<List<com.example.data.local.SavedDietChart>> = repository.getAllSavedCharts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val savedWorkouts: kotlinx.coroutines.flow.StateFlow<List<com.example.data.local.SavedWorkout>> = repository.getAllSavedWorkouts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveDietChart(name: String, content: String, shoppingList: String) {
        viewModelScope.launch {
            val success = repository.saveDietChart(
                com.example.data.local.SavedDietChart(
                    name = name,
                    chartContent = content,
                    shoppingList = shoppingList
                )
            )
            if (!success) {
                _syncErrorEvent.emit("Saved locally, but couldn't sync to the cloud — check your connection")
            }
            repository.logActivityEvent("diet_chart", "Saved diet plan: $name")
        }
    }
    
    fun updateSavedDietChart(chart: com.example.data.local.SavedDietChart) {
        viewModelScope.launch {
            val success = repository.saveDietChart(chart)
            if (!success) {
                _syncErrorEvent.emit("Saved locally, but couldn't sync to the cloud — check your connection")
            }
        }
    }
    
    fun deleteSavedDietChart(chart: com.example.data.local.SavedDietChart) {
        viewModelScope.launch {
            val success = repository.deleteDietChart(chart)
            if (!success) {
                _syncErrorEvent.emit("Deleted locally, but couldn't sync to the cloud — check your connection")
            }
        }
    }

    private val _dietChart = MutableStateFlow<String?>(null)
    val dietChart: StateFlow<String?> = _dietChart

    private val _isGeneratingDiet = MutableStateFlow(false)
    val isGeneratingDiet: StateFlow<Boolean> = _isGeneratingDiet

    private val _shoppingList = MutableStateFlow<String?>(null)
    val shoppingList: StateFlow<String?> = _shoppingList

    private val _premiumRecipe = MutableStateFlow<String?>(null)
    val premiumRecipe: StateFlow<String?> = _premiumRecipe.asStateFlow()
    
    private val _isLoadingRecipe = MutableStateFlow(false)
    val isLoadingRecipe: StateFlow<Boolean> = _isLoadingRecipe.asStateFlow()
    

    fun generateDietChart(durationDays: Int) {
        viewModelScope.launch {
            _isGeneratingDiet.value = true
            val profile = userProfile.value
            if (profile != null) {
                _dietChart.value = ""
                var accumulated = ""
                try {
                    repository.generateDietChartStream(profile, durationDays).collect { chunk ->
                        accumulated += chunk
                        _dietChart.value = accumulated
                    }
                } catch (e: Exception) {
                    _dietChart.value = "The AI is currently busy or unavailable. Please try again in a minute."
                }
            } else {
                _dietChart.value = "Please complete your profile first."
            }
            _isGeneratingDiet.value = false
        }
    }

    fun updateDietChart(newChart: String) {
        _dietChart.value = newChart
    }

    fun generateShoppingList(chart: String) {
        viewModelScope.launch {
            _isGeneratingDiet.value = true
            val list = repository.generateShoppingList(chart)
            _shoppingList.value = list
            _isGeneratingDiet.value = false
        }
    }

    fun generatePremiumRecipe(query: String) {
        viewModelScope.launch {
            _isLoadingRecipe.value = true
            _premiumRecipe.value = null
            val response = repository.generatePremiumRecipe(query, userProfile.value)
            _premiumRecipe.value = response
            _isLoadingRecipe.value = false
        }
    }
    fun saveWorkout(title: String, content: String, structuredJson: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val workout = com.example.data.local.SavedWorkout(
                title = title,
                content = content,
                structuredJson = structuredJson
            )
            val success = repository.saveWorkout(workout)
            if (!success) {
                _syncErrorEvent.emit("Saved locally, but couldn't sync to the cloud — check your connection")
            }
            repository.logActivityEvent("workout", "Saved workout: $title")
        }
    }

    fun deleteWorkout(workout: com.example.data.local.SavedWorkout) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.deleteWorkout(workout)
            if (!success) {
                _syncErrorEvent.emit("Deleted locally, but couldn't sync to the cloud — check your connection")
            }
        }
    }

    val savedChats: StateFlow<List<com.example.data.local.SavedChat>> = repository.getAllSavedChats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveChat(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val history = _chatHistory.value
            if (history.isNotEmpty()) {
                val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, ChatMessage::class.java)
                val adapter = com.example.data.remote.RetrofitClient.moshi.adapter<List<ChatMessage>>(listType)
                val jsonMessages = adapter.toJson(history)
                val chat = com.example.data.local.SavedChat(
                    title = title.ifBlank { "Chat Session" },
                    messages = jsonMessages
                )
                val success = repository.saveChat(chat)
                if (!success) {
                    _syncErrorEvent.emit("Saved locally, but couldn't sync to the cloud — check your connection")
                }
            }
        }
    }

    fun deleteChat(chat: com.example.data.local.SavedChat) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.deleteChat(chat)
            if (!success) {
                _syncErrorEvent.emit("Deleted locally, but couldn't sync to the cloud — check your connection")
            }
        }
    }

    val medicalRecords: StateFlow<List<com.example.data.local.MedicalRecord>> = repository.getMedicalRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addMedicalRecord(record: com.example.data.local.MedicalRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.addMedicalRecord(record)
            if (!success) {
                _syncErrorEvent.emit("Saved locally, but couldn't sync to the cloud — check your connection")
            }
        }
    }

    fun deleteMedicalRecord(record: com.example.data.local.MedicalRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.deleteMedicalRecord(record)
            if (!success) {
                _syncErrorEvent.emit("Deleted locally, but couldn't sync to the cloud — check your connection")
            }
        }
    }
}

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ChatMessage(
    val text: String,
    val isUser: Boolean
)
