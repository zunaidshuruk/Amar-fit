package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.DailyMetric
import com.example.data.local.MetricsDao
import com.example.data.local.UserDao
import com.example.data.local.UserProfile
import com.example.data.remote.*
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

import com.example.presentation.viewmodel.ChatMessage

class AppRepository(
    private val userDao: UserDao,
    private val metricsDao: MetricsDao,
    private val savedDietChartDao: com.example.data.local.SavedDietChartDao,
    private val savedWorkoutDao: com.example.data.local.SavedWorkoutDao,
    private val savedChatDao: com.example.data.local.SavedChatDao,
    private val activityEventDao: com.example.data.local.ActivityEventDao? = null,
    private val youtubeVideoCacheDao: com.example.data.local.YoutubeVideoCacheDao? = null,
    private val medicalRecordDao: com.example.data.local.MedicalRecordDao? = null
) {

    suspend fun logActivityEvent(type: String, description: String, timestamp: Long = System.currentTimeMillis()) {
        activityEventDao?.insert(
            com.example.data.local.ActivityEvent(
                type = type,
                description = description,
                timestamp = timestamp
            )
        )
    }

    fun getTodayActivityEvents(startOfDay: Long, endOfDay: Long): kotlinx.coroutines.flow.Flow<List<com.example.data.local.ActivityEvent>> {
        return activityEventDao?.getTodayEvents(startOfDay, endOfDay) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    fun getAllActivityEvents(): kotlinx.coroutines.flow.Flow<List<com.example.data.local.ActivityEvent>> {
        return activityEventDao?.getAllEvents() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun deleteAccount(): DeleteAccountResult {
        return FirebaseManager.deleteAccount()
    }

    suspend fun syncDataOnLogin() {
        FirebaseManager.pullDataOnLogin(userDao, metricsDao, savedDietChartDao, savedWorkoutDao, savedChatDao, medicalRecordDao)
    }

    private fun resolveApiKeys(): List<String> {
        fun isValidKey(key: String?): Boolean {
            if (key.isNullOrBlank()) return false
            val trimmed = key.trim()
            if (trimmed.startsWith("MY_GEMINI_API_KEY") || trimmed == "default" || trimmed.length < 10) return false
            return true
        }

        val apiKeys = mutableListOf<String>()

        // 1. Key 3 first (user priority)
        try {
            val key3 = BuildConfig.GEMINI_API_KEY_3
            if (isValidKey(key3) && !apiKeys.contains(key3)) apiKeys.add(key3)
        } catch (e: Throwable) { /* Ignored */ }

        // 2. Key 2 second
        try {
            val key2 = BuildConfig.GEMINI_API_KEY_2
            if (isValidKey(key2) && !apiKeys.contains(key2)) apiKeys.add(key2)
        } catch (e: Throwable) { /* Ignored */ }

        // 3. Key 1 third
        try {
            val key1 = BuildConfig.GEMINI_API_KEY
            if (isValidKey(key1) && !apiKeys.contains(key1)) apiKeys.add(key1)
        } catch (e: Throwable) { /* Ignored */ }

        // 4. Any other fields dynamically in BuildConfig that look like Gemini API keys
        try {
            for (field in BuildConfig::class.java.fields) {
                if (field.type == String::class.java && (field.name.contains("GEMINI") || field.name.contains("KEY"))) {
                    val value = field.get(null) as? String
                    if (isValidKey(value) && value != null && !apiKeys.contains(value)) {
                        apiKeys.add(value)
                    }
                }
            }
        } catch (e: Throwable) { /* Ignored */ }

        return apiKeys
    }

    private suspend fun executeGeminiCallWithBackoff(
        request: GenerateContentRequest,
        maxRetries: Int = 3,
        model: String = "gemini-3.5-flash"
    ): GenerateContentResponse {
        val apiKeys = resolveApiKeys()

        if (apiKeys.isEmpty()) {
            throw Exception("API Key is missing or invalid. Please configure GEMINI_API_KEY_3 in the Secrets panel.")
        }

        var currentDelay = 1000L
        for (attempt in 0..maxRetries) {
            for (apiKey in apiKeys) {
                try {
                    return RetrofitClient.service.generateContent(model, apiKey, request)
                } catch (e: HttpException) {
                    val code = e.code()
                    if (code == 429 || code == 403 || code == 503) {
                        continue // try next key
                    }
                    throw e
                } catch (e: Exception) {
                    if (attempt == maxRetries && apiKey == apiKeys.last()) throw e
                    continue
                }
            }
            // All keys failed with 429/403 for this attempt
            if (attempt < maxRetries) {
                kotlinx.coroutines.delay(currentDelay)
                currentDelay *= 2
            } else {
                throw HttpException(retrofit2.Response.error<Any>(429, okhttp3.ResponseBody.create(null, "Quota exceeded across all keys")))
            }
        }
        throw Exception("Max retries exceeded")
    }

    private fun streamGeminiCall(request: GenerateContentRequest): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flow {
        val apiKeys = resolveApiKeys()
        if (apiKeys.isEmpty()) throw Exception("API Key is missing or invalid.")
        var lastError: Exception? = null
        for (apiKey in apiKeys) {
            try {
                val responseBody = RetrofitClient.service.streamGenerateContent(apiKey = apiKey, request = request)
                responseBody.source().use { source ->
                    while (true) {
                        val line = source.readUtf8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val jsonPart = line.removePrefix("data: ").trim()
                            if (jsonPart.isEmpty()) continue
                            try {
                                val adapter = RetrofitClient.moshi.adapter(GenerateContentResponse::class.java)
                                val parsed = adapter.fromJson(jsonPart)
                                val textChunk = parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                if (!textChunk.isNullOrEmpty()) {
                                    emit(textChunk)
                                }
                            } catch (e: Exception) {
                                // skip a malformed/partial chunk, keep reading
                            }
                        }
                    }
                }
                return@flow
            } catch (e: Exception) {
                lastError = e
                continue
            }
        }
        if (lastError != null) throw lastError
    }.flowOn(Dispatchers.IO)

    fun getChatResponseStream(chatHistory: List<ChatMessage>, profile: UserProfile?): kotlinx.coroutines.flow.Flow<String> {
        val contextPrompt = if (profile != null) {
            "User Context: ${profile.age}yo ${profile.gender}, Goal: ${profile.healthGoals}, Restrictions: ${profile.dietaryRestrictions}. "
        } else ""
        
        val systemInstruction = """
            You are 'KardIQ AI', a universal health, fitness, and wellness bot powered by a vast knowledge bank.
            Your goal is to assist the user with ANY health-related query, including general fitness, nutrition, mental wellness, sleep, healthy habits, and medical knowledge.
            
            You can act as a nutritionist, a workout coach, a lifestyle advisor, or a general health assistant.
            You should provide well-rounded, evidence-based advice.
            
            Guidelines:
            - Answer directly and professionally, but maintain a warm and motivating tone.
            - If they ask for recipes, you can provide them.
            - Do NOT force a specific conversation flow (e.g., asking for ingredients first). Simply respond naturally and thoughtfully to their questions.
            - Use markdown (bolding, bullet points) to format your advice for readability.
            
            $contextPrompt
        """.trimIndent()
        
        val apiContents = chatHistory.drop(1).map { msg ->
            Content(
                role = if (msg.isUser) "user" else "model",
                parts = listOf(Part(text = msg.text))
            )
        }
        
        val request = GenerateContentRequest(
            contents = apiContents,
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        
        return streamGeminiCall(request)
    }

    fun getFoodChatResponseStream(chatHistory: List<ChatMessage>): kotlinx.coroutines.flow.Flow<String> {
        val systemInstruction = """
            You are KardIQ's food-logging assistant for users in Bangladesh. Your job is to have a short, natural conversation to figure out exactly what the user ate, then log it.

            Language handling:
            - The user may write in English, in Bangla script, or in phonetic Bangla typed with English letters (e.g. "ami duita ruti r ek bati dal khaisi"). Understand all three naturally, the way a Bangladeshi person reading a text message would. Never ask the user to "please use English" or explain that you detected Banglish -- just understand it and respond warmly in the same style/language they used.
            - Respond primarily in English unless the user writes in Bangla or Banglish, in which case you may reply in Bangla script.

            Conversation flow:
            - If the user's description is missing an important detail you need for a reasonable calorie/macro estimate (roughly how much, e.g. "a plate of rice" with no sense of portion, or an ambiguous dish name), ask ONE short, natural clarifying question at a time. Do not interrogate -- one question, then log using reasonable defaults if they don't give a precise answer.
            - Once you have enough information (this usually only takes 1-2 exchanges), respond with a brief, friendly confirmation sentence, then on a new line write exactly:
            READY_TO_LOG
            followed immediately by a single raw JSON object (no markdown, no code fences) with these exact keys:
            "name" (string), "category" (string), "calories" (integer), "carbs" (number, grams), "protein" (number, grams), "fat" (number, grams), "sodium" (number, milligrams), "sugar" (number, grams), "fiber" (number, grams), "description" (string), "mealType" (string, one of "Breakfast", "Lunch", "Dinner", "Snack" -- infer from context or time of day if not stated).
            - Only emit READY_TO_LOG once, when you are done -- not while still asking clarifying questions.
        """.trimIndent()

        val apiContents = chatHistory.map { msg ->
            Content(
                role = if (msg.isUser) "user" else "model",
                parts = listOf(Part(text = msg.text))
            )
        }

        val request = GenerateContentRequest(
            contents = apiContents,
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        return streamGeminiCall(request)
    }

    val userProfile = userDao.getUserProfile()
    
    fun getMetricsForDate(date: String) = metricsDao.getMetricsForDate(date)

    fun getMetricsHistory(startDate: String) = metricsDao.getMetricsHistory(startDate)

    fun getMetricsHistoryRange(startDate: String, endDate: String) = metricsDao.getMetricsHistoryRange(startDate, endDate)

    suspend fun saveUserProfile(profile: UserProfile) {
        val profileToSave = if (profile.friendCode.isBlank()) {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val claimedCode = if (user != null) FirebaseManager.claimFriendCode(user.uid) else null
            if (claimedCode != null) profile.copy(friendCode = claimedCode) else profile
        } else {
            profile
        }
        userDao.insertProfile(profileToSave)
        try {
            FirebaseManager.syncProfile(profileToSave)
            FirebaseManager.syncPublicProfile(
                name = profileToSave.name,
                currentStreak = profileToSave.currentStreak,
                points = profileToSave.points,
                badges = profileToSave.badges,
                friendCode = profileToSave.friendCode
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun saveMetrics(metric: DailyMetric) {
        metricsDao.insertMetrics(metric)
        try {
            FirebaseManager.syncMetric(metric)
            val currentProfile = userDao.getUserProfile().firstOrNull()
            if (currentProfile != null) {
                FirebaseManager.syncFriendStats(metric, currentProfile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun awardChallengeBonus(points: Int, badgeId: String) {
        val profile = userDao.getUserProfile().firstOrNull() ?: return
        val badges = profile.badges.split(",").filter { it.isNotBlank() }.toMutableList()
        if (!badges.contains(badgeId)) badges.add(badgeId)
        val updated = profile.copy(points = profile.points + points, badges = badges.joinToString(","))
        saveUserProfile(updated)
    }
    
    suspend fun checkAndAwardBadges(metric: DailyMetric) {
        val profile = userDao.getUserProfile().firstOrNull() ?: return
        
        var pointsToAdd = 0
        var newStreak = profile.currentStreak
        val currentBadges = profile.badges.split(",").filter { it.isNotBlank() }.toMutableSet()
        
        // Streak logic
        val isNewDay = profile.lastActiveDate != metric.date
        if (isNewDay) {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            try {
                val lastDate = sdf.parse(profile.lastActiveDate)
                val currentDate = sdf.parse(metric.date)
                if (lastDate != null && currentDate != null) {
                    val diff = (currentDate.time - lastDate.time) / (1000 * 60 * 60 * 24)
                    if (diff == 1L) {
                        newStreak += 1
                    } else if (diff > 1L) {
                        newStreak = 1
                    }
                } else {
                    newStreak = 1
                }
            } catch (e: Exception) {
                newStreak = 1
            }
        }
        
        // award(): grants a badge exactly once and adds its points, keyed off com.example.data.model.ALL_BADGES.
        fun award(id: String, points: Int) {
            if (!currentBadges.contains(id)) {
                currentBadges.add(id)
                pointsToAdd += points
                com.example.data.repository.FirebaseManager.postActivityEvent("badge_earned", "Earned a new badge!")
            }
        }

        if (metric.waterLiters >= profile.dailyWaterLimitLiters) award("Hydration Hero", 50)
        if (metric.steps >= 10000) award("10k Steps Master", 100)
        if (newStreak >= 3) award("Consistency Starter", 20)

        // --- Steps & distance ---
        if (metric.steps > 0) award("First Steps", 10)
        if (metric.steps >= 5000) award("5K Strider", 30)
        if (metric.steps >= 15000) award("15K Steps Champion", 150)
        if (metric.steps >= 20000) award("20K Steps Legend", 250)
        if (metric.distanceMeters >= 5000f) award("Distance Walker", 60)
        if (metric.distanceMeters >= 10000f) award("Marathon Mover", 150)
        if (metric.distanceMeters >= 21000f) award("Half Marathon Hero", 300)

        // --- Hydration ---
        if (metric.waterLiters >= 1.0f) award("Water Sipper", 15)
        if (profile.dailyWaterLimitLiters > 0f && metric.waterLiters >= profile.dailyWaterLimitLiters / 2f) award("Halfway Hydrated", 25)
        if (profile.dailyWaterLimitLiters > 0f && metric.waterLiters >= profile.dailyWaterLimitLiters * 2f) award("Double Hydration", 70)

        // --- Sleep ---
        if (metric.sleepHours >= 7f) award("Power Rest", 40)
        if (metric.sleepHours >= 8f) award("Well Rested", 60)
        if (metric.sleepHours >= 9f) award("Deep Sleeper", 90)

        // --- Heart & vitals ---
        if (metric.restingHeartRate > 0) award("Heart Check-In", 15)
        if (metric.restingHeartRate in 40..70) award("Steady Heart", 80)
        if (metric.heartRate >= 140 || metric.heartRateMax >= 140) award("Cardio Zone", 70)
        if (metric.heartRateVariability > 0f) award("HRV Tracker", 20)
        if (metric.oxygenSaturation >= 95f) award("Oxygen Ace", 50)
        if (metric.respiratoryRate > 0f) award("Breath Aware", 15)
        if (metric.skinTemperatureCelsius > 0f) award("Temperature Check", 10)

        // --- Workouts ---
        if (metric.exerciseMinutes >= 30) award("Workout Warrior", 60)
        if (metric.exerciseMinutes >= 60) award("Iron Will", 100)
        if (metric.exerciseMinutes >= 90) award("Endurance Elite", 180)
        if (metric.activeCaloriesBurned >= 500) award("Calorie Crusher", 100)
        if (metric.activeCaloriesBurned >= 300) award("Fat Burner", 60)

        // --- Mindfulness ---
        if (metric.mindfulnessMinutes >= 1) award("Mindful Minute", 10)
        if (metric.mindfulnessMinutes >= 10) award("Calm Mind", 50)
        if (metric.mindfulnessMinutes >= 20) award("Zen Master", 90)

        // --- Nutrition ---
        if (metric.proteinG >= 100f) award("Protein Powerhouse", 60)
        if (metric.carbsG > 0f && metric.proteinG > 0f && metric.fatG > 0f) award("Balanced Plate", 50)
        if (metric.proteinG >= 120f && metric.carbsG >= 150f && metric.fatG >= 50f) award("Macro Master", 150)
        if (metric.caloriesConsumed > 0 && profile.dailyCalorieLimit > 0 && metric.caloriesConsumed <= profile.dailyCalorieLimit) award("Calorie Conscious", 80)
        if (metric.externalNutritionCalories > 0) award("Mindful Eater", 20)

        // --- Body & metrics logging ---
        if (metric.weightKg > 0f) award("Weigh-In Warrior", 15)
        val glucoseReadings = listOf(
            metric.bloodGlucoseMorning, metric.bloodGlucoseNight,
            metric.bloodGlucoseBeforeBreakfast, metric.bloodGlucoseAfterBreakfast,
            metric.bloodGlucoseBeforeLunch, metric.bloodGlucoseAfterLunch,
            metric.bloodGlucoseBeforeDinner, metric.bloodGlucoseAfterDinner
        )
        if (glucoseReadings.any { it > 0f }) award("Glucose Guardian", 20)
        if (profile.bloodGlucoseTargetMax > 0f) {
            val inRange = glucoseReadings.any { it > 0f && it in profile.bloodGlucoseTargetMin..profile.bloodGlucoseTargetMax }
            if (inRange) award("In-Range Champion", 90)
        }
        if (metric.bloodPressure.isNotBlank()) {
            award("BP Tracker", 20)
            try {
                val parts = metric.bloodPressure.split("/")
                val systolic = parts.getOrNull(0)?.trim()?.toIntOrNull()
                val diastolic = parts.getOrNull(1)?.trim()?.toIntOrNull()
                if (systolic != null && diastolic != null && systolic <= 120 && diastolic <= 80) {
                    award("Healthy Pressure", 90)
                }
            } catch (e: Exception) {
                // Unparsable blood pressure format; skip the "Healthy Pressure" check for this entry.
            }
        }

        // --- Streaks ---
        if (newStreak >= 7) award("Week Warrior", 100)
        if (newStreak >= 14) award("Fortnight Fighter", 150)
        if (newStreak >= 30) award("Monthly Master", 300)
        if (newStreak >= 90) award("Quarter Champion", 500)
        if (newStreak >= 100) award("Century Streak", 600)
        if (newStreak >= 180) award("Half-Year Hero", 800)
        if (newStreak >= 365) award("Year-Long Legend", 1000)

        // --- Combo / all-rounder ---
        if (profile.stepGoal > 0 && metric.steps >= profile.stepGoal &&
            profile.dailyWaterLimitLiters > 0f && metric.waterLiters >= profile.dailyWaterLimitLiters &&
            profile.sleepGoalHours > 0f && metric.sleepHours >= profile.sleepGoalHours
        ) {
            award("Triple Threat", 200)
        }
        if (metric.steps > 0 && metric.waterLiters > 0f && metric.sleepHours > 0f &&
            metric.weightKg > 0f && metric.exerciseMinutes > 0
        ) {
            award("Full Log Day", 120)
        }
        val loggedMetricCount = listOf(
            metric.steps > 0, metric.waterLiters > 0f, metric.sleepHours > 0f,
            metric.weightKg > 0f, metric.exerciseMinutes > 0, metric.mindfulnessMinutes > 0,
            metric.heartRate > 0, metric.bloodPressure.isNotBlank()
        ).count { it }
        if (loggedMetricCount >= 5) award("Data Devotee", 100)
        if (metric.exerciseMinutes >= 30 && metric.mindfulnessMinutes >= 10) award("Two Birds", 100)
        if (metric.bloodPressure.isNotBlank() && metric.respiratoryRate > 0f && metric.oxygenSaturation > 0f) {
            award("Vitals Check", 90)
        }

        // --- Points milestones (checked against the running total including everything awarded above) ---
        val runningPoints = profile.points + pointsToAdd
        if (runningPoints >= 500) award("Point Collector", 0)
        if (runningPoints >= 1000) award("Point Master", 0)
        if (runningPoints >= 2500) award("Point Legend", 0)

        if (pointsToAdd > 0 || isNewDay) {
            val updatedProfile = profile.copy(
                points = profile.points + pointsToAdd,
                badges = currentBadges.joinToString(","),
                currentStreak = newStreak,
                lastActiveDate = metric.date
            )
            userDao.updateProfile(updatedProfile)
        }
    }

    suspend fun getChatResponse(chatHistory: List<ChatMessage>, profile: UserProfile?): String = withContext(Dispatchers.IO) {
        // API Key logic is handled by executeGeminiCallWithBackoff
        
        val contextPrompt = if (profile != null) {
            "User Context: ${profile.age}yo ${profile.gender}, Goal: ${profile.healthGoals}, Restrictions: ${profile.dietaryRestrictions}. "
        } else ""
        
        val systemInstruction = """
            You are 'KardIQ AI', a universal health, fitness, and wellness bot powered by a vast knowledge bank.
            Your goal is to assist the user with ANY health-related query, including general fitness, nutrition, mental wellness, sleep, healthy habits, and medical knowledge.
            
            You can act as a nutritionist, a workout coach, a lifestyle advisor, or a general health assistant.
            You should provide well-rounded, evidence-based advice.
            
            Guidelines:
            - Answer directly and professionally, but maintain a warm and motivating tone.
            - If they ask for recipes, you can provide them.
            - Do NOT force a specific conversation flow (e.g., asking for ingredients first). Simply respond naturally and thoughtfully to their questions.
            - Use markdown (bolding, bullet points) to format your advice for readability.
            
            $contextPrompt
        """.trimIndent()
        
        val apiContents = chatHistory.drop(1).map { msg ->
            Content(
                role = if (msg.isUser) "user" else "model",
                parts = listOf(Part(text = msg.text))
            )
        }
        
        val request = GenerateContentRequest(
            contents = apiContents,
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        
        try {
            val response = executeGeminiCallWithBackoff(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not generate response."
        } catch (e: HttpException) {
            if (e.code() == 429) "The AI is currently busy due to high traffic. Retries exhausted. Please try again in a minute."
            else "Error: ${e.message}"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun getFoodLogsForDate(date: String) = metricsDao.getFoodLogsForDate(date)

    suspend fun deleteOldFoodLogs(cutoffDate: String) {
        metricsDao.deleteOldLogs(cutoffDate)
    }
    fun getRecentFoodLogs() = metricsDao.getRecentFoodLogs()

    suspend fun checkChronicDeficiency(logs: List<com.example.data.local.FoodLog>, profile: com.example.data.local.UserProfile): String? = withContext(Dispatchers.IO) {
        if (logs.size < 5) return@withContext null // Need some data
        
        val logsText = logs.take(21).joinToString("\n") { "${it.date}: ${it.name} - ${it.calories} kcal (${it.description})" }
        val systemInstruction = """
            You are an AI nutritionist evaluating a user for chronic macro-nutrient deficiencies based on their 7-day food log and profile.
            Profile: ${profile.age} years old, ${profile.weightKg}kg, ${profile.heightCm}cm, Gender: ${profile.gender}, Goals: ${profile.healthGoals}.
            Evaluate if there is a SEVERE, CHRONIC deficiency in Protein, Carbs, or Fats.
            If there is a severe deficiency, reply with ONLY a short 1-sentence alert message (e.g., "You have a chronic protein deficiency, consider adding eggs or lentils."). 
            If there is NO severe deficiency, reply exactly with "NONE".
        """.trimIndent()
        
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Food Logs:\n$logsText")))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        try {
            val response = executeGeminiCallWithBackoff(request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: "NONE"
            if (text.uppercase() == "NONE" || text.isEmpty()) null else text
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generateGlucoseGuidance(
        profile: com.example.data.local.UserProfile,
        metrics: List<com.example.data.local.DailyMetric>
    ): String = withContext(Dispatchers.IO) {
        val glucoseReadings = metrics.flatMap { m ->
            listOfNotNull(
                m.bloodGlucoseMorning.takeIf { it > 0 },
                m.bloodGlucoseNight.takeIf { it > 0 },
                m.bloodGlucoseBeforeBreakfast.takeIf { it > 0 },
                m.bloodGlucoseAfterBreakfast.takeIf { it > 0 },
                m.bloodGlucoseBeforeLunch.takeIf { it > 0 },
                m.bloodGlucoseAfterLunch.takeIf { it > 0 },
                m.bloodGlucoseBeforeDinner.takeIf { it > 0 },
                m.bloodGlucoseAfterDinner.takeIf { it > 0 }
            )
        }
        if (glucoseReadings.isEmpty()) return@withContext "Log a few blood glucose readings first so suggestions can be based on your actual data."

        val metricsText = metrics.take(7).joinToString("\n") { m ->
            "${m.date}: bgBeforeBreakfast=${m.bloodGlucoseBeforeBreakfast}, bgAfterBreakfast=${m.bloodGlucoseAfterBreakfast}, bgBeforeLunch=${m.bloodGlucoseBeforeLunch}, bgAfterLunch=${m.bloodGlucoseAfterLunch}, bgBeforeDinner=${m.bloodGlucoseBeforeDinner}, bgAfterDinner=${m.bloodGlucoseAfterDinner}, steps=${m.steps}, sleepHours=${m.sleepHours}, weightKg=${m.weightKg}, bloodPressure=${m.bloodPressure}, waterLiters=${m.waterLiters}, caloriesConsumed=${m.caloriesConsumed}, carbsG=${m.carbsG}, proteinG=${m.proteinG}, fatG=${m.fatG}, exerciseMinutes=${m.exerciseMinutes}"
        }
        val targetRangeText = if (profile.bloodGlucoseTargetMin > 0f && profile.bloodGlucoseTargetMax > 0f) {
            "User's target range: ${profile.bloodGlucoseTargetMin}-${profile.bloodGlucoseTargetMax} mmol/L."
        } else {
            "User has not set a target range."
        }

        val systemInstruction = """
            You are a wellness assistant. Based on the user's last 7 days of health metrics below (blood glucose readings, steps, sleep, weight, blood pressure, water, calories, macros, exercise minutes), suggest 3-5 SHORT, actionable, general lifestyle changes that could help lower or better manage blood glucose.
            Profile: ${profile.age} years old, ${profile.gender}, activity level: ${profile.activityLevel}. $targetRangeText
            STRICT RULES — non-negotiable:
            - NEVER name a specific disease, diagnosis, or medical condition (e.g. do not say "diabetes").
            - NEVER claim certainty about the user's health status or guarantee a result.
            - Frame every suggestion as general wellness/lifestyle awareness only, never medical advice or a treatment plan.
            - Base suggestions only on patterns actually visible in the data provided (e.g. low exercise minutes, high evening readings, low water intake) — do not invent patterns that aren't there.
            - Format as a short markdown bullet list, one suggestion per bullet, each 1 sentence.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "7-Day Metrics:\n$metricsText")))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        try {
            val response = executeGeminiCallWithBackoff(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Could not generate suggestions right now. Please try again later."
        } catch (e: Exception) {
            "Could not generate suggestions right now. Please try again later."
        }
    }

    suspend fun generateHealthInsight(
        profile: com.example.data.local.UserProfile,
        metrics: List<com.example.data.local.DailyMetric>,
        foodLogs: List<com.example.data.local.FoodLog>
    ): String = withContext(Dispatchers.IO) {
        if (metrics.isEmpty()) return@withContext "Keep logging your health data daily to unlock personalized wellness insights."

        val metricsText = metrics.take(7).joinToString("\n") { m ->
            "${m.date}: steps=${m.steps}, sleepHours=${m.sleepHours}, restingHR=${m.restingHeartRate}, weightKg=${m.weightKg}, waterLiters=${m.waterLiters}/${profile.dailyWaterLimitLiters}, caloriesConsumed=${m.caloriesConsumed}/${profile.dailyCalorieLimit}, bloodGlucoseMorning=${m.bloodGlucoseMorning}, bloodGlucoseNight=${m.bloodGlucoseNight}, bloodPressure=${m.bloodPressure}, oxygenSaturation=${m.oxygenSaturation}"
        }
        val foodLogsText = foodLogs.take(21).joinToString("\n") { "${it.date}: ${it.name} (${it.calories} kcal)" }

        val systemInstruction = """
            You are a wellness assistant generating a short daily health insight card for a fitness app user.
            Profile: ${profile.age} years old, ${profile.gender}, ${profile.heightCm}cm, ${profile.weightKg}kg, activity level: ${profile.activityLevel}.
            Based on the last 7 days of health metrics and recent food logs below, write a SHORT (2-3 sentences max) summary covering:
            1. A notable trend or pattern from the data (positive or needing attention).
            2. One general wellness or lifestyle-risk awareness point relevant to the data, if applicable.
            STRICT RULES — non-negotiable:
            - NEVER name a specific disease, diagnosis, or medical condition.
            - NEVER claim certainty about the user's health status.
            - Frame everything as general wellness/lifestyle awareness only, never medical advice.
            - Keep it encouraging and actionable, not alarming.
            - Do not use markdown formatting — plain sentences only.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "7-Day Metrics:\n$metricsText\n\nRecent Food Logs:\n$foodLogsText")))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        try {
            val response = executeGeminiCallWithBackoff(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: "Keep up your healthy habits! Check back tomorrow for a fresh insight."
        } catch (e: Exception) {
            "Keep up your healthy habits! Check back tomorrow for a fresh insight."
        }
    }

    suspend fun generateNutritionalInsights(logs: List<com.example.data.local.FoodLog>): String = withContext(Dispatchers.IO) {
        if (logs.isEmpty()) return@withContext "Not enough food logged yet to generate insights. Keep logging your meals!"
        
        val logsText = logs.joinToString("\n") { "${it.date}: ${it.name} - ${it.calories} kcal (${it.description})" }
        val systemInstruction = """
            You are an expert AI nutritionist.
            Analyze the following weekly food logs.
            1. Summarize the overall nutritional trends.
            2. Identify any potential macro-nutrient (protein, carbs, fats) deficiencies or imbalances based on the logged items.
            3. Provide a brief, actionable recommendation.
            Keep the response concise, friendly, and formatted nicely in markdown with bullet points.
        """.trimIndent()
        
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Food Logs:\n$logsText")))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        try {
            val response = executeGeminiCallWithBackoff(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not generate insights."
        } catch (e: HttpException) {
            if (e.code() == 429) "The AI is currently busy. Retries exhausted. Please try again in a minute."
            else "Error generating insights: ${e.message}"
        } catch (e: Exception) {
            "Error generating insights: ${e.message}"
        }
    }


    suspend fun deleteFoodLog(foodLog: com.example.data.local.FoodLog) {
        metricsDao.deleteFoodLog(foodLog)
        try {
            FirebaseManager.deleteFoodLog(foodLog)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveFoodLog(foodLog: com.example.data.local.FoodLog) {
        metricsDao.insertFoodLog(foodLog)
        try {
            FirebaseManager.syncFoodLog(foodLog)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateFoodLog(foodLog: com.example.data.local.FoodLog) {
        metricsDao.updateFoodLog(foodLog)
        try {
            FirebaseManager.syncFoodLog(foodLog)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun analyzeFoodText(foodText: String): String = withContext(Dispatchers.IO) {
        // API Key logic is handled by executeGeminiCallWithBackoff
        
        val systemInstruction = """
            You are an expert AI food analyzer specializing in nutrition.
            Based on the provided text description of a meal, identify the food, estimate the portion size, and provide a rough estimate of the total calories and macronutrients.
            You MUST return ONLY a raw JSON object with NO markdown formatting, NO code blocks, and NO extra text.
            The JSON MUST have these exact keys:
            "name" (string), "category" (string), "calories" (integer), "carbs" (number, grams), "protein" (number, grams), "fat" (number, grams), "sodium" (number, milligrams), "sugar" (number, grams), "fiber" (number, grams), "description" (string).
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "Analyze this food description and return JSON: $foodText")
                    )
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = executeGeminiCallWithBackoff(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: """{"name": "Error", "category": "Error", "calories": 0, "carbs": 0, "protein": 0, "fat": 0, "sodium": 0, "sugar": 0, "fiber": 0, "description": "Could not analyze the food."}"""
        } catch (e: HttpException) {
            val msg = if (e.code() == 429) "AI quota exceeded. Retries exhausted (429)." else "Error: ${e.message}"
            """{"name": "Error", "category": "Error", "calories": 0, "carbs": 0, "protein": 0, "fat": 0, "sodium": 0, "sugar": 0, "fiber": 0, "description": "$msg"}"""
        } catch (e: Exception) {
            """{"name": "Error", "category": "Error", "calories": 0, "carbs": 0, "protein": 0, "fat": 0, "sodium": 0, "sugar": 0, "fiber": 0, "description": "Error: ${e.message}"}"""
        }
    }

    suspend fun analyzeFoodImage(base64Image: String): String = withContext(Dispatchers.IO) {
        // API Key logic is handled by executeGeminiCallWithBackoff

        val systemInstruction = """
            You are an expert AI food analyzer specializing in Bangladeshi cuisine.
            Identify the food, estimate the portion size, and provide a rough estimate of the total calories and macronutrients.
            You MUST return ONLY a raw JSON object with NO markdown formatting, NO code blocks, and NO extra text.
            The JSON MUST have these exact keys:
            "name" (string), "category" (string), "calories" (integer), "carbs" (number, grams), "protein" (number, grams), "fat" (number, grams), "sodium" (number, milligrams), "sugar" (number, grams), "fiber" (number, grams), "description" (string).
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "Analyze this food image and return JSON."),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = executeGeminiCallWithBackoff(request, model = "gemini-3.5-flash-lite")
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: """{"name": "Error", "category": "Error", "calories": 0, "carbs": 0, "protein": 0, "fat": 0, "sodium": 0, "sugar": 0, "fiber": 0, "description": "Could not analyze the image."}"""
        } catch (e: HttpException) {
            val msg = if (e.code() == 429) "AI quota exceeded. Retries exhausted (429)." else "Error: ${e.message}"
            """{"name": "Error", "category": "Error", "calories": 0, "carbs": 0, "protein": 0, "fat": 0, "sodium": 0, "sugar": 0, "fiber": 0, "description": "$msg"}"""
        } catch (e: Exception) {
            """{"name": "Error", "category": "Error", "calories": 0, "carbs": 0, "protein": 0, "fat": 0, "sodium": 0, "sugar": 0, "fiber": 0, "description": "Error: ${e.message}"}"""
        }
    }

    suspend fun lookupBarcodeProduct(barcode: String): String = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isEmpty()) {
            return@withContext """{"name": "Error", "category": "Error", "calories": 0, "carbs": 0, "protein": 0, "fat": 0, "sodium": 0, "sugar": 0, "fiber": 0, "description": "Invalid barcode."}"""
        }
        val url = "https://world.openfoodfacts.org/api/v0/product/$cleanBarcode.json"
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "KardIQ - Android - Version 1.0")
            .get()
            .build()
        try {
            val response = RetrofitClient.okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext """{"name": "Error", "category": "Error", "calories": 0, "carbs": 0, "protein": 0, "fat": 0, "sodium": 0, "sugar": 0, "fiber": 0, "description": "Barcode $cleanBarcode not found in Open Food Facts database (HTTP ${response.code})."}"""
            }
            val rootJson = org.json.JSONObject(responseBody)
            val status = rootJson.optInt("status", 0)
            if (status != 1 || !rootJson.has("product")) {
                return@withContext """{"name": "Error", "category": "Error", "calories": 0, "carbs": 0, "protein": 0, "fat": 0, "sodium": 0, "sugar": 0, "fiber": 0, "description": "Barcode $cleanBarcode was not found in the Open Food Facts database."}"""
            }
            val product = rootJson.getJSONObject("product")
            val productName = product.optString("product_name", "").ifBlank {
                product.optString("product_name_en", "").ifBlank { "Packaged Product" }
            }
            val nutriments = product.optJSONObject("nutriments")
            
            var calories = 0
            var carbs = 0f
            var protein = 0f
            var fat = 0f
            var sodium = 0f
            var sugar = 0f
            var fiber = 0f
            var isPerServing = false
            val servingSize = product.optString("serving_size", "").trim()

            if (nutriments != null) {
                val energyServing = nutriments.optDouble("energy-kcal_serving", Double.NaN)
                val carbsServing = nutriments.optDouble("carbohydrates_serving", Double.NaN)
                val proteinServing = nutriments.optDouble("proteins_serving", Double.NaN)
                val fatServing = nutriments.optDouble("fat_serving", Double.NaN)
                val sodiumServing = nutriments.optDouble("sodium_serving", Double.NaN)
                val sugarServing = nutriments.optDouble("sugars_serving", Double.NaN)
                val fiberServing = nutriments.optDouble("fiber_serving", Double.NaN)

                if (!energyServing.isNaN() && energyServing > 0) {
                    calories = energyServing.toInt()
                    carbs = if (!carbsServing.isNaN()) carbsServing.toFloat() else 0f
                    protein = if (!proteinServing.isNaN()) proteinServing.toFloat() else 0f
                    fat = if (!fatServing.isNaN()) fatServing.toFloat() else 0f
                    val sodiumG = if (!sodiumServing.isNaN()) sodiumServing else nutriments.optDouble("sodium_100g", 0.0)
                    sodium = sodiumG.toFloat() * 1000f
                    sugar = (if (!sugarServing.isNaN()) sugarServing else nutriments.optDouble("sugars_100g", 0.0)).toFloat()
                    fiber = (if (!fiberServing.isNaN()) fiberServing else nutriments.optDouble("fiber_100g", 0.0)).toFloat()
                    isPerServing = true
                } else {
                    val energy100g = nutriments.optDouble("energy-kcal_100g", Double.NaN).let {
                        if (it.isNaN()) nutriments.optDouble("energy-kcal", 0.0) else it
                    }
                    val carbs100g = nutriments.optDouble("carbohydrates_100g", 0.0)
                    val protein100g = nutriments.optDouble("proteins_100g", 0.0)
                    val fat100g = nutriments.optDouble("fat_100g", 0.0)
                    val sodium100g = nutriments.optDouble("sodium_100g", 0.0)
                    val sugar100g = nutriments.optDouble("sugars_100g", 0.0)
                    val fiber100g = nutriments.optDouble("fiber_100g", 0.0)

                    calories = energy100g.toInt()
                    carbs = carbs100g.toFloat()
                    protein = protein100g.toFloat()
                    fat = fat100g.toFloat()
                    sodium = sodium100g.toFloat() * 1000f
                    sugar = sugar100g.toFloat()
                    fiber = fiber100g.toFloat()
                    isPerServing = false
                }
            }

            val description = if (isPerServing) {
                if (servingSize.isNotEmpty()) "Per serving ($servingSize)" else "Per serving"
            } else {
                "Per 100g — check the package for your actual portion"
            }

            val resultObj = org.json.JSONObject().apply {
                put("name", productName)
                put("category", "Packaged Food")
                put("calories", calories)
                put("carbs", carbs)
                put("protein", protein)
                put("fat", fat)
                put("sodium", sodium)
                put("sugar", sugar)
                put("fiber", fiber)
                put("description", description)
            }
            resultObj.toString()
        } catch (e: Exception) {
            """{"name": "Error", "category": "Error", "calories": 0, "carbs": 0, "protein": 0, "fat": 0, "sodium": 0, "sugar": 0, "fiber": 0, "description": "Error looking up barcode: ${e.localizedMessage ?: e.message}"}"""
        }
    }

    fun generateCoachAdviceStream(topic: String, habit: String, benefits: String): kotlinx.coroutines.flow.Flow<String> {
        val systemInstruction = """
            You are 'KardIQ AI', an expert Wellness and Sleep Optimization Coach. 
            The user wants to learn about the health topic: "$topic".
            The core habit is: "$habit".
            The benefit is: "$benefits".
            
            Provide a friendly, motivational coaching suggestion directly to the user.
            IMPORTANT FORMATTING RULES:
            - Use proper spacing (blank lines between paragraphs).
            - Use bullet points for actionable steps to make it easily readable.
            - Explain WHY this habit works biologically or psychologically.
            - Keep it highly engaging, well-organized, and professional.
        """.trimIndent()
        
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = "Please give me my wellness coaching advice on $topic."))
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        
        return streamGeminiCall(request)
    }

    suspend fun generateCoachAdvice(topic: String, habit: String, benefits: String): String = withContext(Dispatchers.IO) {
        // API Key logic is handled by executeGeminiCallWithBackoff
        
        val systemInstruction = """
            You are 'KardIQ AI', an expert Wellness and Sleep Optimization Coach. 
            The user wants to learn about the health topic: "$topic".
            The core habit is: "$habit".
            The benefit is: "$benefits".
            
            Provide a friendly, motivational coaching suggestion directly to the user.
            IMPORTANT FORMATTING RULES:
            - Use proper spacing (blank lines between paragraphs).
            - Use bullet points for actionable steps to make it easily readable.
            - Explain WHY this habit works biologically or psychologically.
            - Keep it highly engaging, well-organized, and professional.
        """.trimIndent()
        
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = "Please give me my wellness coaching advice on $topic."))
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        
        try {
            val response = executeGeminiCallWithBackoff(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not generate coaching advice."
        } catch (e: HttpException) {
            if (e.code() == 429) "The AI is currently busy due to high traffic. Retries exhausted. Please try again in a minute."
            else "Error: ${e.message}"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun generateStructuredWorkout(profile: UserProfile?): Result<com.example.data.model.WorkoutPlan> = withContext(Dispatchers.IO) {
        val contextPrompt = if (profile != null) {
            "User Context: ${profile.age}yo ${profile.gender}, Weight: ${profile.weightKg}kg, Height: ${profile.heightCm}cm, Goal: ${profile.healthGoals}. "
        } else ""

        val systemInstruction = """
            You are 'KardIQ AI', an expert fitness coach.
            Generate a personalized daily workout routine based on the user's profile.
            You MUST return ONLY a raw JSON object matching this schema with NO markdown formatting, NO commentary, and NO code fences:
            {
              "title": "String",
              "warmup": [
                {
                  "name": "String",
                  "sets": Int or null,
                  "reps": "String (e.g. 10-12)" or null,
                  "durationSeconds": Int (seconds) or null,
                  "restSeconds": Int,
                  "youtubeSearchQuery": "String search query for exercise demonstration",
                  "metValue": Double (estimated MET intensity, e.g. 3.0 to 8.0)
                }
              ],
              "mainExercises": [
                {
                  "name": "String",
                  "sets": Int or null,
                  "reps": "String (e.g. 8-10)" or null,
                  "durationSeconds": Int or null,
                  "restSeconds": Int,
                  "youtubeSearchQuery": "String",
                  "metValue": Double
                }
              ],
              "cooldown": [
                {
                  "name": "String",
                  "sets": Int or null,
                  "reps": "String" or null,
                  "durationSeconds": Int or null,
                  "restSeconds": Int,
                  "youtubeSearchQuery": "String",
                  "metValue": Double
                }
              ]
            }
            $contextPrompt
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = "Please generate my personalized structured workout for today in JSON format."))
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val response = executeGeminiCallWithBackoff(request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("Empty response from AI service"))
            
            val cleanedJson = jsonText.trim().removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()
            val adapter = RetrofitClient.moshi.adapter(com.example.data.model.WorkoutPlan::class.java)
            val plan = adapter.fromJson(cleanedJson)
            if (plan != null) {
                Result.success(plan)
            } else {
                Result.failure(Exception("Failed to parse workout plan JSON"))
            }
        } catch (e: HttpException) {
            val msg = if (e.code() == 429) {
                "The AI is currently busy due to high traffic. Retries exhausted. Please try again in a minute."
            } else {
                "Error (${e.code()}): ${e.message}"
            }
            Result.failure(Exception(msg, e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateDietChartStream(profile: UserProfile, durationDays: Int): kotlinx.coroutines.flow.Flow<String> {
        val bmi = profile.weightKg / ((profile.heightCm / 100f) * (profile.heightCm / 100f))
        val contextPrompt = "User Context: ${profile.age}yo ${profile.gender}, Weight: ${profile.weightKg}kg, Height: ${profile.heightCm}cm, BMI: ${"%.1f".format(bmi)}, Goal: ${profile.healthGoals}, Restrictions: ${profile.dietaryRestrictions}."
        
        val systemInstruction = """
            You are an expert AI Nutritionist. You MUST follow optimum bangladeshi style food practices and include local Bangladeshi ingredients where possible.
            Generate a personalized $durationDays-day diet chart based on the user's profile.
            
            FORMATTING RULES:
            - Provide a day-by-day breakdown (e.g., Day 1, Day 2).
            - For each day, provide Breakfast, Lunch, Snack, and Dinner.
            - Keep it structured and easy to read using Markdown.
            - IMPORTANT: Include written step-by-step recipes for the meals directly in the plan.
            
            $contextPrompt
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = "Please generate my $durationDays-day personalized diet chart."))
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        return streamGeminiCall(request)
    }

    suspend fun generateDietChart(profile: UserProfile, durationDays: Int): String = withContext(Dispatchers.IO) {
        val bmi = profile.weightKg / ((profile.heightCm / 100f) * (profile.heightCm / 100f))
        val contextPrompt = "User Context: ${profile.age}yo ${profile.gender}, Weight: ${profile.weightKg}kg, Height: ${profile.heightCm}cm, BMI: ${"%.1f".format(bmi)}, Goal: ${profile.healthGoals}, Restrictions: ${profile.dietaryRestrictions}."
        
        val systemInstruction = """
            You are an expert AI Nutritionist. You MUST follow optimum bangladeshi style food practices and include local Bangladeshi ingredients where possible.
            Generate a personalized $durationDays-day diet chart based on the user's profile.
            
            FORMATTING RULES:
            - Provide a day-by-day breakdown (e.g., Day 1, Day 2).
            - For each day, provide Breakfast, Lunch, Snack, and Dinner.
            - Keep it structured and easy to read using Markdown.
            - IMPORTANT: Include written step-by-step recipes for the meals directly in the plan.
            
            $contextPrompt
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = "Please generate my $durationDays-day personalized diet chart."))
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = executeGeminiCallWithBackoff(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not generate diet chart."
        } catch (e: HttpException) {
            if (e.code() == 429) "The AI is currently busy due to high traffic. Retries exhausted. Please try again in a minute."
            else "Error: ${e.message}"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun generateShoppingList(dietChart: String): String = withContext(Dispatchers.IO) {
        val systemInstruction = """
            You are an expert AI Nutritionist assistant.
            Extract a comprehensive grocery shopping list from the provided diet chart.
            
            FORMATTING RULES:
            - Group items by category (e.g., Produce, Proteins, Dairy, Pantry).
            - Use bullet points.
            - Do not include the original diet chart, just the shopping list.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = "Here is my diet chart:\n$dietChart\n\nPlease generate a shopping list based on this chart."))
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = executeGeminiCallWithBackoff(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not generate shopping list."
        } catch (e: HttpException) {
            if (e.code() == 429) "The AI is currently busy due to high traffic. Retries exhausted. Please try again in a minute."
            else "Error: ${e.message}"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun generatePremiumRecipe(query: String, profile: UserProfile?): String = withContext(Dispatchers.IO) {
        // API Key logic is handled by executeGeminiCallWithBackoff
        
        val contextPrompt = if (profile != null) {
            "User Context: ${profile.age}yo ${profile.gender}, Goal: ${profile.healthGoals}, Restrictions: ${profile.dietaryRestrictions}. "
        } else ""
        
        val systemInstruction = """
            You are 'KardIQ AI', an expert in Premium Functional Foods and Medicinal Recipes, strictly following the Lifestyle Modification (LCHF) protocol from the JK Lifestyle handbook.
            The user wants a medicinal recipe based on their query.
            
            RULES:
            1. Suggest a highly medicinal, functional recipe (e.g., Turmeric Golden Milk, ACV Tonics, Bone Broth, LCHF Keto Smoothies).
            2. The recipe MUST be 100% LCHF compliant (No sugar, no grains, no seed oils, no lentils).
            3. Use bullet points (•) and empty lines between points for readability.
            4. State the medicinal benefits of the recipe.
            5. Provide exact ingredients and step-by-step instructions.
            6. At the VERY END of your response, output exactly: YOUTUBE_SEARCH: [Exact Name of Recipe]
              
            $contextPrompt
        """.trimIndent()
        
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = "Suggest a premium functional medicinal recipe for: $query"))
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        
        try {
            val response = executeGeminiCallWithBackoff(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not generate recipe."
        } catch (e: HttpException) {
            if (e.code() == 429) "The AI is currently busy due to high traffic. Retries exhausted. Please try again in a minute."
            else "Error: ${e.message}"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }


    fun getAllSavedCharts() = savedDietChartDao.getAllSavedCharts()
    suspend fun saveDietChart(chart: com.example.data.local.SavedDietChart): Boolean {
        savedDietChartDao.insertChart(chart)
        return FirebaseManager.syncSavedDietChart(chart)
    }

    suspend fun deleteDietChart(chart: com.example.data.local.SavedDietChart): Boolean {
        savedDietChartDao.deleteChart(chart)
        return FirebaseManager.deleteSavedDietChart(chart)
    }
    
    fun getAllSavedWorkouts() = savedWorkoutDao.getAllSavedWorkouts()
    
    suspend fun saveWorkout(workout: com.example.data.local.SavedWorkout): Boolean {
        savedWorkoutDao.insertWorkout(workout)
        return FirebaseManager.syncSavedWorkout(workout)
    }
    
    suspend fun deleteWorkout(workout: com.example.data.local.SavedWorkout): Boolean {
        savedWorkoutDao.deleteWorkout(workout)
        return FirebaseManager.deleteSavedWorkout(workout)
    }

    fun getAllSavedChats() = savedChatDao.getAllSavedChats()

    suspend fun saveChat(chat: com.example.data.local.SavedChat): Boolean {
        savedChatDao.insertChat(chat)
        return FirebaseManager.syncSavedChat(chat)
    }

    suspend fun deleteChat(chat: com.example.data.local.SavedChat): Boolean {
        savedChatDao.deleteChat(chat)
        return FirebaseManager.deleteSavedChat(chat)
    }

    fun getMedicalRecords(): kotlinx.coroutines.flow.Flow<List<com.example.data.local.MedicalRecord>> =
        medicalRecordDao?.getAllMedicalRecords() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun addMedicalRecord(record: com.example.data.local.MedicalRecord): Boolean {
        medicalRecordDao?.insertMedicalRecord(record)
        return FirebaseManager.syncMedicalRecord(record)
    }

    suspend fun deleteMedicalRecord(record: com.example.data.local.MedicalRecord): Boolean {
        medicalRecordDao?.deleteMedicalRecord(record)
        return FirebaseManager.deleteMedicalRecordRemote(record.cloudId)
    }

    suspend fun resolveYoutubeVideoId(searchQuery: String): String? = withContext(Dispatchers.IO) {
        val trimmedQuery = searchQuery.trim()
        val normalizedQuery = trimmedQuery.lowercase()
        if (normalizedQuery.isBlank()) return@withContext null

        try {
            val cached = youtubeVideoCacheDao?.getByQuery(normalizedQuery)
            if (cached != null && cached.videoId.isNotBlank()) {
                return@withContext cached.videoId
            }
        } catch (e: Exception) {
            // Proceed to network on cache failure
        }

        val apiKey = try {
            val key = com.example.BuildConfig.YOUTUBE_API_KEY
            if (key.isBlank() || key.startsWith("MY_YOUTUBE_API_KEY") || key == "default") null else key
        } catch (e: Throwable) {
            null
        }

        if (apiKey == null) {
            return@withContext null
        }

        try {
            val response = com.example.data.remote.YouTubeClient.service.searchVideo(
                query = trimmedQuery,
                apiKey = apiKey
            )
            val videoId = response.items?.firstOrNull()?.id?.videoId
            if (!videoId.isNullOrBlank()) {
                try {
                    youtubeVideoCacheDao?.insert(
                        com.example.data.local.YoutubeVideoCache(
                            query = normalizedQuery,
                            videoId = videoId
                        )
                    )
                } catch (e: Exception) {
                    // Ignore cache insert error
                }
                videoId
            } else {
                null
            }
        } catch (e: Exception) {
            // 403 quota exceeded, bad key, or network failure - return null without crashing
            null
        }
    }

    private var cachedExerciseLibrary: List<com.example.data.local.LibraryExercise>? = null

    suspend fun getExerciseLibrary(context: android.content.Context): List<com.example.data.local.LibraryExercise> = withContext(Dispatchers.IO) {
        cachedExerciseLibrary?.let { return@withContext it }
        try {
            val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            val listType = com.squareup.moshi.Types.newParameterizedType(
                List::class.java,
                com.example.data.local.LibraryExercise::class.java
            )
            val adapter = com.example.data.remote.RetrofitClient.moshi.adapter<List<com.example.data.local.LibraryExercise>>(listType)
            val parsed = adapter.fromJson(json) ?: emptyList()
            cachedExerciseLibrary = parsed
            parsed
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Error loading exercise library", e)
            emptyList()
        }
    }
}