import re

with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'r') as f:
    content = f.read()

# Add missing imports for aggregate and heart rate
imports_to_add = """
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.records.HeartRateRecord
"""
if "import androidx.health.connect.client.request.AggregateRequest" not in content:
    content = content.replace("import androidx.health.connect.client.request.ReadRecordsRequest", "import androidx.health.connect.client.request.ReadRecordsRequest\n" + imports_to_add)

new_sync = """    fun syncWithHealthConnect(context: Context) {
        viewModelScope.launch {
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
                        bloodGlucose = latest.level.inMilligramsPerDeciliter.toFloat()
                    }
                }

                // 5. Latest Heart Rate
                var heartRate = 0
                val hrResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter)
                )
                if (hrResponse.records.isNotEmpty()) {
                    val latestRecord = hrResponse.records.maxByOrNull { it.startTime }
                    if (latestRecord != null && latestRecord.samples.isNotEmpty()) {
                        heartRate = latestRecord.samples.last().beatsPerMinute.toInt()
                    }
                }

                val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
                val updated = current.copy(
                    steps = if (totalSteps > 0) totalSteps else current.steps,
                    sleepHours = if (sleepHours > 0f) sleepHours else current.sleepHours,
                    bloodPressure = if (bloodPressure.isNotEmpty()) bloodPressure else current.bloodPressure,
                    bloodGlucoseMorning = if (bloodGlucose > 0f) bloodGlucose else current.bloodGlucoseMorning,
                    heartRate = if (heartRate > 0) heartRate else current.heartRate
                )
                repository.saveMetrics(updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }"""

old_sync_pattern = r"fun syncWithHealthConnect\(context: Context\) \{.*?(?=fun addSteps)"
content = re.sub(old_sync_pattern, new_sync + "\n\n    ", content, flags=re.DOTALL)

with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'w') as f:
    f.write(content)

