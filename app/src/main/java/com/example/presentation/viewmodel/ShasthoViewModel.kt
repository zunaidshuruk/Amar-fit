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
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.request.ReadRecordsRequest

import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.records.HeartRateRecord

import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

import java.util.Locale

class ShasthoViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)

    fun syncDataOnLogin(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncDataOnLogin()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            database.clearAllTables()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private val repository = AppRepository(database.userDao(), database.metricsDao(), database.savedDietChartDao())

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
        listOf(ChatMessage("Hello! I am Shastho AI, your universal health bot. Ask me anything about fitness, wellness, nutrition, or lifestyle!", false))
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()
    
    private val _isLoadingChat = MutableStateFlow(false)
    val isLoadingChat: StateFlow<Boolean> = _isLoadingChat.asStateFlow()
    
    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun analyzeFoodText(foodText: String) {
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
                    logScannedFood(parsedName, parsedCategory, parsedCalories, parsedDescription)
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

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            // Initialize today's metrics if not exist
            if (todayMetrics.value == null) {
                repository.saveMetrics(DailyMetric(date = todayDateString))
            }
        }
    }
    
        fun setSleep(hours: Float) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(sleepHours = hours)
            repository.saveMetrics(updated)
        }
    }

    fun addWater(amountLiters: Float) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(waterLiters = current.waterLiters + amountLiters)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
        }
    }
    
    fun setBloodGlucoseMorning(value: Float) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(bloodGlucoseMorning = value)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
        }
    }

    fun setBloodGlucoseNight(value: Float) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(bloodGlucoseNight = value)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
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
        }
    }
    
    fun setBloodPressure(value: String) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(bloodPressure = value)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
        }
    }
    
    
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

                val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
                val updated = current.copy(
                    steps = if (totalSteps > 0) totalSteps else current.steps,
                    sleepHours = if (sleepHours > 0f) sleepHours else current.sleepHours,
                    bloodPressure = if (bloodPressure.isNotEmpty()) bloodPressure else current.bloodPressure,
                    bloodGlucoseMorning = if (bloodGlucose > 0f) bloodGlucose else current.bloodGlucoseMorning,
                    heartRate = if (heartRate > 0) heartRate else current.heartRate
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
        }
    }
    
    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            val newUserMsg = ChatMessage(message, true)
            _chatHistory.value = _chatHistory.value + newUserMsg
            _isLoadingChat.value = true
            
            // Pass the entire conversation context to Gemini
            val response = repository.getChatResponse(_chatHistory.value, userProfile.value)
            
            _isLoadingChat.value = false
            _chatHistory.value = _chatHistory.value + ChatMessage(response, false)
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
            _coachAdvice.value = null
            val response = repository.generateCoachAdvice(topic, habit, benefits)
            _coachAdvice.value = response
            _isLoadingCoach.value = false
        }
    }
    
    private val _workoutPlan = MutableStateFlow<String?>(null)
    val workoutPlan: StateFlow<String?> = _workoutPlan.asStateFlow()
    
    private val _isLoadingWorkout = MutableStateFlow(false)
    val isLoadingWorkout: StateFlow<Boolean> = _isLoadingWorkout.asStateFlow()
    
    fun clearWorkoutPlan() {
        _workoutPlan.value = null
    }
    
    fun generateAIWorkout() {
        viewModelScope.launch {
            _isLoadingWorkout.value = true
            _workoutPlan.value = null
            val response = repository.generateWorkout(userProfile.value)
            _workoutPlan.value = response
            _isLoadingWorkout.value = false
        }
    }



    val savedDietCharts: StateFlow<List<com.example.data.local.SavedDietChart>> = repository.getAllSavedCharts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveDietChart(name: String, content: String, shoppingList: String) {
        viewModelScope.launch {
            repository.saveDietChart(
                com.example.data.local.SavedDietChart(
                    name = name,
                    chartContent = content,
                    shoppingList = shoppingList
                )
            )
        }
    }
    
    fun updateSavedDietChart(chart: com.example.data.local.SavedDietChart) {
        viewModelScope.launch {
            repository.saveDietChart(chart)
        }
    }
    
    fun deleteSavedDietChart(chart: com.example.data.local.SavedDietChart) {
        viewModelScope.launch {
            repository.deleteDietChart(chart)
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
                val chart = repository.generateDietChart(profile, durationDays)
                _dietChart.value = chart
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
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)
