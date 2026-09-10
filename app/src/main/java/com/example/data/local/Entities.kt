package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val age: Int = 0,
    val onboardingCompleted: Boolean = false,
    val dateOfBirth: String = "",
    val gender: String = "",
    val heightCm: Float = 0f,
    val weightKg: Float = 0f,
    val dietaryRestrictions: String = "",
    val healthGoals: String = "",
    val dailyCalorieLimit: Int = 0,
    val dailyWaterLimitLiters: Float = 0f,
    val stepGoal: Int = 10000,
    val stepGoalIsAuto: Boolean = true,
    val sleepGoalHours: Float = 8f,
    val sleepGoalIsAuto: Boolean = true,
    val waterGoalIsAuto: Boolean = true,
    val calorieGoalIsAuto: Boolean = true,
    val activityLevel: String = "Moderate",
    val currentStreak: Int = 0,
    val points: Int = 0,
    val badges: String = "", 
    val lastActiveDate: String = "",
    val profilePictureUri: String? = null,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val selectedLanguage: String = "English",
    val todayTileSlots: String = ""
)

@Entity(tableName = "daily_metrics")
data class DailyMetric(
    @PrimaryKey val date: String = "", 
    val caloriesConsumed: Int = 0,
    val waterLiters: Float = 0f,
    val steps: Int = 0,
    val bloodGlucoseMorning: Float = 0f,
    val bloodGlucoseNight: Float = 0f,
    val bloodPressure: String = "", 
    val weightKg: Float = 0f,
    val sleepHours: Float = 0f,
    val heartRate: Int = 0,
    val heartRateMin: Int = 0,
    val heartRateMax: Int = 0,
    val distanceMeters: Float = 0f,
    val exerciseMinutes: Int = 0,
    val externalNutritionCalories: Int = 0,
    val activeCaloriesBurned: Int = 0,
    val heartRateVariability: Float = 0f,
    val oxygenSaturation: Float = 0f,
    val skinTemperatureCelsius: Float = 0f,
    val respiratoryRate: Float = 0f,
    val mindfulnessMinutes: Int = 0,
    val carbsG: Float = 0f,
    val proteinG: Float = 0f,
    val fatG: Float = 0f
)

@Entity(tableName = "food_logs", indices = [Index(value = ["cloudId"], unique = true)])
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cloudId: String = UUID.randomUUID().toString(),
    val date: String = "",
    val name: String = "",
    val category: String = "",
    val calories: Int = 0,
    val description: String = "",
    val time: String = "",
    val mealType: String = "",
    val carbsG: Float = 0f,
    val proteinG: Float = 0f,
    val fatG: Float = 0f
)
