package com.example.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DailyMetric
import com.example.data.local.UserProfile
import com.example.data.repository.AppRepository
import com.example.data.repository.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Volume

import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

import java.util.Locale

class ShasthoViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val savedWorkoutSessionKeys = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val savedMindfulnessSessionKeys = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun syncDataOnLogin(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeoutOrNull(4000L) {
                    repository.syncDataOnLogin()
                }
            } catch (e: Exception) {
                // Ignore sync timeout or network errors during startup
            }
            val profile = try {
                withTimeoutOrNull(1000L) {
                    repository.userProfile.firstOrNull()
                }
            } catch (e: Exception) {
                null
            }
            val hasValidProfile = profile != null && profile.onboardingCompleted
            if (profile != null && hasValidProfile && profile.friendCode.isBlank()) {
                try {
                    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        val claimedCode = FirebaseManager.claimFriendCode(user.uid)
                        if (claimedCode != null) {
                            val updated = profile.copy(friendCode = claimedCode)
                            repository.saveUserProfile(updated)
                        }
                    }
                } catch (e: Exception) {
                    // Non-fatal -- the Friends screen will simply keep showing
                    // "Your code is being generated" and can be retried next launch.
                }
            }
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
                    ChatMessage("Hi! I'm KardIQ AI. How can I help you?", false)
                )
                _scanResult.value = null
                _coachAdvice.value = null
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
                ChatMessage("Hi! I'm KardIQ AI. How can I help you?", false)
            )
            _scanResult.value = null
            _coachAdvice.value = null
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

    val workoutHistory: StateFlow<List<com.example.data.local.ActivityEvent>> = repository
        .getAllActivityEvents()
        .map { events -> events.filter { it.type == "workout" && it.description.startsWith("Completed workout") } }
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
    
    val metricsHistory = repository.getMetricsHistory(startDateForRange(90)).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Converts a "last N days" window into an actual calendar start date (yyyy-MM-dd).
    // getMetricsHistory used to take a row LIMIT instead of a date range, which meant every
    // range (D/W/M/3M/Y) returned identical results whenever fewer rows existed than the
    // selected range's day count -- the "History Trend" range selector looked broken across
    // every metric detail screen because they all share this one function.
    private fun startDateForRange(days: Int): String {
        return java.time.LocalDate.now().minusDays((days - 1).toLong()).toString()
    }

    fun getMetricsHistoryFlow(days: Int): kotlinx.coroutines.flow.Flow<List<com.example.data.local.DailyMetric>> {
        return repository.getMetricsHistory(startDateForRange(days))
    }

    fun getMetricsHistoryFlowForPeriod(rangeKey: String, periodOffset: Int): kotlinx.coroutines.flow.Flow<List<com.example.data.local.DailyMetric>> {
        val windowDays = when (rangeKey) { "D" -> 1; "W" -> 7; "M" -> 30; "3M" -> 90; "Y" -> 365; else -> 7 }
        val today = java.time.LocalDate.now()
        val endDate = today.minusDays((periodOffset.toLong()) * windowDays)
        val startDate = endDate.minusDays((windowDays - 1).toLong())
        return repository.getMetricsHistoryRange(startDate.toString(), endDate.toString())
    }

    fun getMetricsForDateFlow(date: String): kotlinx.coroutines.flow.Flow<com.example.data.local.DailyMetric?> {
        return repository.getMetricsForDate(date)
    }

    fun getFoodLogsForDateFlow(date: String): kotlinx.coroutines.flow.Flow<List<com.example.data.local.FoodLog>> {
        return repository.getFoodLogsForDate(date)
    }

    fun getActivityEventsForDateFlow(date: String): kotlinx.coroutines.flow.Flow<List<com.example.data.local.ActivityEvent>> {
        val cal = java.util.Calendar.getInstance()
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
        if (parsed != null) cal.time = parsed
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startMillis = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val endMillis = cal.timeInMillis
        return repository.getTodayActivityEvents(startMillis, endMillis)
    }
    
    val recentFoodLogs = repository.getRecentFoodLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private val _updateInfo = MutableStateFlow<com.example.data.repository.UpdateChecker.UpdateInfo?>(null)
    val updateInfo: StateFlow<com.example.data.repository.UpdateChecker.UpdateInfo?> = _updateInfo.asStateFlow()

    fun checkForAppUpdate() {
        viewModelScope.launch {
            val info = com.example.data.repository.UpdateChecker.checkForUpdate(com.example.BuildConfig.VERSION_CODE)
            _updateInfo.value = info
        }
    }

    fun dismissUpdateBanner() {
        _updateInfo.value = null
    }

    init {
        checkForAppUpdate()
    }

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

    private val _glucoseGuidance = MutableStateFlow<String?>(null)
    val glucoseGuidance: StateFlow<String?> = _glucoseGuidance.asStateFlow()

    private val _isLoadingGlucoseGuidance = MutableStateFlow(false)
    val isLoadingGlucoseGuidance: StateFlow<Boolean> = _isLoadingGlucoseGuidance.asStateFlow()

    fun fetchGlucoseGuidance() {
        viewModelScope.launch {
            _isLoadingGlucoseGuidance.value = true
            val profile = userProfile.filterNotNull().first()
            val metrics = getMetricsHistoryFlow(7).first()
            val guidance = repository.generateGlucoseGuidance(profile, metrics)
            _glucoseGuidance.value = guidance
            _isLoadingGlucoseGuidance.value = false
        }
    }

    suspend fun generateGlucoseCsvUri(): android.net.Uri? = withContext(Dispatchers.IO) {
        try {
            val endDate = java.time.LocalDate.now()
            val startDate = endDate.minusDays(29)
            val metricsList = repository.getMetricsHistoryRange(startDate.toString(), endDate.toString()).firstOrNull() ?: emptyList()
            val metricsMap = metricsList.associateBy { it.date }

            val rows = mutableListOf<String>()
            rows.add("Date,Before Breakfast,After Breakfast,Before Lunch,After Lunch,Before Dinner,After Dinner,Specimen Source")

            var curDate = startDate
            while (!curDate.isAfter(endDate)) {
                val dateStr = curDate.toString()
                val m = metricsMap[dateStr]
                val bb = if (m != null && m.bloodGlucoseBeforeBreakfast > 0f) m.bloodGlucoseBeforeBreakfast.toString() else ""
                val ab = if (m != null && m.bloodGlucoseAfterBreakfast > 0f) m.bloodGlucoseAfterBreakfast.toString() else ""
                val bl = if (m != null && m.bloodGlucoseBeforeLunch > 0f) m.bloodGlucoseBeforeLunch.toString() else ""
                val al = if (m != null && m.bloodGlucoseAfterLunch > 0f) m.bloodGlucoseAfterLunch.toString() else ""
                val bd = if (m != null && m.bloodGlucoseBeforeDinner > 0f) m.bloodGlucoseBeforeDinner.toString() else ""
                val ad = if (m != null && m.bloodGlucoseAfterDinner > 0f) m.bloodGlucoseAfterDinner.toString() else ""
                val source = if (m != null && m.bloodGlucoseSpecimenSource.isNotBlank() && m.bloodGlucoseSpecimenSource != "Not set") {
                    m.bloodGlucoseSpecimenSource
                } else {
                    ""
                }
                rows.add("$dateStr,$bb,$ab,$bl,$al,$bd,$ad,$source")
                curDate = curDate.plusDays(1)
            }

            val csvContent = rows.joinToString("\n")
            val file = java.io.File(getApplication<Application>().cacheDir, "glucose_template.csv")
            file.writeText(csvContent)

            androidx.core.content.FileProvider.getUriForFile(
                getApplication(),
                "${getApplication<Application>().packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }

    data class GlucoseCsvImportResult(val daysImported: Int, val daysSkippedAlreadyLogged: Int, val rowsSkippedInvalid: Int)

    suspend fun importGlucoseCsv(uri: android.net.Uri): GlucoseCsvImportResult = withContext(Dispatchers.IO) {
        var daysImported = 0
        var daysSkippedAlreadyLogged = 0
        var rowsSkippedInvalid = 0
        try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return@withContext GlucoseCsvImportResult(0, 0, 1)
            }
            val lines = inputStream.bufferedReader().use { it.readLines() }
            val dataLines = lines.drop(1).filter { it.isNotBlank() }

            for (line in dataLines) {
                val parts = line.split(",")
                if (parts.size != 8) {
                    rowsSkippedInvalid++
                    continue
                }
                val dateStr = parts[0].trim()
                try {
                    java.time.LocalDate.parse(dateStr)
                } catch (e: Exception) {
                    rowsSkippedInvalid++
                    continue
                }

                val existing = repository.getMetricsForDate(dateStr).firstOrNull()
                val hasExistingGlucose = existing != null && listOf(
                    existing.bloodGlucoseMorning,
                    existing.bloodGlucoseNight,
                    existing.bloodGlucoseBeforeBreakfast,
                    existing.bloodGlucoseAfterBreakfast,
                    existing.bloodGlucoseBeforeLunch,
                    existing.bloodGlucoseAfterLunch,
                    existing.bloodGlucoseBeforeDinner,
                    existing.bloodGlucoseAfterDinner
                ).any { it > 0f }

                if (hasExistingGlucose) {
                    daysSkippedAlreadyLogged++
                    continue
                }

                val specimenSourceRaw = parts[7].trim()
                val specimenSource = if (specimenSourceRaw.isNotBlank() && specimenSourceRaw != "Not set") specimenSourceRaw else "Not set"

                val bb = parts[1].trim().takeIf { it.isNotBlank() }?.toFloatOrNull()
                val ab = parts[2].trim().takeIf { it.isNotBlank() }?.toFloatOrNull()
                val bl = parts[3].trim().takeIf { it.isNotBlank() }?.toFloatOrNull()
                val al = parts[4].trim().takeIf { it.isNotBlank() }?.toFloatOrNull()
                val bd = parts[5].trim().takeIf { it.isNotBlank() }?.toFloatOrNull()
                val ad = parts[6].trim().takeIf { it.isNotBlank() }?.toFloatOrNull()

                val anyValue = listOf(bb, ab, bl, al, bd, ad).any { it != null }
                if (anyValue) {
                    saveGlucoseReadingsForDate(dateStr, bb, ab, bl, al, bd, ad, specimenSource)
                    daysImported++
                }
            }
        } catch (e: Exception) {
            // Return accumulated counts on unexpected exception
        }
        GlucoseCsvImportResult(daysImported, daysSkippedAlreadyLogged, rowsSkippedInvalid)
    }

    suspend fun saveGlucoseReadingsForDate(
        date: String,
        beforeBreakfast: Float? = null,
        afterBreakfast: Float? = null,
        beforeLunch: Float? = null,
        afterLunch: Float? = null,
        beforeDinner: Float? = null,
        afterDinner: Float? = null,
        specimenSource: String = "Not set"
    ) {
        val current = if (date == todayDateString) {
            todayMetrics.value ?: DailyMetric(date = date)
        } else {
            repository.getMetricsForDate(date).firstOrNull() ?: DailyMetric(date = date)
        }
        val updated = current.copy(
            bloodGlucoseBeforeBreakfast = beforeBreakfast ?: current.bloodGlucoseBeforeBreakfast,
            bloodGlucoseAfterBreakfast = afterBreakfast ?: current.bloodGlucoseAfterBreakfast,
            bloodGlucoseBeforeLunch = beforeLunch ?: current.bloodGlucoseBeforeLunch,
            bloodGlucoseAfterLunch = afterLunch ?: current.bloodGlucoseAfterLunch,
            bloodGlucoseBeforeDinner = beforeDinner ?: current.bloodGlucoseBeforeDinner,
            bloodGlucoseAfterDinner = afterDinner ?: current.bloodGlucoseAfterDinner,
            bloodGlucoseSpecimenSource = specimenSource
        )
        repository.saveMetrics(updated)
        repository.checkAndAwardBadges(updated)
        repository.logActivityEvent("glucose", "Logged blood glucose reading(s) for $date")
        beforeBreakfast?.let { writeGlucoseToHealthConnect(it, MealType.MEAL_TYPE_BREAKFAST, BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL, specimenSource, date) }
        afterBreakfast?.let { writeGlucoseToHealthConnect(it, MealType.MEAL_TYPE_BREAKFAST, BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL, specimenSource, date) }
        beforeLunch?.let { writeGlucoseToHealthConnect(it, MealType.MEAL_TYPE_LUNCH, BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL, specimenSource, date) }
        afterLunch?.let { writeGlucoseToHealthConnect(it, MealType.MEAL_TYPE_LUNCH, BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL, specimenSource, date) }
        beforeDinner?.let { writeGlucoseToHealthConnect(it, MealType.MEAL_TYPE_DINNER, BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL, specimenSource, date) }
        afterDinner?.let { writeGlucoseToHealthConnect(it, MealType.MEAL_TYPE_DINNER, BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL, specimenSource, date) }
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

    val frequentFoods: StateFlow<List<com.example.data.local.FoodLog>> = allFoodLogs
        .map { logs ->
            logs.groupBy { it.name }
                .values
                .map { group -> group.maxByOrNull { it.id }!! to group.size }
                .sortedByDescending { it.second }
                .take(8)
                .map { it.first }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Hello! I am KardIQ AI, your universal health bot. Ask me anything about fitness, wellness, nutrition, or lifestyle!", false))
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
                val parsedCarbs = json.optDouble("carbs", 0.0).toFloat()
                val parsedProtein = json.optDouble("protein", 0.0).toFloat()
                val parsedFat = json.optDouble("fat", 0.0).toFloat()
                val parsedSodium = json.optDouble("sodium", 0.0).toFloat()
                val parsedSugar = json.optDouble("sugar", 0.0).toFloat()
                val parsedFiber = json.optDouble("fiber", 0.0).toFloat()
                val parsedDescription = json.optString("description", "")
                if (parsedCalories > 0) {
                    logScannedFood(
                        parsedName, parsedCategory, parsedCalories, parsedDescription,
                        mealType = mealType,
                        carbsG = parsedCarbs, proteinG = parsedProtein, fatG = parsedFat,
                        sodiumMg = parsedSodium, sugarG = parsedSugar, fiberG = parsedFiber
                    )
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

    fun lookupBarcode(barcode: String) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanResult.value = null
            val result = repository.lookupBarcodeProduct(barcode)
            _scanResult.value = result
            _isScanning.value = false
        }
    }

    fun setScanResultDirect(json: String) {
        _scanResult.value = json
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
            val joined = slots.joinToString(",")
            val updated = current.copy(todayTileSlots = joined)
            repository.saveUserProfile(updated)
        }
    }

    fun recalculateAutoGoals(profile: UserProfile): UserProfile {
        return profile.copy(
            stepGoal = if (profile.stepGoalIsAuto) com.example.data.health.HealthGoalCalculator.calculateStepGoal(profile) else profile.stepGoal,
            sleepGoalHours = if (profile.sleepGoalIsAuto) com.example.data.health.HealthGoalCalculator.calculateSleepGoalHours(profile) else profile.sleepGoalHours,
            dailyWaterLimitLiters = if (profile.waterGoalIsAuto) com.example.data.health.HealthGoalCalculator.calculateWaterGoalLiters(profile) else profile.dailyWaterLimitLiters,
            dailyCalorieLimit = if (profile.calorieGoalIsAuto) com.example.data.health.HealthGoalCalculator.calculateCalorieGoal(profile) else profile.dailyCalorieLimit
        )
    }

    fun acceptPrivacyPolicy() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userProfile.value ?: return@launch
            saveProfile(current.copy(hasAcceptedPrivacyPolicy = true))
        }
    }

    suspend fun saveProfile(profile: UserProfile): Boolean {
        return try {
            val updatedProfile = recalculateAutoGoals(profile)
            repository.saveUserProfile(updatedProfile)
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

    fun addWater(amountLiters: Float, onHealthConnectSyncResult: (Boolean) -> Unit = {}) {
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
                    onHealthConnectSyncResult(true)
                } else {
                    onHealthConnectSyncResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onHealthConnectSyncResult(false)
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

    suspend fun saveCompletedMindfulnessSession(
        sessionType: Int,
        startTime: Instant,
        endTime: Instant
    ) = withContext(Dispatchers.IO) {
        val sessionKey = "${sessionType}_${startTime.toEpochMilli()}"
        if (savedMindfulnessSessionKeys.contains(sessionKey)) {
            return@withContext
        }
        savedMindfulnessSessionKeys.add(sessionKey)

        // 1. Save locally first:
        // Increment (never overwrite) today's DailyMetric.mindfulnessMinutes by the real elapsed minutes between startTime and endTime
        val durationSeconds = if (endTime.isAfter(startTime)) {
            java.time.Duration.between(startTime, endTime).seconds.toInt().coerceAtLeast(0)
        } else {
            0
        }
        val elapsedMinutes = (durationSeconds / 60).coerceAtLeast(if (durationSeconds > 0) 1 else 0)

        val current = todayMetrics.value ?: repository.getMetricsForDate(todayDateString).firstOrNull() ?: DailyMetric(date = todayDateString)
        val updated = current.copy(
            mindfulnessMinutes = (current.mindfulnessMinutes + elapsedMinutes).coerceAtLeast(0)
        )
        repository.saveMetrics(updated)
        repository.checkAndAwardBadges(updated)

        val sessionTitle = when (sessionType) {
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION -> "Meditation"
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING -> "Breathing"
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT -> "Mindful Movement"
            else -> "Mindfulness"
        }
        val logMsg = "Completed $sessionTitle session (${elapsedMinutes}m)"
        repository.logActivityEvent("mindfulness", logMsg)

        // 2. Health Connect write in its own try-catch (can never block or fail the local save)
        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
            val effectiveEndTime = if (endTime.isAfter(startTime)) endTime else startTime.plusSeconds(durationSeconds.toLong().coerceAtLeast(1L))
            val startOffset = ZoneId.systemDefault().rules.getOffset(startTime)
            val endOffset = ZoneId.systemDefault().rules.getOffset(effectiveEndTime)
            val mindfulnessRecord = MindfulnessSessionRecord(
                startTime = startTime,
                startZoneOffset = startOffset,
                endTime = effectiveEndTime,
                endZoneOffset = endOffset,
                mindfulnessSessionType = sessionType,
                title = sessionTitle
            )
            healthConnectClient.insertRecords(listOf(mindfulnessRecord))
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
    
    fun setBloodPressure(
        systolic: Int,
        diastolic: Int,
        bodyPosition: String = "Not set",
        armLocation: String = "Not set",
        onHealthConnectSyncResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val value = "$systolic/$diastolic"
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(
                bloodPressure = value,
                bloodPressureBodyPosition = bodyPosition,
                bloodPressureArmLocation = armLocation
            )
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("blood_pressure", "Logged blood pressure: ${value}")

            // Write to Health Connect
            try {
                val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
                val now = Instant.now()
                val zoneOffset = ZoneId.systemDefault().rules.getOffset(now)
                val bpPosition = when (bodyPosition) {
                    "Standing" -> BloodPressureRecord.BODY_POSITION_STANDING_UP
                    "Sitting" -> BloodPressureRecord.BODY_POSITION_SITTING_DOWN
                    "Lying down" -> BloodPressureRecord.BODY_POSITION_LYING_DOWN
                    "Reclining" -> BloodPressureRecord.BODY_POSITION_RECLINING
                    else -> BloodPressureRecord.BODY_POSITION_UNKNOWN
                }
                val bpLocation = when (armLocation) {
                    "Left wrist" -> BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_WRIST
                    "Right wrist" -> BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_WRIST
                    "Left upper arm" -> BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM
                    "Right upper arm" -> BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM
                    else -> BloodPressureRecord.MEASUREMENT_LOCATION_UNKNOWN
                }
                val bpRecord = BloodPressureRecord(
                    time = now,
                    zoneOffset = zoneOffset,
                    systolic = Pressure.millimetersOfMercury(systolic.toDouble()),
                    diastolic = Pressure.millimetersOfMercury(diastolic.toDouble()),
                    bodyPosition = bpPosition,
                    measurementLocation = bpLocation
                )
                healthConnectClient.insertRecords(listOf(bpRecord))
                onHealthConnectSyncResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onHealthConnectSyncResult(false)
            }
        }
    }

    private suspend fun writeGlucoseToHealthConnect(
        value: Float,
        mealType: Int,
        relationToMeal: Int,
        specimenSource: String,
        date: String
    ) {
        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
            val recordInstant = if (date == todayDateString) {
                Instant.now()
            } else {
                java.time.LocalDate.parse(date).atTime(java.time.LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant()
            }
            val zoneOffset = ZoneId.systemDefault().rules.getOffset(recordInstant)
            val source = when (specimenSource) {
                "Interstitial fluid" -> BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID
                "Capillary blood" -> BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD
                "Plasma" -> BloodGlucoseRecord.SPECIMEN_SOURCE_PLASMA
                "Serum" -> BloodGlucoseRecord.SPECIMEN_SOURCE_SERUM
                "Tears" -> BloodGlucoseRecord.SPECIMEN_SOURCE_TEARS
                "Whole blood" -> BloodGlucoseRecord.SPECIMEN_SOURCE_WHOLE_BLOOD
                else -> BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN
            }
            val record = BloodGlucoseRecord(
                time = recordInstant,
                zoneOffset = zoneOffset,
                level = BloodGlucose.millimolesPerLiter(value.toDouble()),
                specimenSource = source,
                mealType = mealType,
                relationToMeal = relationToMeal
            )
            healthConnectClient.insertRecords(listOf(record))
        } catch (e: Exception) {
            // Silently ignore — matches setBloodPressure's existing error-handling convention
        }
    }

    fun setBloodGlucoseBeforeBreakfast(value: Float, specimenSource: String = "Not set", date: String = todayDateString) {
        viewModelScope.launch {
            val current = if (date == todayDateString) {
                todayMetrics.value ?: DailyMetric(date = date)
            } else {
                repository.getMetricsForDate(date).firstOrNull() ?: DailyMetric(date = date)
            }
            val updated = current.copy(bloodGlucoseBeforeBreakfast = value, bloodGlucoseSpecimenSource = specimenSource)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("glucose", "Logged blood glucose (before breakfast): ${value} mmol/L")
            writeGlucoseToHealthConnect(value, MealType.MEAL_TYPE_BREAKFAST, BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL, specimenSource, date)
        }
    }

    fun setBloodGlucoseAfterBreakfast(value: Float, specimenSource: String = "Not set", date: String = todayDateString) {
        viewModelScope.launch {
            val current = if (date == todayDateString) {
                todayMetrics.value ?: DailyMetric(date = date)
            } else {
                repository.getMetricsForDate(date).firstOrNull() ?: DailyMetric(date = date)
            }
            val updated = current.copy(bloodGlucoseAfterBreakfast = value, bloodGlucoseSpecimenSource = specimenSource)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("glucose", "Logged blood glucose (after breakfast): ${value} mmol/L")
            writeGlucoseToHealthConnect(value, MealType.MEAL_TYPE_BREAKFAST, BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL, specimenSource, date)
        }
    }

    fun setBloodGlucoseBeforeLunch(value: Float, specimenSource: String = "Not set", date: String = todayDateString) {
        viewModelScope.launch {
            val current = if (date == todayDateString) {
                todayMetrics.value ?: DailyMetric(date = date)
            } else {
                repository.getMetricsForDate(date).firstOrNull() ?: DailyMetric(date = date)
            }
            val updated = current.copy(bloodGlucoseBeforeLunch = value, bloodGlucoseSpecimenSource = specimenSource)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("glucose", "Logged blood glucose (before lunch): ${value} mmol/L")
            writeGlucoseToHealthConnect(value, MealType.MEAL_TYPE_LUNCH, BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL, specimenSource, date)
        }
    }

    fun setBloodGlucoseAfterLunch(value: Float, specimenSource: String = "Not set", date: String = todayDateString) {
        viewModelScope.launch {
            val current = if (date == todayDateString) {
                todayMetrics.value ?: DailyMetric(date = date)
            } else {
                repository.getMetricsForDate(date).firstOrNull() ?: DailyMetric(date = date)
            }
            val updated = current.copy(bloodGlucoseAfterLunch = value, bloodGlucoseSpecimenSource = specimenSource)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("glucose", "Logged blood glucose (after lunch): ${value} mmol/L")
            writeGlucoseToHealthConnect(value, MealType.MEAL_TYPE_LUNCH, BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL, specimenSource, date)
        }
    }

    fun setBloodGlucoseBeforeDinner(value: Float, specimenSource: String = "Not set", date: String = todayDateString) {
        viewModelScope.launch {
            val current = if (date == todayDateString) {
                todayMetrics.value ?: DailyMetric(date = date)
            } else {
                repository.getMetricsForDate(date).firstOrNull() ?: DailyMetric(date = date)
            }
            val updated = current.copy(bloodGlucoseBeforeDinner = value, bloodGlucoseSpecimenSource = specimenSource)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("glucose", "Logged blood glucose (before dinner): ${value} mmol/L")
            writeGlucoseToHealthConnect(value, MealType.MEAL_TYPE_DINNER, BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL, specimenSource, date)
        }
    }

    fun setBloodGlucoseAfterDinner(value: Float, specimenSource: String = "Not set", date: String = todayDateString) {
        viewModelScope.launch {
            val current = if (date == todayDateString) {
                todayMetrics.value ?: DailyMetric(date = date)
            } else {
                repository.getMetricsForDate(date).firstOrNull() ?: DailyMetric(date = date)
            }
            val updated = current.copy(bloodGlucoseAfterDinner = value, bloodGlucoseSpecimenSource = specimenSource)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
            repository.logActivityEvent("glucose", "Logged blood glucose (after dinner): ${value} mmol/L")
            writeGlucoseToHealthConnect(value, MealType.MEAL_TYPE_DINNER, BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL, specimenSource, date)
        }
    }
    
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    @OptIn(ExperimentalFeatureAvailabilityApi::class)
    fun syncWithHealthConnect(context: Context, showRefreshIndicator: Boolean = true) {
        viewModelScope.launch {
            if (showRefreshIndicator) _isSyncing.value = true
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
                        bloodGlucose = latest.level.inMillimolesPerLiter.toFloat()
                    }
                }

                // 5. Latest Heart Rate + daily min/max
                var heartRate = 0
                var heartRateMin = 0
                var heartRateMax = 0
                val hrResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter)
                )
                if (hrResponse.records.isNotEmpty()) {
                    val latestRecord = hrResponse.records.maxByOrNull { it.startTime }
                    if (latestRecord != null && latestRecord.samples.isNotEmpty()) {
                        heartRate = latestRecord.samples.last().beatsPerMinute.toInt()
                    }
                    val allBpm = hrResponse.records.flatMap { it.samples }.map { it.beatsPerMinute.toInt() }.filter { it > 0 }
                    if (allBpm.isNotEmpty()) {
                        heartRateMin = allBpm.min()
                        heartRateMax = allBpm.max()
                    }
                }

                // 5b. Latest Resting Heart Rate
                var restingHeartRateValue = 0
                try {
                    val restingHrResponse = healthConnectClient.readRecords(
                        ReadRecordsRequest(RestingHeartRateRecord::class, timeRangeFilter)
                    )
                    if (restingHrResponse.records.isNotEmpty()) {
                        val latestRestingRecord = restingHrResponse.records.maxByOrNull { it.time }
                        if (latestRestingRecord != null) {
                            restingHeartRateValue = latestRestingRecord.beatsPerMinute.toInt()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
                    heartRateMin = if (heartRateMin > 0) heartRateMin else current.heartRateMin,
                    heartRateMax = if (heartRateMax > 0) heartRateMax else current.heartRateMax,
                    restingHeartRate = if (restingHeartRateValue > 0) restingHeartRateValue else current.restingHeartRate,
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
            } finally {
                if (showRefreshIndicator) _isSyncing.value = false
            }
        }
    }

    @OptIn(ExperimentalFeatureAvailabilityApi::class)
    suspend fun getHeartRateSamplesForDate(date: java.time.LocalDate): List<Pair<java.time.Instant, Int>> {
        return try {
            if (HealthConnectClient.getSdkStatus(getApplication()) != HealthConnectClient.SDK_AVAILABLE) {
                return emptyList()
            }
            val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
            val zoneId = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zoneId).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant()
            val timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            val hrResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter)
            )
            val samples = mutableListOf<Pair<java.time.Instant, Int>>()
            for (record in hrResponse.records) {
                for (sample in record.samples) {
                    samples.add(sample.time to sample.beatsPerMinute.toInt())
                }
            }
            samples.sortBy { it.first }
            samples
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getHourlyStepsForDate(date: java.time.LocalDate): List<Pair<java.time.Instant, Int>> {
        return try {
            if (HealthConnectClient.getSdkStatus(getApplication()) != HealthConnectClient.SDK_AVAILABLE) {
                return emptyList()
            }
            val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
            val zoneId = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zoneId).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant()
            val response = healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                    timeRangeSlicer = java.time.Duration.ofHours(1)
                )
            )
            response.map { bucket ->
                bucket.startTime to (bucket.result[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getHourlyActiveCaloriesForDate(date: java.time.LocalDate): List<Pair<java.time.Instant, Int>> {
        return try {
            if (HealthConnectClient.getSdkStatus(getApplication()) != HealthConnectClient.SDK_AVAILABLE) {
                return emptyList()
            }
            val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
            val zoneId = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zoneId).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant()
            val response = healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                    timeRangeSlicer = java.time.Duration.ofHours(1)
                )
            )
            response.map { bucket ->
                bucket.startTime to (bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt() ?: 0)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getHourlyWaterForDate(date: java.time.LocalDate): List<Pair<java.time.Instant, Float>> {
        return try {
            if (HealthConnectClient.getSdkStatus(getApplication()) != HealthConnectClient.SDK_AVAILABLE) {
                return emptyList()
            }
            val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
            val zoneId = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zoneId).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant()
            val response = healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                    timeRangeSlicer = java.time.Duration.ofHours(1)
                )
            )
            response.map { bucket ->
                bucket.startTime to (bucket.result[HydrationRecord.VOLUME_TOTAL]?.inLiters?.toFloat() ?: 0f)
            }
        } catch (e: Exception) {
            emptyList()
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

    private val _healthInsight = MutableStateFlow<String?>(null)
    val healthInsight: StateFlow<String?> = _healthInsight.asStateFlow()

    fun checkAndGenerateHealthInsight(context: android.content.Context) {
        viewModelScope.launch {
            val sharedPrefs = context.getSharedPreferences("ShasthoPrefs", android.content.Context.MODE_PRIVATE)
            val lastCheck = sharedPrefs.getLong("last_health_insight_check", 0L)
            val cachedText = sharedPrefs.getString("cached_health_insight_text", null)
            val now = System.currentTimeMillis()
            if (now - lastCheck <= 86400000L && cachedText != null) {
                _healthInsight.value = cachedText
                return@launch
            }
            val profile = userProfile.filterNotNull().first()
            val metrics = getMetricsHistoryFlow(7).first()
            val foodLogs = repository.getRecentFoodLogs().first()
            val insight = repository.generateHealthInsight(profile, metrics, foodLogs)
            _healthInsight.value = insight
            sharedPrefs.edit()
                .putLong("last_health_insight_check", now)
                .putString("cached_health_insight_text", insight)
                .apply()
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
        mealType: String = "Snack",
        carbsG: Float = 0f,
        proteinG: Float = 0f,
        fatG: Float = 0f,
        sodiumMg: Float = 0f,
        sugarG: Float = 0f,
        fiberG: Float = 0f
    ) {
        viewModelScope.launch {
            val foodLog = com.example.data.local.FoodLog(
                date = todayDateString,
                name = name,
                category = category,
                calories = calories,
                description = description,
                time = time,
                mealType = mealType,
                carbsG = carbsG,
                proteinG = proteinG,
                fatG = fatG,
                sodiumMg = sodiumMg,
                sugarG = sugarG,
                fiberG = fiberG
            )
            repository.saveFoodLog(foodLog)

            // Also add calories to today's metrics
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(
                caloriesConsumed = current.caloriesConsumed + calories,
                carbsG = current.carbsG + carbsG,
                proteinG = current.proteinG + proteinG,
                fatG = current.fatG + fatG
            )
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

    fun relogFood(log: com.example.data.local.FoodLog) {
        logScannedFood(
            name = log.name,
            category = log.category,
            calories = log.calories,
            description = log.description,
            mealType = log.mealType,
            carbsG = log.carbsG,
            proteinG = log.proteinG,
            fatG = log.fatG,
            sodiumMg = log.sodiumMg,
            sugarG = log.sugarG,
            fiberG = log.fiberG
        )
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

    private val _foodChatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val foodChatHistory: StateFlow<List<ChatMessage>> = _foodChatHistory.asStateFlow()

    private val _isLoadingFoodChat = MutableStateFlow(false)
    val isLoadingFoodChat: StateFlow<Boolean> = _isLoadingFoodChat.asStateFlow()

    private val _foodChatLoggedConfirmation = MutableStateFlow<String?>(null)
    val foodChatLoggedConfirmation: StateFlow<String?> = _foodChatLoggedConfirmation.asStateFlow()

    private val _pendingFoodLogEntry = MutableStateFlow<PendingFoodLogEntry?>(null)
    val pendingFoodLogEntry: StateFlow<PendingFoodLogEntry?> = _pendingFoodLogEntry.asStateFlow()

    fun confirmPendingFoodLog() {
        val entry = _pendingFoodLogEntry.value ?: return
        logScannedFood(
            name = entry.name,
            category = entry.category,
            calories = entry.calories,
            description = entry.description,
            mealType = entry.mealType,
            carbsG = entry.carbsG,
            proteinG = entry.proteinG,
            fatG = entry.fatG,
            sodiumMg = entry.sodiumMg,
            sugarG = entry.sugarG,
            fiberG = entry.fiberG
        )
        _foodChatLoggedConfirmation.value = "${entry.name} logged -- ${entry.calories} kcal"
        _pendingFoodLogEntry.value = null
    }

    fun discardPendingFoodLog() {
        _pendingFoodLogEntry.value = null
    }

    fun clearFoodChatLoggedConfirmation() {
        _foodChatLoggedConfirmation.value = null
    }

    fun resetFoodChat() {
        _foodChatHistory.value = emptyList()
        _pendingFoodLogEntry.value = null
    }

    data class PendingFoodLogEntry(
        val name: String,
        val category: String,
        val calories: Int,
        val description: String,
        val mealType: String,
        val carbsG: Float,
        val proteinG: Float,
        val fatG: Float,
        val sodiumMg: Float,
        val sugarG: Float,
        val fiberG: Float
    )

    fun sendFoodChatMessage(message: String) {
        viewModelScope.launch {
            val newUserMsg = ChatMessage(message, true)
            _foodChatHistory.value = _foodChatHistory.value + newUserMsg
            _isLoadingFoodChat.value = true

            val placeholderIndex = _foodChatHistory.value.size
            _foodChatHistory.value = _foodChatHistory.value + ChatMessage("", false)

            var accumulated = ""
            try {
                repository.getFoodChatResponseStream(_foodChatHistory.value.dropLast(1)).collect { chunk ->
                    accumulated += chunk
                    val displayText = accumulated.substringBefore("READY_TO_LOG").trim()
                    _foodChatHistory.value = _foodChatHistory.value.toMutableList().also {
                        it[placeholderIndex] = ChatMessage(displayText, false)
                    }
                }

                if (accumulated.contains("READY_TO_LOG")) {
                    val jsonPart = accumulated.substringAfter("READY_TO_LOG").trim()
                    try {
                        val json = org.json.JSONObject(jsonPart)
                        _pendingFoodLogEntry.value = PendingFoodLogEntry(
                            name = json.optString("name", "Food"),
                            category = json.optString("category", "Meal"),
                            calories = json.optInt("calories", 0),
                            description = json.optString("description", ""),
                            mealType = json.optString("mealType", "Snack"),
                            carbsG = json.optDouble("carbs", 0.0).toFloat(),
                            proteinG = json.optDouble("protein", 0.0).toFloat(),
                            fatG = json.optDouble("fat", 0.0).toFloat(),
                            sodiumMg = json.optDouble("sodium", 0.0).toFloat(),
                            sugarG = json.optDouble("sugar", 0.0).toFloat(),
                            fiberG = json.optDouble("fiber", 0.0).toFloat()
                        )
                    } catch (e: Exception) {
                        // Model didn't return valid JSON this turn -- leave the chat text as-is, no log happens.
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FoodChat", "sendFoodChatMessage failed", e)
                _foodChatHistory.value = _foodChatHistory.value.toMutableList().also {
                    it[placeholderIndex] = ChatMessage("Error: ${e.message ?: e.toString()}", false)
                }
            }
            _isLoadingFoodChat.value = false
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

    private val _structuredWorkoutPlan = MutableStateFlow<com.example.data.model.WorkoutPlan?>(null)
    val structuredWorkoutPlan: StateFlow<com.example.data.model.WorkoutPlan?> = _structuredWorkoutPlan.asStateFlow()

    private val _rawStructuredWorkoutJson = MutableStateFlow<String>("")
    val rawStructuredWorkoutJson: StateFlow<String> = _rawStructuredWorkoutJson.asStateFlow()

    private val _isLoadingStructuredWorkout = MutableStateFlow(false)
    val isLoadingStructuredWorkout: StateFlow<Boolean> = _isLoadingStructuredWorkout.asStateFlow()

    private val _structuredWorkoutError = MutableStateFlow<String?>(null)
    val structuredWorkoutError: StateFlow<String?> = _structuredWorkoutError.asStateFlow()
    
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

    suspend fun getExerciseLibrary(context: android.content.Context): List<com.example.data.local.LibraryExercise> {
        return repository.getExerciseLibrary(context)
    }

    private val _incomingFriendRequests = MutableStateFlow<List<FirebaseManager.FriendRequestInfo>>(emptyList())
    val incomingFriendRequests: StateFlow<List<FirebaseManager.FriendRequestInfo>> = _incomingFriendRequests.asStateFlow()

    private val _outgoingFriendRequests = MutableStateFlow<List<FirebaseManager.FriendRequestInfo>>(emptyList())
    val outgoingFriendRequests: StateFlow<List<FirebaseManager.FriendRequestInfo>> = _outgoingFriendRequests.asStateFlow()

    private val _friendActionMessage = MutableStateFlow<String?>(null)
    val friendActionMessage: StateFlow<String?> = _friendActionMessage.asStateFlow()

    fun clearFriendActionMessage() {
        _friendActionMessage.value = null
    }

    fun refreshFriendRequests() {
        viewModelScope.launch(Dispatchers.IO) {
            val (incoming, outgoing) = FirebaseManager.getFriendRequests()
            _incomingFriendRequests.value = incoming
            _outgoingFriendRequests.value = outgoing
        }
    }

    fun sendFriendRequestByCode(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = code.trim().uppercase()
            if (trimmed.isBlank()) {
                _friendActionMessage.value = "Enter a friend code."
                return@launch
            }
            val targetUid = FirebaseManager.resolveFriendCode(trimmed)
            if (targetUid == null) {
                _friendActionMessage.value = "No user found with that code."
                return@launch
            }
            val result = FirebaseManager.sendFriendRequest(targetUid)
            _friendActionMessage.value = result
            refreshFriendRequests()
        }
    }

    fun acceptFriendRequest(pairId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            FirebaseManager.acceptFriendRequest(pairId)
            refreshFriendRequests()
        }
    }

    fun declineOrCancelFriendRequest(pairId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            FirebaseManager.deleteFriendRequest(pairId)
            refreshFriendRequests()
        }
    }

    private val _kudosSentTo = MutableStateFlow<Set<String>>(emptySet())
    val kudosSentTo: StateFlow<Set<String>> = _kudosSentTo.asStateFlow()

    fun sendKudosToFriend(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sent = FirebaseManager.sendKudos(uid)
            if (sent) {
                _kudosSentTo.value = _kudosSentTo.value + uid
            }
        }
    }

    fun checkKudosSentToday(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (FirebaseManager.hasSentKudosToday(uid)) {
                _kudosSentTo.value = _kudosSentTo.value + uid
            }
        }
    }

    fun removeFriend(pairId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            FirebaseManager.removeFriend(pairId)
            fetchFriendsList()
        }
    }

    private val _leaderboard = MutableStateFlow<List<FirebaseManager.LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<FirebaseManager.LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _friendsList = MutableStateFlow<List<FirebaseManager.FriendInfo>>(emptyList())
    val friendsList: StateFlow<List<FirebaseManager.FriendInfo>> = _friendsList.asStateFlow()

    private val _selectedFriendStats = MutableStateFlow<FirebaseManager.FriendStatsInfo?>(null)
    val selectedFriendStats: StateFlow<FirebaseManager.FriendStatsInfo?> = _selectedFriendStats.asStateFlow()

    fun fetchLeaderboard() {
        viewModelScope.launch(Dispatchers.IO) {
            _leaderboard.value = FirebaseManager.getLeaderboard()
        }
    }

    fun fetchFriendsList() {
        viewModelScope.launch(Dispatchers.IO) {
            _friendsList.value = FirebaseManager.getAcceptedFriends()
        }
    }

    fun fetchFriendStats(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedFriendStats.value = FirebaseManager.getFriendStats(uid)
        }
    }

    fun clearSelectedFriendStats() {
        _selectedFriendStats.value = null
    }

    private val _challenges = MutableStateFlow<List<FirebaseManager.ChallengeInfo>>(emptyList())
    val challenges: StateFlow<List<FirebaseManager.ChallengeInfo>> = _challenges.asStateFlow()

    private val _challengeActionMessage = MutableStateFlow<String?>(null)
    val challengeActionMessage: StateFlow<String?> = _challengeActionMessage.asStateFlow()

    fun clearChallengeActionMessage() {
        _challengeActionMessage.value = null
    }

    fun createChallengeWithFriend(targetUid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val challengeId = FirebaseManager.createChallenge(targetUid)
            _challengeActionMessage.value = if (challengeId != null) "Challenge sent!" else "Couldn't create challenge -- try again."
            fetchChallenges()
        }
    }

    fun fetchChallenges() {
        viewModelScope.launch(Dispatchers.IO) {
            val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val list = FirebaseManager.getMyChallenges()
            for (challenge in list) {
                if (challenge.status != "active") continue
                val mySteps = repository.getMetricsHistoryRange(challenge.startDate, challenge.endDate).firstOrNull()
                    ?.sumOf { it.steps } ?: 0
                FirebaseManager.updateMyChallengeProgress(challenge.challengeId, mySteps.toLong())
                val winnerUid = FirebaseManager.completeChallengeIfDue(
                    challenge.challengeId, challenge.endDate, mySteps.toLong(), challenge.otherProgress, myUid, challenge.otherUid
                )
                if (winnerUid == myUid) {
                    repository.awardChallengeBonus(150, "Challenge Champion")
                }
            }
            _challenges.value = FirebaseManager.getMyChallenges()
        }
    }

    private val _activityFeed = MutableStateFlow<List<FirebaseManager.ActivityFeedEntry>>(emptyList())
    val activityFeed: StateFlow<List<FirebaseManager.ActivityFeedEntry>> = _activityFeed.asStateFlow()

    fun fetchActivityFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            _activityFeed.value = FirebaseManager.getActivityFeed()
        }
    }

    private var messagesJob: kotlinx.coroutines.Job? = null
    private val _messages = MutableStateFlow<List<FirebaseManager.DirectMessage>>(emptyList())
    val messages: StateFlow<List<FirebaseManager.DirectMessage>> = _messages.asStateFlow()

    fun startObservingMessages(pairId: String, friendName: String? = null) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            FirebaseManager.observeMessages(pairId).collect { list ->
                _messages.value = list
            }
        }
    }

    fun stopObservingMessages() {
        messagesJob?.cancel()
        _messages.value = emptyList()
    }

    private val _dmErrorMessage = MutableStateFlow<String?>(null)
    val dmErrorMessage: StateFlow<String?> = _dmErrorMessage.asStateFlow()

    fun clearDmErrorMessage() {
        _dmErrorMessage.value = null
    }

    fun sendMessage(pairId: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val error = FirebaseManager.sendDirectMessage(pairId, text)
            if (error != null) {
                _dmErrorMessage.value = error
            }
        }
    }

    companion object {
        fun calculateResilienceScore(metricsHistory: List<DailyMetric>): ResilienceResult {
            val recent7Days = metricsHistory.take(7)
            val recentSleepDay = recent7Days.firstOrNull { it.sleepHours > 0f }

            val sleepScore = recentSleepDay?.let {
                (100f - kotlin.math.abs(it.sleepHours - 8f) * 20f).coerceIn(0f, 100f)
            }

            val hrvScore = run {
                val mostRecentHrvDay = recent7Days.firstOrNull { it.heartRateVariability > 0f }
                if (mostRecentHrvDay != null) {
                    val todayHrv = mostRecentHrvDay.heartRateVariability
                    val priorHrvDays = metricsHistory.filter { it.date < mostRecentHrvDay.date && it.heartRateVariability > 0f }.take(7)
                    if (priorHrvDays.size >= 3) {
                        val avgHrv = priorHrvDays.map { it.heartRateVariability }.average().toFloat()
                        if (avgHrv > 0f) {
                            (50f + ((todayHrv - avgHrv) / avgHrv) * 200f).coerceIn(0f, 100f)
                        } else null
                    } else null
                } else null
            }

            val resilienceScore = when {
                sleepScore == null -> null
                hrvScore == null -> kotlin.math.round(sleepScore).toInt().coerceIn(0, 100)
                else -> kotlin.math.round(0.6f * sleepScore + 0.4f * hrvScore).toInt().coerceIn(0, 100)
            }

            val resilienceBucket = resilienceScore?.let { score ->
                when {
                    score >= 80 -> "Great"
                    score >= 60 -> "Good"
                    score >= 40 -> "Fair"
                    else -> "Low"
                }
            }

            return ResilienceResult(
                score = resilienceScore,
                bucket = resilienceBucket,
                sleepScore = sleepScore,
                hrvScore = hrvScore
            )
        }
    }

    private suspend fun gatherBackupPayload(): com.example.data.repository.DriveBackupPayload {
        val profile = repository.userProfile.firstOrNull()
        val dailyMetrics = database.metricsDao().getAllMetrics().firstOrNull() ?: emptyList()
        val foodLogs = database.metricsDao().getAllFoodLogsUnbounded().firstOrNull() ?: emptyList()
        val savedDietCharts = repository.getAllSavedCharts().firstOrNull() ?: emptyList()
        val savedWorkouts = repository.getAllSavedWorkouts().firstOrNull() ?: emptyList()
        val savedChats = repository.getAllSavedChats().firstOrNull() ?: emptyList()
        val medicalRecords = repository.getMedicalRecords().firstOrNull() ?: emptyList()

        return com.example.data.repository.DriveBackupPayload(
            backupDate = java.time.Instant.now().toString(),
            profile = profile,
            dailyMetrics = dailyMetrics,
            foodLogs = foodLogs,
            savedDietCharts = savedDietCharts,
            savedWorkouts = savedWorkouts,
            savedChats = savedChats,
            medicalRecords = medicalRecords
        )
    }

    fun performDriveBackupWithToken(token: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = gatherBackupPayload()
                val success = com.example.data.repository.GoogleDriveManager.backupToDrive(token, payload)
                withContext(Dispatchers.Main) {
                    if (success) {
                        onResult(true, "Backup saved to Google Drive")
                    } else {
                        onResult(false, "Failed to upload backup to Google Drive")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Backup failed")
                }
            }
        }
    }

    fun backupToDrive(
        context: Context,
        onRequiresResolution: ((android.app.PendingIntent) -> Unit)? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        com.example.data.repository.GoogleDriveManager.requestAuthorization(context)
            .addOnSuccessListener { authResult ->
                if (authResult.hasResolution()) {
                    val pendingIntent = authResult.pendingIntent
                    if (pendingIntent != null && onRequiresResolution != null) {
                        onRequiresResolution(pendingIntent)
                    } else {
                        onResult(false, "Authorization resolution required")
                    }
                } else {
                    val token = authResult.accessToken
                    if (!token.isNullOrBlank()) {
                        performDriveBackupWithToken(token, onResult)
                    } else {
                        onResult(false, "Failed to obtain Google Drive authorization token")
                    }
                }
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "Google Drive authorization failed")
            }
    }

    fun performFetchDriveBackupWithToken(
        token: String,
        onResult: (com.example.data.repository.DriveBackupPayload?, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = com.example.data.repository.GoogleDriveManager.restoreFromDrive(token)
                withContext(Dispatchers.Main) {
                    if (payload != null) {
                        onResult(payload, "Backup fetched successfully")
                    } else {
                        onResult(null, "No backup found in Google Drive")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(null, e.message ?: "Failed to fetch backup")
                }
            }
        }
    }

    fun fetchDriveBackupForRestore(
        context: Context,
        onRequiresResolution: ((android.app.PendingIntent) -> Unit)? = null,
        onResult: (com.example.data.repository.DriveBackupPayload?, String) -> Unit
    ) {
        com.example.data.repository.GoogleDriveManager.requestAuthorization(context)
            .addOnSuccessListener { authResult ->
                if (authResult.hasResolution()) {
                    val pendingIntent = authResult.pendingIntent
                    if (pendingIntent != null && onRequiresResolution != null) {
                        onRequiresResolution(pendingIntent)
                    } else {
                        onResult(null, "Authorization resolution required")
                    }
                } else {
                    val token = authResult.accessToken
                    if (!token.isNullOrBlank()) {
                        performFetchDriveBackupWithToken(token, onResult)
                    } else {
                        onResult(null, "Failed to obtain Google Drive authorization token")
                    }
                }
            }
            .addOnFailureListener { e ->
                onResult(null, e.message ?: "Google Drive authorization failed")
            }
    }

    fun applyDriveBackup(
        payload: com.example.data.repository.DriveBackupPayload,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (payload.profile != null) {
                    repository.saveUserProfile(payload.profile)
                }
                for (metric in payload.dailyMetrics) {
                    repository.saveMetrics(metric)
                }
                for (foodLog in payload.foodLogs) {
                    repository.saveFoodLog(foodLog.copy(id = 0))
                }
                for (chart in payload.savedDietCharts) {
                    repository.saveDietChart(chart.copy(id = 0))
                }
                for (workout in payload.savedWorkouts) {
                    repository.saveWorkout(workout)
                }
                for (chat in payload.savedChats) {
                    repository.saveChat(chat)
                }
                for (record in payload.medicalRecords) {
                    repository.addMedicalRecord(record)
                }
                withContext(Dispatchers.Main) {
                    onResult(true, "Restore complete")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Restore failed")
                }
            }
        }
    }
}

data class ResilienceResult(
    val score: Int?,
    val bucket: String?,
    val sleepScore: Float? = null,
    val hrvScore: Float? = null
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ChatMessage(
    val text: String,
    val isUser: Boolean
)
