package com.example.presentation.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.notifications.NotificationPrefsCodec
import com.example.data.notifications.ReminderCatalog
import com.example.data.notifications.ReminderDecider
import com.example.data.notifications.ReminderKind
import com.example.data.notifications.TodaySnapshot
import com.example.data.notifications.WeeklySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Receives every scheduled reminder alarm. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val specId = intent.getStringExtra(EXTRA_SPEC_ID) ?: return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderRunner.run(appContext, specId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SPEC_ID = "SPEC_ID"
    }
}

/** What happens when a reminder fires: re-arm the next one, then show it unless there is a reason to skip it. */
object ReminderRunner {
    suspend fun run(context: Context, specId: String) {
        val db = AppDatabase.getDatabase(context)
        val profile = db.userDao().getUserProfile().firstOrNull() ?: return
        val prefs = NotificationPrefsCodec.decode(profile.notificationPrefsJson)
        // switched off in the meantime: nothing to show and nothing to re-arm
        val spec = ReminderCatalog.build(prefs).firstOrNull { it.id == specId } ?: return

        // Always arm the next occurrence first, so a problem further down never ends the series.
        ReminderScheduler.armNext(context, spec)
        if (!prefs.masterEnabled) return

        val now = LocalDateTime.now()
        val minuteNow = now.hour * 60 + now.minute
        val today = LocalDate.now()
        val date = today.toString()

        val metric = db.metricsDao().getMetricsForDate(date).firstOrNull()
        val foodLogs = db.metricsDao().getFoodLogsForDate(date).firstOrNull() ?: emptyList()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val workoutEvents = db.activityEventDao().getEventsSince(startOfDay).firstOrNull()
            ?.count { it.type == "workout" } ?: 0

        val snapshot = TodaySnapshot(
            loggedMealTypes = foodLogs.map { it.mealType.lowercase() }.toSet(),
            waterLiters = metric?.waterLiters ?: 0f,
            waterGoalLiters = profile.dailyWaterLimitLiters,
            exerciseMinutes = metric?.exerciseMinutes ?: 0,
            workoutEventsToday = workoutEvents,
            mindfulnessMinutes = metric?.mindfulnessMinutes ?: 0,
            weightLoggedToday = (metric?.weightKg ?: 0f) > 0f,
            glucoseSlotLogged = listOf(
                (metric?.bloodGlucoseBeforeBreakfast ?: 0f) > 0f,
                (metric?.bloodGlucoseAfterBreakfast ?: 0f) > 0f,
                (metric?.bloodGlucoseBeforeLunch ?: 0f) > 0f,
                (metric?.bloodGlucoseAfterLunch ?: 0f) > 0f,
                (metric?.bloodGlucoseBeforeDinner ?: 0f) > 0f,
                (metric?.bloodGlucoseAfterDinner ?: 0f) > 0f
            )
        )
        if (!ReminderDecider.shouldShow(spec, snapshot, prefs, minuteNow)) return

        var title = spec.title
        var message = spec.message
        if (spec.kind == ReminderKind.WEEKLY_CHECKIN) {
            val start = today.minusDays(6).toString()
            val week = db.metricsDao().getMetricsHistory(start).firstOrNull() ?: emptyList()
            val summary = WeeklySummary.build(week)
            title = summary.title
            message = summary.body
        }
        NotificationHelper.showNotification(
            context = context,
            title = title,
            message = message,
            notificationId = spec.requestCode,
            targetRoute = spec.route,
            channelId = spec.channel
        )
    }
}
