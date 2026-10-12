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
    const val DEEP_REVIEW_PROMPT = "Give me a full 30-day deep review of my health data and a 7-day action plan."
    const val GO_DEEPER_PREFIX = "Go deeper on this: "

    fun isDeepRequest(message: String): Boolean {
        val trimmed = message.trim()
        return trimmed == DEEP_REVIEW_PROMPT || trimmed.startsWith(GO_DEEPER_PREFIX)
    }

    fun isHealthQuestion(message: String): Boolean {
        if (HealthFactsBuilder.detectTopics(message).isNotEmpty()) {
            return true
        }
        val lower = message.lowercase(Locale.US)
        val keywords = listOf(
            "my health", "my data", "my numbers", "how am i", "my progress",
            "what should i", "what should i do", "suggest", "advice", "improve", "lower my",
            "reduce", "diet", "workout", "meal", "exercise", "sleep", "glucose",
            "sugar", "pressure", "weight", "recommend", "plan", "food", "eat", "lunch", "dinner", "breakfast",
            "ঘুম", "সুগার", "প্রেসার", "ওজন", "স্বাস্থ্য"
        )
        return keywords.any { lower.contains(it) }
    }

    fun stripFooter(text: String): String {
        val markers = listOf(
            "\n\n*Based on your records",
            "\n\n*From KardIQ's lifestyle protocol",
            "\n\n*Check with your doctor",
            ON_DEVICE_FOOTER
        )
        val indices = markers.map { text.indexOf(it) }.filter { it >= 0 }
        return if (indices.isNotEmpty()) {
            text.substring(0, indices.minOrNull()!!)
        } else {
            text
        }
    }

    fun buildFooter(dataLabels: String, protocolNames: List<String>, doctorNote: Boolean): String {
        val sb = StringBuilder()
        if (dataLabels.isNotBlank()) {
            sb.append("\n\n*Based on your records: $dataLabels*")
        }
        if (protocolNames.isNotEmpty()) {
            sb.append("\n\n*From KardIQ's lifestyle protocol: ${protocolNames.joinToString("; ")}*")
        }
        if (doctorNote) {
            sb.append("\n\n*" + MedicalNotes.DOCTOR_LINE + "*")
        }
        sb.append(ON_DEVICE_FOOTER)
        return sb.toString()
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

    private val ALWAYS_CLOUD = listOf(
        "i ate", "i had ", "i drank", "for breakfast", "for lunch", "for dinner", "for snack",
        "log my", "log a", "add to my", "kcal", "macro", "meal plan", "recipe",
        "what should i eat", "workout plan", "workout program", "training plan",
        "workout routine", "generate", "create a plan", "make a plan", "hba1c", "mmol", "mg/dl",
        "dose", "dosage", "medicine", "medication", "insulin", "diabetes", "cholesterol",
        "normal", "too high", "too low", "dangerous", "is it safe", "healthy range",
        "target range", "diet chart", "diet plan", "diet",
        "ইনসুলিন", "ওষুধ", "রেসিপি", "ওয়ার্কআউট", "ডায়েট"
    )

    private val CLOUD_UNLESS_PERSONAL_DATA = listOf(
        "calorie", "protein", "carb", "glucose", "blood sugar", "sugar level",
        "blood pressure", "analyze", "analyse", "insight", "trend",
        "ক্যালরি", "সুগার", "প্রেসার", "খাবার"
    )

    fun isCloudOnlyTopic(message: String): Boolean {
        if (isDeepRequest(message)) {
            return true
        }
        val trimmed = message.trim()
        if (trimmed.length > 400 || trimmed.lines().size >= 3) {
            return true
        }
        val lower = trimmed.lowercase(Locale.US)
        if (ALWAYS_CLOUD.any { lower.contains(it) }) {
            return true
        }
        if (CLOUD_UNLESS_PERSONAL_DATA.any { lower.contains(it) } && !HealthFactsBuilder.isPersonalDataQuestion(message)) {
            return true
        }
        return false
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
            AssistantRoute.NEEDS_INTERNET -> "This kind of question (food logging, calories, diet and workout plans, deep health reviews, health numbers) needs the cloud AI, and you're offline. Please try again when you have a connection."
            AssistantRoute.NEEDS_CLOUD_ENGINE -> "This needs the cloud AI because it involves food, calories, plans, deep health reviews or health numbers. Switch the AI engine to Auto or Cloud in Settings > Offline AI (beta) and ask again."
            else -> ""
        }
    }

    val ON_DEVICE_SYSTEM_PROMPT: String =
        "You are KardIQ AI, a friendly health and fitness assistant inside the KardIQ app for users in Bangladesh. Answer in the same language the user writes in (English or Bangla). Be brief: at most 150 words unless the user asks for more. Use plain text and simple markdown (short bullet lists). Never use LaTeX or math markup; write numbers normally. You are not a doctor: do not diagnose, do not give medicine doses, and do not give blood-glucose, blood-pressure or calorie targets or normal ranges. You MAY suggest foods, drinks, habits and routines from the KARDIQ PROTOCOL section when one is provided: answer from it, name the entry you use, and never invent recipes, ingredients or amounts that are not in it. When a FACTS section is provided, use only the numbers in it, never calculate new totals or averages yourself, never say whether a number is good or bad, and if the user asks for data that is not in FACTS say you do not have it. For emergencies (chest pain, trouble breathing, severe bleeding, fainting, stroke signs, thoughts of self-harm) tell the user to call 999 or go to the nearest hospital right now. Never claim you logged or changed anything in the app. " + DeshiFoodRules.SHORT

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
