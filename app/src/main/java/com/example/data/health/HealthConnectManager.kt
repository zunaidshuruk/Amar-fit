package com.example.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord

object HealthConnectManager {
    val REQUIRED_PERMISSIONS: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(SkinTemperatureRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(BloodPressureRecord::class),
        HealthPermission.getWritePermission(BloodGlucoseRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(MindfulnessSessionRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(BasalMetabolicRateRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class)
    )

    val HISTORY_PERMISSION: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

    fun isAvailable(context: Context): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    @OptIn(androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi::class)
    suspend fun isHistorySupported(context: Context): Boolean {
        return try {
            if (!isAvailable(context)) return false
            val client = HealthConnectClient.getOrCreate(context)
            client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasHistoryPermission(context: Context): Boolean {
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            HISTORY_PERMISSION in granted
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasAllPermissions(context: Context): Boolean {
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(REQUIRED_PERMISSIONS)
        } catch (e: Exception) {
            false
        }
    }

    // Some newer/optional record types (HRV, Skin Temperature, Respiratory Rate, Mindfulness)
    // aren't supported on every installed Health Connect version, so a device can permanently
    // fail hasAllPermissions() even after the user has granted everything the OS actually offers.
    // Use this wherever "is Health Connect usable at all" is the real question (e.g. gating a
    // sync action) — syncWithHealthConnect() already reads each metric in its own try-catch and
    // simply skips whatever wasn't granted, so a partial grant still works correctly.
    suspend fun hasAnyPermissions(context: Context): Boolean {
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            granted.any { it in REQUIRED_PERMISSIONS }
        } catch (e: Exception) {
            false
        }
    }

    fun getPermissionDisplayName(permission: String): String {
        val key = permission.substringAfterLast('.')
            .removePrefix("READ_").removePrefix("WRITE_")
            .replace("_", "")
            .lowercase()
        return when (key) {
            "steps" -> "Steps"
            "sleep" -> "Sleep"
            "exercise" -> "Exercise"
            "nutrition" -> "Nutrition"
            "hydration" -> "Hydration"
            "heartrate" -> "Heart Rate"
            "restingheartrate" -> "Resting Heart Rate"
            "heartratevariability" -> "Heart Rate Variability (HRV)"
            "bloodpressure" -> "Blood Pressure"
            "bloodglucose" -> "Blood Glucose"
            "distance" -> "Distance"
            "activecaloriesburned" -> "Active Calories"
            "totalcaloriesburned" -> "Total Calories Burned"
            "oxygensaturation" -> "Oxygen Saturation (SpO2)"
            "skintemperature" -> "Skin Temperature"
            "respiratoryrate" -> "Respiratory Rate"
            "mindfulness" -> "Mindfulness"
            "speed" -> "Speed"
            "basalmetabolicrate" -> "Basal Metabolic Rate"
            "weight" -> "Weight"
            "healthdatahistory" -> "Full Health History"
            else -> permission.substringAfterLast('.').replace("Record", "")
        }
    }

    fun exerciseTypeLabel(type: Int): String {
        return when (type) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Running"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walking"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Cycling"
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hiking"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swimming"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
            ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "Pilates"
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength Training"
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
            ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "Dancing"
            ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> "Stretching"
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "Calisthenics"
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "Rowing"
            ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Elliptical"
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> "Stair Climbing"
            ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> "Badminton"
            ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> "Tennis"
            ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> "Table Tennis"
            ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> "Basketball"
            ExerciseSessionRecord.EXERCISE_TYPE_CRICKET -> "Cricket"
            ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> "Martial Arts"
            ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> "Boxing"
            ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> "Golf"
            ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "Football"
            ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL -> "Volleyball"
            ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL -> "Handball"
            ExerciseSessionRecord.EXERCISE_TYPE_SQUASH -> "Squash"
            ExerciseSessionRecord.EXERCISE_TYPE_RUGBY -> "Rugby"
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Weightlifting"
            ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> "Gymnastics"
            ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING -> "Climbing"
            ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS -> "Fitness Class"
            ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> "Paddling"
            ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> "Surfing"
            ExerciseSessionRecord.EXERCISE_TYPE_SKATING -> "Skating"
            else -> "Workout"
        }
    }
}
