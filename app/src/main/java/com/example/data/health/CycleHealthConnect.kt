package com.example.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.data.cycle.FlowLevel
import com.example.data.cycle.PeriodSpan
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Cycle data in Health Connect (period and flow records).
 * These permissions are deliberately NOT part of HealthConnectManager.REQUIRED_PERMISSIONS: only people who switch on
 * "Share with Health Connect" in Cycle settings are ever asked for them.
 */
object CycleHealthConnect {
    val PERMISSIONS: Set<String> = setOf(
        HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
        HealthPermission.getWritePermission(MenstruationPeriodRecord::class),
        HealthPermission.getReadPermission(MenstruationFlowRecord::class),
        HealthPermission.getWritePermission(MenstruationFlowRecord::class)
    )

    suspend fun hasPermissions(context: Context): Boolean = try {
        HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
    } catch (e: Exception) {
        false
    }

    /** Periods recorded in Health Connect over the last [sinceDays] days (by this or any other app). */
    suspend fun readPeriods(context: Context, sinceDays: Long = 400): List<PeriodSpan> {
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val now = Instant.now()
            val response = client.readRecords(
                ReadRecordsRequest(
                    MenstruationPeriodRecord::class,
                    TimeRangeFilter.between(now.minus(sinceDays, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS))
                )
            )
            response.records.mapNotNull { record ->
                val zone = ZoneId.systemDefault()
                val startOffset = record.startZoneOffset ?: zone.rules.getOffset(record.startTime)
                val endOffset = record.endZoneOffset ?: zone.rules.getOffset(record.endTime)
                val start = record.startTime.atOffset(startOffset).toLocalDate()
                // the end of a record is exclusive, so the last day is the day just before it
                val end = record.endTime.minusMillis(1).atOffset(endOffset).toLocalDate()
                if (end.isBefore(start)) PeriodSpan(start, null) else PeriodSpan(start, end)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /** Saves a finished period (first day through last day, both included). */
    suspend fun writePeriod(context: Context, start: LocalDate, end: LocalDate): Boolean {
        return try {
            val zone = ZoneId.systemDefault()
            val startInstant = start.atStartOfDay(zone).toInstant()
            val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
            val record = MenstruationPeriodRecord(
                startTime = startInstant,
                startZoneOffset = zone.rules.getOffset(startInstant),
                endTime = endInstant,
                endZoneOffset = zone.rules.getOffset(endInstant)
            )
            HealthConnectClient.getOrCreate(context).insertRecords(listOf(record))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Saves the flow of one day. Health Connect knows light, medium and heavy only, so spotting and none are not written. */
    suspend fun writeFlow(context: Context, date: LocalDate, flowCode: Int): Boolean {
        val hcFlow = when (FlowLevel.fromCode(flowCode)) {
            FlowLevel.LIGHT -> MenstruationFlowRecord.FLOW_LIGHT
            FlowLevel.MEDIUM -> MenstruationFlowRecord.FLOW_MEDIUM
            FlowLevel.HEAVY -> MenstruationFlowRecord.FLOW_HEAVY
            else -> return false
        }
        return try {
            val zone = ZoneId.systemDefault()
            val time = date.atTime(12, 0).atZone(zone).toInstant()
            val record = MenstruationFlowRecord(time = time, zoneOffset = zone.rules.getOffset(time), flow = hcFlow)
            HealthConnectClient.getOrCreate(context).insertRecords(listOf(record))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
