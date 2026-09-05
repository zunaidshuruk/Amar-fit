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
import kotlinx.coroutines.withContext

import com.example.presentation.viewmodel.ChatMessage

class AppRepository(
    private val userDao: UserDao,
    private val metricsDao: MetricsDao,
    private val savedDietChartDao: com.example.data.local.SavedDietChartDao
) {

    suspend fun syncDataOnLogin() {
        FirebaseManager.pullDataOnLogin(userDao, metricsDao)
    }

    private suspend fun executeGeminiCallWithBackoff(request: GenerateContentRequest, maxRetries: Int = 3): GenerateContentResponse {
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

        if (apiKeys.isEmpty()) {
            throw Exception("API Key is missing or invalid. Please configure GEMINI_API_KEY_3 in the Secrets panel.")
        }

        var currentDelay = 1000L
        for (attempt in 0..maxRetries) {
            for (apiKey in apiKeys) {
                try {
                    return RetrofitClient.service.generateContent(apiKey, request)
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

    val userProfile = userDao.getUserProfile()
    
    fun getMetricsForDate(date: String) = metricsDao.getMetricsForDate(date)

    fun getMetricsHistory(limit: Int) = metricsDao.getMetricsHistory(limit)

    suspend fun saveUserProfile(profile: UserProfile) {
        userDao.insertProfile(profile)
        try {
            FirebaseManager.syncProfile(profile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun saveMetrics(metric: DailyMetric) {
        metricsDao.insertMetrics(metric)
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
        
        if (metric.waterLiters >= profile.dailyWaterLimitLiters && !currentBadges.contains("Hydration Hero")) {
            currentBadges.add("Hydration Hero")
            pointsToAdd += 50
        }
        
        if (metric.steps >= 10000 && !currentBadges.contains("10k Steps Master")) {
            currentBadges.add("10k Steps Master")
            pointsToAdd += 100
        }

        if (!currentBadges.contains("Consistency Starter")) {
            currentBadges.add("Consistency Starter")
            pointsToAdd += 20
        }
        
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
            You are 'Amar-Fit AI', a universal health, fitness, and wellness bot powered by a vast knowledge bank.
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
    }

    suspend fun saveFoodLog(foodLog: com.example.data.local.FoodLog) {
        metricsDao.insertFoodLog(foodLog)
    }

    suspend fun updateFoodLog(foodLog: com.example.data.local.FoodLog) {
        metricsDao.updateFoodLog(foodLog)
    }

    suspend fun analyzeFoodText(foodText: String): String = withContext(Dispatchers.IO) {
        // API Key logic is handled by executeGeminiCallWithBackoff
        
        val systemInstruction = """
            You are an expert AI food analyzer specializing in nutrition.
            Based on the provided text description of a meal, identify the food, estimate the portion size, and provide a rough estimate of the total calories and macronutrients.
            You MUST return ONLY a raw JSON object with NO markdown formatting, NO code blocks, and NO extra text.
            The JSON MUST have these exact keys:
            "name" (string), "category" (string), "calories" (integer), "description" (string, include macros and details here).
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
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: """{"name": "Error", "category": "Error", "calories": 0, "description": "Could not analyze the food."}"""
        } catch (e: HttpException) {
            val msg = if (e.code() == 429) "AI quota exceeded. Retries exhausted (429)." else "Error: ${e.message}"
            """{"name": "Error", "category": "Error", "calories": 0, "description": "$msg"}"""
        } catch (e: Exception) {
            """{"name": "Error", "category": "Error", "calories": 0, "description": "Error: ${e.message}"}"""
        }
    }

    suspend fun analyzeFoodImage(base64Image: String): String = withContext(Dispatchers.IO) {
        // API Key logic is handled by executeGeminiCallWithBackoff
        
        val systemInstruction = """
            You are an expert AI food analyzer specializing in Bangladeshi cuisine.
            Identify the food, estimate the portion size, and provide a rough estimate of the total calories and macronutrients.
            You MUST return ONLY a raw JSON object with NO markdown formatting, NO code blocks, and NO extra text.
            The JSON MUST have these exact keys:
            "name" (string), "category" (string), "calories" (integer), "description" (string, include macros and details here).
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
            val response = executeGeminiCallWithBackoff(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: """{"name": "Error", "category": "Error", "calories": 0, "description": "Could not analyze the image."}"""
        } catch (e: HttpException) {
            val msg = if (e.code() == 429) "AI quota exceeded. Retries exhausted (429)." else "Error: ${e.message}"
            """{"name": "Error", "category": "Error", "calories": 0, "description": "$msg"}"""
        } catch (e: Exception) {
            """{"name": "Error", "category": "Error", "calories": 0, "description": "Error: ${e.message}"}"""
        }
    }

    suspend fun generateCoachAdvice(topic: String, habit: String, benefits: String): String = withContext(Dispatchers.IO) {
        // API Key logic is handled by executeGeminiCallWithBackoff
        
        val systemInstruction = """
            You are 'Amar-Fit AI', an expert Wellness and Sleep Optimization Coach. 
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
    
    suspend fun generateWorkout(profile: UserProfile?): String = withContext(Dispatchers.IO) {
        // API Key logic is handled by executeGeminiCallWithBackoff
        
        val contextPrompt = if (profile != null) {
            "User Context: ${profile.age}yo ${profile.gender}, Weight: ${profile.weightKg}kg, Height: ${profile.heightCm}cm, Goal: ${profile.healthGoals}. "
        } else ""
        
        val systemInstruction = """
            You are 'Amar-Fit AI', an expert fitness coach. 
            Generate a personalized daily workout routine based on the user's profile.
            
            FORMATTING RULES:
            - Give the workout a catchy title.
            - Provide a brief warmup, main workout, and cooldown.
            - Use bullet points (•) and empty lines between points for spacing.
            - For EACH exercise, provide a YouTube search link in this exact format:
              https://www.youtube.com/results?search_query=[Exercise+Name+Here]
              
            $contextPrompt
        """.trimIndent()
        
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = "Please generate my personalized workout for today."))
                )
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )
        
        try {
            val response = executeGeminiCallWithBackoff(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not generate workout plan."
        } catch (e: HttpException) {
            if (e.code() == 429) "The AI is currently busy due to high traffic. Retries exhausted. Please try again in a minute."
            else "Error: ${e.message}"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
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
            You are 'Amar-Fit AI', an expert in Premium Functional Foods and Medicinal Recipes, strictly following the Lifestyle Modification (LCHF) protocol from the JK Lifestyle handbook.
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
    suspend fun saveDietChart(chart: com.example.data.local.SavedDietChart) = savedDietChartDao.insertChart(chart)
    suspend fun deleteDietChart(chart: com.example.data.local.SavedDietChart) = savedDietChartDao.deleteChart(chart)
}