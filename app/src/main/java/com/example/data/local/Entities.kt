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
    val waistCm: Float = 0f,
    val neckCm: Float = 0f,
    val hipCm: Float = 0f,
    val bodyFatCalcGender: String = "",
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
    val hasAcceptedPrivacyPolicy: Boolean = false,
    /** Which version of the privacy policy was accepted (LegalContent.POLICY_VERSION); 0 = before versions existed. */
    val acceptedPolicyVersion: Int = 0,
    /** Cycle tracker: offered only to Female profiles, off until the person switches it on and agrees (cycleConsentAt > 0). */
    val cycleTrackingEnabled: Boolean = false,
    val cycleConsentAt: Long = 0L,
    /** The AI Assistant may use a short cycle summary. On by default (shown as ON on the consent screen so the person can turn it off). */
    val cycleAiEnabled: Boolean = true,
    /** Cycle notifications show only a generic text on the lock screen until the phone is unlocked. */
    val cycleDiscreetNotifications: Boolean = true,
    val cycleHealthConnectSync: Boolean = false,
    /** Typical values used for predictions until there is history. */
    val cycleDefaultLength: Int = 28,
    val cyclePeriodDefaultLength: Int = 5,
    /** Optional Ramadan fasting make-up tracking inside the cycle tracker (off by default; it reveals religious practice). */
    val ramadanTrackingEnabled: Boolean = false,
    /** Local moon sighting can start Ramadan a day later or earlier than the built-in calendar: -2 to +2 days. */
    val ramadanOffsetDays: Int = 0,
    val friendCode: String = "",
    val lastActiveDate: String = "",
    val profilePictureUri: String? = null,
    val isDarkMode: Boolean = false,
    val useImperialUnits: Boolean = false,
    val glucoseUnitMgdl: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val selectedLanguage: String = "English",
    val todayTileSlots: String = "",
    val bloodGlucoseTargetMin: Float = 0f,
    val bloodGlucoseTargetMax: Float = 0f,
    val navTabs: String = "",
    /** Everything in Settings > Notifications as JSON (see NotificationPrefs). Empty = defaults. */
    val notificationPrefsJson: String = "",
    /** Workout days per week the user aims for (weekly workout goal notification). */
    val weeklyWorkoutGoal: Int = 3,
    /** The color theme chosen under Settings > App theme (an id from ThemePalettes). Unknown ids fall back to the default. */
    val themeId: String = "chartreuse",
    val updatedAt: Long = 0L
)

@Entity(tableName = "daily_metrics")
data class DailyMetric(
    @PrimaryKey val date: String = "", 
    val caloriesConsumed: Int = 0,
    val waterLiters: Float = 0f,
    val steps: Int = 0,
    val bloodGlucoseMorning: Float = 0f,
    val bloodGlucoseNight: Float = 0f,
    val bloodGlucoseBeforeBreakfast: Float = 0f,
    val bloodGlucoseAfterBreakfast: Float = 0f,
    val bloodGlucoseBeforeLunch: Float = 0f,
    val bloodGlucoseAfterLunch: Float = 0f,
    val bloodGlucoseBeforeDinner: Float = 0f,
    val bloodGlucoseAfterDinner: Float = 0f,
    val bloodGlucoseSpecimenSource: String = "Not set",
    val bloodPressure: String = "", 
    val bloodPressureBodyPosition: String = "Not set",
    val bloodPressureArmLocation: String = "Not set",
    val weightKg: Float = 0f,
    val sleepHours: Float = 0f,
    val heartRate: Int = 0,
    val heartRateMin: Int = 0,
    val heartRateMax: Int = 0,
    val restingHeartRate: Int = 0,
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
    val fatG: Float = 0f,
    val updatedAt: Long = 0L
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
    val fatG: Float = 0f,
    val portionAmount: Float = 100f,
    val portionUnit: String = "g",
    val sodiumMg: Float = 0f,
    val sugarG: Float = 0f,
    val fiberG: Float = 0f
)
