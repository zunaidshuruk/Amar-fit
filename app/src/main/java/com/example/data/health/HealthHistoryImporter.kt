package com.example.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import kotlin.reflect.KClass

data class Day(
    val steps: Int = 0,
    val distanceMeters: Float = 0f,
    val activeCalories: Int = 0,
    val sleepHours: Float = 0f,
    val exerciseMinutes: Int = 0,
    val heartRateAvg: Int = 0,
    val heartRateMin: Int = 0,
    val heartRateMax: Int = 0,
    val restingHeartRate: Int = 0,
    val hrv: Float = 0f,
    val spo2: Float = 0f,
    val skinTemp: Float = 0f,
    val respiratoryRate: Float = 0f,
    val mindfulnessMinutes: Int = 0,
    val bloodPressure: String = "",
    val bloodGlucose: Float = 0f
)

private class DayBuilder {
    var steps: Int = 0
    var distanceMeters: Float = 0f
    var activeCalories: Int = 0
    var sleepHours: Float = 0f
    var exerciseMinutes: Int = 0
    var heartRateAvg: Int = 0
    var heartRateMin: Int = 0
    var heartRateMax: Int = 0
    var restingHeartRate: Int = 0
    var hrv: Float = 0f
    var spo2: Float = 0f
    var skinTemp: Float = 0f
    var respiratoryRate: Float = 0f
    var mindfulnessMinutes: Int = 0
    var bloodPressure: String = ""
    var bloodGlucose: Float = 0f

    fun build(): Day = Day(
        steps = steps,
        distanceMeters = distanceMeters,
        activeCalories = activeCalories,
        sleepHours = sleepHours,
        exerciseMinutes = exerciseMinutes,
        heartRateAvg = heartRateAvg,
        heartRateMin = heartRateMin,
        heartRateMax = heartRateMax,
        restingHeartRate = restingHeartRate,
        hrv = hrv,
        spo2 = spo2,
        skinTemp = skinTemp,
        respiratoryRate = respiratoryRate,
        mindfulnessMinutes = mindfulnessMinutes,
        bloodPressure = bloodPressure,
        bloodGlucose = bloodGlucose
    )
}

object HealthHistoryImporter {
    @Volatile
    var lastReport: String = ""

    suspend fun readDays(
        context: Context,
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<String, Day> {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
            lastReport = "Health Connect is not available on this phone."
            return emptyMap()
        }

        val client = try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            lastReport = "Health Connect is not available on this phone."
            return emptyMap()
        }

