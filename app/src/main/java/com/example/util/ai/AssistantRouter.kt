package com.example.util.ai

import com.example.data.local.DailyMetric
import com.example.data.local.UserProfile
import java.util.Locale
import kotlin.math.roundToInt

enum class AiEngineSetting {
    CLOUD,
    AUTO,
    ON_DEVICE
}

enum class AssistantRoute {
    CLOUD,
    ON_DEVICE,
    NEEDS_SETUP,
    NEEDS_INTERNET,
    NEEDS_CLOUD_ENGINE
}

object AssistantRouter {

    const val ON_DEVICE_FOOTER = "\n\n*Answered on this phone*"

    fun stripFooter(text: String): String {
        return if (text.endsWith(ON_DEVICE_FOOTER)) {
            text.substring(0, text.length - ON_DEVICE_FOOTER.length)
        } else {
            text
        }
    }

    fun isEmergency(message: String): Boolean {
        val lower = message.lowercase(Locale.US)
        val keywords = listOf(
            "chest pain", "can't breathe", "cannot breathe", "trouble breathing",
            "unconscious", "fainted", "stroke", "severe bleeding", "overdose",
            "suicide", "want to die", "heart attack", "বুকে ব্যথা", "শ্বাস নিতে পারছি না"
        )
        return keywords.any { lower.contains(it) }
    }

    fun isCloudOnlyTopic(message: String): Boolean {
        val trimmed = message.trim()
        if (trimmed.length > 400 || trimmed.lines().size >= 3) {
            return true
        }
        val lower = trimmed.lowercase(Locale.US)
        val keywords = listOf(
            "i ate", "i had ", "i drank", "for breakfast", "for lunch", "for dinner", "for snack",
            "log my", "log a", "add to my", "calorie", "kcal", "macro", "protein", "carb",
            "diet", "meal plan", "recipe", "what should i eat", "workout plan", "workout program",
            "training plan", "routine", "generate", "create a plan", "make a plan",
            "glucose", "blood sugar", "sugar level", "hba1c", "blood pressure", "mmol", "mg/dl",
            "dose", "dosage", "medicine", "medication", "insulin", "diabetes", "cholesterol",
            "normal range", "target range", "analyze", "analyse", "trend", "insight",
            "my data", "last week", "this week",
            "ক্যালরি", "খাবার", "ডায়েট", "সুগার", "প্রেসার", "ওষুধ", "ইনসুলিন", "ওয়ার্কআউট", "রেসিপি"
        )
        return keywords.any { lower.contains(it) }
    }

    fun route(message: String, setting: AiEngineSetting, modelReady: Boolean, online: Boolean): AssistantRoute {
        when (setting) {
            AiEngineSetting.CLOUD -> return AssistantRoute.CLOUD
            AiEngineSetting.ON_DEVICE -> {
                if (!modelReady) return AssistantRoute.NEEDS_SETUP
                val emergency = isEmergency(message)
                val cloudOnly = isCloudOnlyTopic(message)
                if (cloudOnly && !emergency) {
                    return AssistantRoute.NEEDS_CLOUD_ENGINE
                }
                return AssistantRoute.ON_DEVICE
            }
            AiEngineSetting.AUTO -> {
                if (!modelReady) return AssistantRoute.CLOUD
                val emergency = isEmergency(message)
                if (emergency) return AssistantRoute.ON_DEVICE
                val cloudOnly = isCloudOnlyTopic(message)
                if (cloudOnly) {
                    return if (online) AssistantRoute.CLOUD else AssistantRoute.NEEDS_INTERNET
                }
                return AssistantRoute.ON_DEVICE
            }
        }
    }

    fun notice(route: AssistantRoute): String {
        return when (route) {
            AssistantRoute.NEEDS_SETUP -> "Offline AI isn't set up yet. Open Settings > Offline AI (beta) to download the model, or switch the AI engine to Cloud."
            AssistantRoute.NEEDS_INTERNET -> "This kind of question (food logging, calories, diet and workout plans, health numbers) needs the cloud AI, and you're offline. Please try again when you have a connection."
            AssistantRoute.NEEDS_CLOUD_ENGINE -> "This needs the cloud AI because it involves food, calories, plans or health numbers. Switch the AI engine to Auto or Cloud in Settings > Offline AI (beta) and ask again."
            else -> ""
        }
    }

    const val ON_DEVICE_SYSTEM_PROMPT: String =
        "You are KardIQ AI, a friendly health and fitness assistant inside the KardIQ app for users in Bangladesh. Answer in the same language the user writes in (English or Bangla). Be brief: at most 120 words unless the user asks for more. Use plain text and simple markdown (short bullet lists). Never use LaTeX or math markup; write numbers normally. You are not a doctor. Do not give diagnoses, medicine doses, or blood-glucose, blood-pressure or calorie targets or ranges; say you cannot give exact medical numbers here and suggest asking a doctor or using the app's own health goals. For emergencies (chest pain, trouble breathing, severe bleeding, fainting, stroke signs, thoughts of self-harm) tell the user to call 999 or go to the nearest hospital right now. Never claim you logged or changed anything in the app."

    fun buildContext(profile: UserProfile, metrics: List<DailyMetric>): String {
        val profileParts = mutableListOf<String>()
        if (profile.age > 0) profileParts.add("age ${profile.age}")
        if (profile.gender.isNotBlank()) profileParts.add("gender ${profile.gender}")
        if (profile.weightKg > 0f) profileParts.add(String.format(Locale.US, "%.1f kg", profile.weightKg))
        if (profile.heightCm > 0f) profileParts.add(String.format(Locale.US, "%.1f cm", profile.heightCm))
        if (profile.activityLevel.isNotBlank()) profileParts.add("activity ${profile.activityLevel}")

        val profileStr = if (profileParts.isNotEmpty()) "User: ${profileParts.joinToString(", ")}." else ""

        val sortedMetrics = metrics.sortedBy { it.date }
        val todayMetric = sortedMetrics.maxByOrNull { it.date }

        val todayParts = mutableListOf<String>()
        if (todayMetric != null) {
            if (todayMetric.steps > 0) todayParts.add("${todayMetric.steps} steps")
            if (todayMetric.sleepHours > 0f) todayParts.add(String.format(Locale.US, "%.1f h sleep", todayMetric.sleepHours))
            if (todayMetric.waterLiters > 0f) todayParts.add(String.format(Locale.US, "%.1f L water", todayMetric.waterLiters))
        }
        val todayStr = if (todayParts.isNotEmpty()) "Today: ${todayParts.joinToString(", ")}." else ""

        val stepDays = metrics.filter { it.steps > 0 }
        val avgSteps = if (stepDays.isNotEmpty()) stepDays.map { it.steps }.average().roundToInt() else 0

        val sleepDays = metrics.filter { it.sleepHours > 0f }
        val avgSleep = if (sleepDays.isNotEmpty()) sleepDays.map { it.sleepHours }.average().toFloat() else 0f

        val avgParts = mutableListOf<String>()
        if (avgSteps > 0) avgParts.add("$avgSteps steps")
        if (avgSleep > 0f) avgParts.add(String.format(Locale.US, "%.1f h sleep", avgSleep))

        val avgStr = if (avgParts.isNotEmpty()) "7-day average: ${avgParts.joinToString(", ")}." else ""

        return listOf(profileStr, todayStr, avgStr).filter { it.isNotBlank() }.joinToString(" ")
    }
}
