import re

with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'r') as f:
    content = f.read()

imports = """
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
"""

if "import androidx.health.connect.client.HealthConnectClient" not in content:
    content = content.replace('import java.util.Date', 'import java.util.Date\n' + imports)

sync_func = """
    fun syncWithHealthConnect(context: Context) {
        viewModelScope.launch {
            try {
                if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return@launch
                val healthConnectClient = HealthConnectClient.getOrCreate(context)
                
                // Set time range for today
                val zdt = ZonedDateTime.now(ZoneId.systemDefault())
                val startOfDay = zdt.toLocalDate().atStartOfDay(zdt.zone).toInstant()
                val endOfDay = zdt.toLocalDate().plusDays(1).atStartOfDay(zdt.zone).toInstant()
                val timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                
                var totalSteps = 0
                val stepsResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(StepsRecord::class, timeRangeFilter)
                )
                for (record in stepsResponse.records) {
                    totalSteps += record.count.toInt()
                }

                var sleepHours = 0f
                val sleepResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter)
                )
                for (record in sleepResponse.records) {
                    val durationStr = java.time.Duration.between(record.startTime, record.endTime)
                    sleepHours += durationStr.toMinutes() / 60f
                }

                var bloodPressure = ""
                val bpResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(BloodPressureRecord::class, timeRangeFilter)
                )
                if (bpResponse.records.isNotEmpty()) {
                    val latest = bpResponse.records.last()
                    bloodPressure = "${latest.systolic.inMillimetersOfMercury.toInt()}/${latest.diastolic.inMillimetersOfMercury.toInt()}"
                }

                var bloodGlucose = 0f
                val bgResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(BloodGlucoseRecord::class, timeRangeFilter)
                )
                if (bgResponse.records.isNotEmpty()) {
                    val latest = bgResponse.records.last()
                    bloodGlucose = latest.level.inMilligramsPerDeciliter.toFloat()
                }

                val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
                val updated = current.copy(
                    steps = if (totalSteps > 0) totalSteps else current.steps,
                    sleepHours = if (sleepHours > 0f) sleepHours else current.sleepHours,
                    bloodPressure = if (bloodPressure.isNotEmpty()) bloodPressure else current.bloodPressure,
                    bloodGlucoseMorning = if (bloodGlucose > 0f) bloodGlucose else current.bloodGlucoseMorning
                )
                repository.saveMetrics(updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
"""

if "fun syncWithHealthConnect(" not in content:
    content = content.replace('fun addSteps(steps: Int) {', sync_func + '\n    fun addSteps(steps: Int) {')

with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'w') as f:
    f.write(content)