        val granted = try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            lastReport = "Could not read Health Connect permissions."
            return emptyMap()
        }

        fun hasRead(recordClass: KClass<out Record>): Boolean {
            return HealthPermission.getReadPermission(recordClass) in granted
        }

        val errors = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        lastReport = ""

        fun recordError(metricName: String, e: Exception) {
            e.printStackTrace()
            if (errors.none { it.startsWith("$metricName:") }) {
                errors.add("$metricName: ${e.javaClass.simpleName} ${e.message?.take(80) ?: ""}".trim())
            }
        }

        val daysMap = mutableMapOf<String, DayBuilder>()
        val zoneId = ZoneId.systemDefault()

        var currentChunkStart = startDate
        while (!currentChunkStart.isAfter(endDate)) {
            val chunkEnd = if (currentChunkStart.plusDays(30).isAfter(endDate)) endDate else currentChunkStart.plusDays(30)
            val chunkEndExclusive = chunkEnd.plusDays(1)
            val startTimeInstant = currentChunkStart.atStartOfDay(zoneId).toInstant()
            val endTimeInstant = chunkEndExclusive.atStartOfDay(zoneId).toInstant()
            val timeRangeFilter = TimeRangeFilter.between(startTimeInstant, endTimeInstant)

            // 1. Steps
            if (hasRead(StepsRecord::class)) {
                try {
                    val req = AggregateGroupByPeriodRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = timeRangeFilter,
                        timeRangeSlicer = Period.ofDays(1)
                    )
                    val response = client.aggregateGroupByPeriod(req)
                    for (item in response) {
                        val dateStr = item.startTime.atZone(zoneId).toLocalDate().toString()
                        val count = item.result[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0
                        if (count > 0) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.steps = count
                        }
                    }
                } catch (e: Exception) {
                    recordError("Steps", e)
                }
            } else {
                if ("Steps" !in skipped) skipped.add("Steps")
            }

            // 2. Distance
            if (hasRead(DistanceRecord::class)) {
                try {
                    val req = AggregateGroupByPeriodRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = timeRangeFilter,
                        timeRangeSlicer = Period.ofDays(1)
                    )
                    val response = client.aggregateGroupByPeriod(req)
                    for (item in response) {
                        val dateStr = item.startTime.atZone(zoneId).toLocalDate().toString()
                        val meters = item.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters?.toFloat() ?: 0f
                        if (meters > 0f) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.distanceMeters = meters
                        }
                    }
                } catch (e: Exception) {
                    recordError("Distance", e)
                }
            } else {
                if ("Distance" !in skipped) skipped.add("Distance")
            }

            // 3. Active Calories
            if (hasRead(ActiveCaloriesBurnedRecord::class)) {
                try {
                    val req = AggregateGroupByPeriodRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = timeRangeFilter,
                        timeRangeSlicer = Period.ofDays(1)
                    )
                    val response = client.aggregateGroupByPeriod(req)
                    for (item in response) {
                        val dateStr = item.startTime.atZone(zoneId).toLocalDate().toString()
                        val cals = item.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt() ?: 0
                        if (cals > 0) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.activeCalories = cals
                        }
                    }
                } catch (e: Exception) {
                    recordError("Active calories", e)
                }
            } else {
                if ("Active calories" !in skipped) skipped.add("Active calories")
            }

            // 4. Sleep Hours
            if (hasRead(SleepSessionRecord::class)) {
                try {
                    val req = AggregateGroupByPeriodRequest(
                        metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                        timeRangeFilter = timeRangeFilter,
                        timeRangeSlicer = Period.ofDays(1)
                    )
                    val response = client.aggregateGroupByPeriod(req)
                    for (item in response) {
                        val dateStr = item.startTime.atZone(zoneId).toLocalDate().toString()
                        val dur = item.result[SleepSessionRecord.SLEEP_DURATION_TOTAL]
                        if (dur != null && dur.toMinutes() > 0) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.sleepHours = dur.toMinutes() / 60f
                        }
                    }
                } catch (e: Exception) {
                    recordError("Sleep", e)
                }
            } else {
                if ("Sleep" !in skipped) skipped.add("Sleep")
            }

            // 5. Exercise Minutes
            if (hasRead(ExerciseSessionRecord::class)) {
                try {
                    val req = AggregateGroupByPeriodRequest(
                        metrics = setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
                        timeRangeFilter = timeRangeFilter,
                        timeRangeSlicer = Period.ofDays(1)
                    )
                    val response = client.aggregateGroupByPeriod(req)
                    for (item in response) {
                        val dateStr = item.startTime.atZone(zoneId).toLocalDate().toString()
                        val dur = item.result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]
                        if (dur != null && dur.toMinutes() > 0) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.exerciseMinutes = dur.toMinutes().toInt()
                        }
                    }
                } catch (e: Exception) {
                    recordError("Exercise", e)
                }
            } else {
                if ("Exercise" !in skipped) skipped.add("Exercise")
            }

            // 6. Heart Rate
            if (hasRead(HeartRateRecord::class)) {
                try {
                    val req = AggregateGroupByPeriodRequest(
                        metrics = setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MIN, HeartRateRecord.BPM_MAX),
                        timeRangeFilter = timeRangeFilter,
                        timeRangeSlicer = Period.ofDays(1)
                    )
                    val response = client.aggregateGroupByPeriod(req)
                    for (item in response) {
                        val dateStr = item.startTime.atZone(zoneId).toLocalDate().toString()
                        val avg = item.result[HeartRateRecord.BPM_AVG]?.toInt() ?: 0
                        val min = item.result[HeartRateRecord.BPM_MIN]?.toInt() ?: 0
                        val max = item.result[HeartRateRecord.BPM_MAX]?.toInt() ?: 0
                        if (avg > 0 || min > 0 || max > 0) {
                            val builder = daysMap.getOrPut(dateStr) { DayBuilder() }
                            builder.heartRateAvg = avg
                            builder.heartRateMin = min
                            builder.heartRateMax = max
                        }
                    }
                } catch (e: Exception) {
                    recordError("Heart rate", e)
                }
            } else {
                if ("Heart rate" !in skipped) skipped.add("Heart rate")
            }

            // 7. Resting Heart Rate
            if (hasRead(RestingHeartRateRecord::class)) {
                try {
                    val req = AggregateGroupByPeriodRequest(
                        metrics = setOf(RestingHeartRateRecord.BPM_AVG),
                        timeRangeFilter = timeRangeFilter,
                        timeRangeSlicer = Period.ofDays(1)
                    )
                    val response = client.aggregateGroupByPeriod(req)
                    for (item in response) {
                        val dateStr = item.startTime.atZone(zoneId).toLocalDate().toString()
                        val avg = item.result[RestingHeartRateRecord.BPM_AVG]?.toInt() ?: 0
                        if (avg > 0) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.restingHeartRate = avg
                        }
                    }
                } catch (e: Exception) {
                    recordError("Resting heart rate", e)
                }
            } else {
                if ("Resting heart rate" !in skipped) skipped.add("Resting heart rate")
            }

            // 8. Mindfulness Minutes
            if (hasRead(MindfulnessSessionRecord::class)) {
                try {
                    val req = AggregateGroupByPeriodRequest(
                        metrics = setOf(MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL),
                        timeRangeFilter = timeRangeFilter,
                        timeRangeSlicer = Period.ofDays(1)
                    )
                    val response = client.aggregateGroupByPeriod(req)
                    for (item in response) {
                        val dateStr = item.startTime.atZone(zoneId).toLocalDate().toString()
                        val dur = item.result[MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL]
                        if (dur != null && dur.toMinutes() > 0) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.mindfulnessMinutes = dur.toMinutes().toInt()
                        }
                    }
                } catch (e: Exception) {
                    try {
                        var pageToken: String? = null
                        do {
                            val response = client.readRecords(
                                ReadRecordsRequest(
                                    recordType = MindfulnessSessionRecord::class,
                                    timeRangeFilter = timeRangeFilter,
                                    pageSize = 1000,
                                    pageToken = pageToken
                                )
                            )
                            for (rec in response.records) {
                                val dateStr = rec.startTime.atZone(zoneId).toLocalDate().toString()
                                val mins = java.time.Duration.between(rec.startTime, rec.endTime).toMinutes().toInt()
                                if (mins > 0) {
                                    val b = daysMap.getOrPut(dateStr) { DayBuilder() }
                                    b.mindfulnessMinutes += mins
                                }
                            }
                            pageToken = response.pageToken
                        } while (pageToken != null)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }

            // 9. HRV
            if (hasRead(HeartRateVariabilityRmssdRecord::class)) {
                try {
                    var pageToken: String? = null
                    val latestMap = mutableMapOf<String, Pair<Instant, Float>>()
                    do {
                        val response = client.readRecords(
                            ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, timeRangeFilter, pageSize = 1000, pageToken = pageToken)
                        )
                        for (rec in response.records) {
                            val dateStr = rec.time.atZone(zoneId).toLocalDate().toString()
                            val existing = latestMap[dateStr]
                            if (existing == null || rec.time.isAfter(existing.first)) {
                                latestMap[dateStr] = Pair(rec.time, rec.heartRateVariabilityMillis.toFloat())
                            }
                        }
                        pageToken = response.pageToken
                    } while (pageToken != null)
                    for ((dateStr, pair) in latestMap) {
                        if (pair.second > 0f) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.hrv = pair.second
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            // 10. SpO2
            if (hasRead(OxygenSaturationRecord::class)) {
                try {
                    var pageToken: String? = null
                    val latestMap = mutableMapOf<String, Pair<Instant, Float>>()
                    do {
                        val response = client.readRecords(
                            ReadRecordsRequest(OxygenSaturationRecord::class, timeRangeFilter, pageSize = 1000, pageToken = pageToken)
                        )
                        for (rec in response.records) {
                            val dateStr = rec.time.atZone(zoneId).toLocalDate().toString()
                            val existing = latestMap[dateStr]
                            if (existing == null || rec.time.isAfter(existing.first)) {
                                latestMap[dateStr] = Pair(rec.time, rec.percentage.value.toFloat())
                            }
                        }
                        pageToken = response.pageToken
                    } while (pageToken != null)
                    for ((dateStr, pair) in latestMap) {
                        if (pair.second > 0f) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.spo2 = pair.second
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            // 11. Skin Temperature
            if (hasRead(SkinTemperatureRecord::class)) {
                try {
                    var pageToken: String? = null
                    val latestMap = mutableMapOf<String, Pair<Instant, Float>>()
                    do {
                        val response = client.readRecords(
                            ReadRecordsRequest(SkinTemperatureRecord::class, timeRangeFilter, pageSize = 1000, pageToken = pageToken)
                        )
                        for (rec in response.records) {
                            val dateStr = rec.startTime.atZone(zoneId).toLocalDate().toString()
                            val baseline = rec.baseline?.inCelsius?.toFloat() ?: 0f
                            if (baseline != 0f) {
                                val existing = latestMap[dateStr]
                                if (existing == null || rec.startTime.isAfter(existing.first)) {
                                    latestMap[dateStr] = Pair(rec.startTime, baseline)
                                }
                            }
                        }
                        pageToken = response.pageToken
                    } while (pageToken != null)
                    for ((dateStr, pair) in latestMap) {
                        if (pair.second != 0f) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.skinTemp = pair.second
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            // 12. Respiratory Rate
            if (hasRead(RespiratoryRateRecord::class)) {
                try {
                    var pageToken: String? = null
                    val latestMap = mutableMapOf<String, Pair<Instant, Float>>()
                    do {
                        val response = client.readRecords(
                            ReadRecordsRequest(RespiratoryRateRecord::class, timeRangeFilter, pageSize = 1000, pageToken = pageToken)
                        )
                        for (rec in response.records) {
                            val dateStr = rec.time.atZone(zoneId).toLocalDate().toString()
                            val existing = latestMap[dateStr]
                            if (existing == null || rec.time.isAfter(existing.first)) {
                                latestMap[dateStr] = Pair(rec.time, rec.rate.toFloat())
                            }
                        }
                        pageToken = response.pageToken
                    } while (pageToken != null)
                    for ((dateStr, pair) in latestMap) {
                        if (pair.second > 0f) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.respiratoryRate = pair.second
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            // 13. Blood Pressure
            if (hasRead(BloodPressureRecord::class)) {
                try {
                    var pageToken: String? = null
                    val latestMap = mutableMapOf<String, Pair<Instant, String>>()
                    do {
                        val response = client.readRecords(
                            ReadRecordsRequest(BloodPressureRecord::class, timeRangeFilter, pageSize = 1000, pageToken = pageToken)
                        )
                        for (rec in response.records) {
                            val dateStr = rec.time.atZone(zoneId).toLocalDate().toString()
                            val sys = rec.systolic.inMillimetersOfMercury.toInt()
                            val dia = rec.diastolic.inMillimetersOfMercury.toInt()
                            val bpStr = "$sys/$dia"
                            val existing = latestMap[dateStr]
                            if (existing == null || rec.time.isAfter(existing.first)) {
                                latestMap[dateStr] = Pair(rec.time, bpStr)
                            }
                        }
                        pageToken = response.pageToken
                    } while (pageToken != null)
                    for ((dateStr, pair) in latestMap) {
                        if (pair.second.isNotBlank()) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.bloodPressure = pair.second
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            // 14. Blood Glucose
            if (hasRead(BloodGlucoseRecord::class)) {
                try {
                    var pageToken: String? = null
                    val latestMap = mutableMapOf<String, Pair<Instant, Float>>()
                    do {
                        val response = client.readRecords(
                            ReadRecordsRequest(BloodGlucoseRecord::class, timeRangeFilter, pageSize = 1000, pageToken = pageToken)
                        )
                        for (rec in response.records) {
                            val dateStr = rec.time.atZone(zoneId).toLocalDate().toString()
                            val valG = rec.level.inMillimolesPerLiter.toFloat()
                            val existing = latestMap[dateStr]
                            if (existing == null || rec.time.isAfter(existing.first)) {
                                latestMap[dateStr] = Pair(rec.time, valG)
                            }
                        }
                        pageToken = response.pageToken
                    } while (pageToken != null)
                    for ((dateStr, pair) in latestMap) {
                        if (pair.second > 0f) {
                            daysMap.getOrPut(dateStr) { DayBuilder() }.bloodGlucose = pair.second
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            // 15. Resting Heart Rate Spot Fallback
            if (hasRead(RestingHeartRateRecord::class)) {
                try {
                    var pageToken: String? = null
                    val latestMap = mutableMapOf<String, Pair<Instant, Int>>()
                    do {
                        val response = client.readRecords(
                            ReadRecordsRequest(RestingHeartRateRecord::class, timeRangeFilter, pageSize = 1000, pageToken = pageToken)
                        )
                        for (rec in response.records) {
                            val dateStr = rec.time.atZone(zoneId).toLocalDate().toString()
                            val existing = latestMap[dateStr]
                            if (existing == null || rec.time.isAfter(existing.first)) {
                                latestMap[dateStr] = Pair(rec.time, rec.beatsPerMinute.toInt())
                            }
                        }
                        pageToken = response.pageToken
                    } while (pageToken != null)
                    for ((dateStr, pair) in latestMap) {
                        val b = daysMap.getOrPut(dateStr) { DayBuilder() }
                        if (b.restingHeartRate == 0 && pair.second > 0) {
                            b.restingHeartRate = pair.second
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            currentChunkStart = chunkEnd.plusDays(1)
        }

        return daysMap.mapValues { (_, builder) -> builder.build() }
    }
}
