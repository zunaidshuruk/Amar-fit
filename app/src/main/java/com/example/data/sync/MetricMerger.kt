package com.example.data.sync

import com.example.data.local.DailyMetric

object MetricMerger {
    fun merge(a: DailyMetric, b: DailyMetric): DailyMetric {
        if (a.date != b.date) return a

        val bNewer = b.updatedAt > a.updatedAt

        fun <T> pickReading(
            local: T,
            remote: T,
            isBNewer: Boolean,
            isEmpty: (T) -> Boolean
        ): T {
            val localEmpty = isEmpty(local)
            val remoteEmpty = isEmpty(remote)
            return when {
                localEmpty && !remoteEmpty -> remote
                !localEmpty && remoteEmpty -> local
                localEmpty && remoteEmpty -> local
                else -> if (isBNewer) remote else local
            }
        }

        fun isFloatEmpty(v: Float) = v == 0f
        fun isIntEmpty(v: Int) = v == 0
        fun isStringEmpty(v: String) = v.isBlank() || v == "Not set"

        // Blood Pressure Bundle
        val bpLocalEmpty = isStringEmpty(a.bloodPressure)
        val bpRemoteEmpty = isStringEmpty(b.bloodPressure)
        val useBForBP = when {
            bpLocalEmpty && !bpRemoteEmpty -> true
            !bpLocalEmpty && bpRemoteEmpty -> false
            bpLocalEmpty && bpRemoteEmpty -> false
            else -> bNewer
        }

        return a.copy(
            // COUNTERS (max)
            caloriesConsumed = maxOf(a.caloriesConsumed, b.caloriesConsumed),
            waterLiters = maxOf(a.waterLiters, b.waterLiters),
            steps = maxOf(a.steps, b.steps),
            distanceMeters = maxOf(a.distanceMeters, b.distanceMeters),
            exerciseMinutes = maxOf(a.exerciseMinutes, b.exerciseMinutes),
            externalNutritionCalories = maxOf(a.externalNutritionCalories, b.externalNutritionCalories),
            activeCaloriesBurned = maxOf(a.activeCaloriesBurned, b.activeCaloriesBurned),
            mindfulnessMinutes = maxOf(a.mindfulnessMinutes, b.mindfulnessMinutes),
            carbsG = maxOf(a.carbsG, b.carbsG),
            proteinG = maxOf(a.proteinG, b.proteinG),
            fatG = maxOf(a.fatG, b.fatG),

            // Heart Rate Min/Max
            heartRateMin = when {
                a.heartRateMin == 0 -> b.heartRateMin
                b.heartRateMin == 0 -> a.heartRateMin
                else -> minOf(a.heartRateMin, b.heartRateMin)
            },
            heartRateMax = maxOf(a.heartRateMax, b.heartRateMax),

            // READINGS (newer if both present)
            bloodGlucoseMorning = pickReading(a.bloodGlucoseMorning, b.bloodGlucoseMorning, bNewer, { isFloatEmpty(it) }),
            bloodGlucoseNight = pickReading(a.bloodGlucoseNight, b.bloodGlucoseNight, bNewer, { isFloatEmpty(it) }),
            bloodGlucoseBeforeBreakfast = pickReading(a.bloodGlucoseBeforeBreakfast, b.bloodGlucoseBeforeBreakfast, bNewer, { isFloatEmpty(it) }),
            bloodGlucoseAfterBreakfast = pickReading(a.bloodGlucoseAfterBreakfast, b.bloodGlucoseAfterBreakfast, bNewer, { isFloatEmpty(it) }),
            bloodGlucoseBeforeLunch = pickReading(a.bloodGlucoseBeforeLunch, b.bloodGlucoseBeforeLunch, bNewer, { isFloatEmpty(it) }),
            bloodGlucoseAfterLunch = pickReading(a.bloodGlucoseAfterLunch, b.bloodGlucoseAfterLunch, bNewer, { isFloatEmpty(it) }),
            bloodGlucoseBeforeDinner = pickReading(a.bloodGlucoseBeforeDinner, b.bloodGlucoseBeforeDinner, bNewer, { isFloatEmpty(it) }),
            bloodGlucoseAfterDinner = pickReading(a.bloodGlucoseAfterDinner, b.bloodGlucoseAfterDinner, bNewer, { isFloatEmpty(it) }),
            weightKg = pickReading(a.weightKg, b.weightKg, bNewer, { isFloatEmpty(it) }),
            sleepHours = pickReading(a.sleepHours, b.sleepHours, bNewer, { isFloatEmpty(it) }),
            heartRate = pickReading(a.heartRate, b.heartRate, bNewer, { isIntEmpty(it) }),
            restingHeartRate = pickReading(a.restingHeartRate, b.restingHeartRate, bNewer, { isIntEmpty(it) }),
            heartRateVariability = pickReading(a.heartRateVariability, b.heartRateVariability, bNewer, { isFloatEmpty(it) }),
            oxygenSaturation = pickReading(a.oxygenSaturation, b.oxygenSaturation, bNewer, { isFloatEmpty(it) }),
            skinTemperatureCelsius = pickReading(a.skinTemperatureCelsius, b.skinTemperatureCelsius, bNewer, { isFloatEmpty(it) }),
            respiratoryRate = pickReading(a.respiratoryRate, b.respiratoryRate, bNewer, { isFloatEmpty(it) }),

            // Bundles / Special
            bloodPressure = if (useBForBP) b.bloodPressure else a.bloodPressure,
            bloodPressureBodyPosition = if (useBForBP) b.bloodPressureBodyPosition else a.bloodPressureBodyPosition,
            bloodPressureArmLocation = if (useBForBP) b.bloodPressureArmLocation else a.bloodPressureArmLocation,
            bloodGlucoseSpecimenSource = pickReading(a.bloodGlucoseSpecimenSource, b.bloodGlucoseSpecimenSource, bNewer, { isStringEmpty(it) }),

            updatedAt = maxOf(a.updatedAt, b.updatedAt)
        )
    }
}
