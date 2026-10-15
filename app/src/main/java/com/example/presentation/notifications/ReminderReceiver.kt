package com.example.presentation.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.cycle.CycleAccess
import com.example.data.cycle.CycleNotices
import com.example.data.cycle.FastingLedgerBuilder
import com.example.data.cycle.FastingNotices
import com.example.data.cycle.toEntry
import com.example.data.cycle.CyclePredictor
import com.example.data.cycle.toSpan
import com.example.data.local.AppDatabase
import com.example.data.notifications.MedicineReminders
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

        // Cycle reminders are shown only while tracking is on for a Female profile (settings can be stale after a gender change).
        val isCycleKind = spec.kind == ReminderKind.CYCLE || spec.kind == ReminderKind.CYCLE_LOG || spec.kind == ReminderKind.FASTING
        if (isCycleKind && !CycleAccess.isActive(profile.gender, profile.cycleTrackingEnabled, profile.cycleConsentAt)) return
        if (spec.kind == ReminderKind.FASTING && !profile.ramadanTrackingEnabled) return

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
            ),
            medicineTakenToday = if (spec.kind == ReminderKind.MEDICINE) MedicineTakenStore(context).handledOn(today) else emptySet(),
            cycleLoggedToday = if (isCycleKind) {
                db.cycleDao().getDay(date)?.let { it.flow > 0 || it.symptoms != 0 || it.mood != 0 } == true
            } else false
        )
        if (!ReminderDecider.shouldShow(spec, snapshot, prefs, minuteNow)) return

        // medicine reminders carry the Taken and Snooze buttons
        if (spec.kind == ReminderKind.MEDICINE) {
            MedicineNotifier.show(
                context, spec.detail, spec.requestCode, spec.title, spec.message, prefs.medicineDiscreet,
                missedAfterMinutes = if (prefs.medicineMissedAlert) MedicineReminders.missedAfter(prefs) else 0
            )
            return
        }

        // the Health Connect nudge decides for itself (connected? available? 3 days since the last one?) and shows itself
        if (spec.kind == ReminderKind.HEALTH_CONNECT) {
            HealthConnectNudge.maybeShow(context, prefs, today, minuteNow, firstOnly = false)
            return
        }

        var title = spec.title
        var message = spec.message
        if (spec.kind == ReminderKind.WEEKLY_CHECKIN) {
            val start = today.minusDays(6).toString()
            val week = db.metricsDao().getMetricsHistory(start).firstOrNull() ?: emptyList()
            val summary = WeeklySummary.build(week)
            title = summary.title
            message = summary.body
        }
        if (spec.kind == ReminderKind.CYCLE) {
            val periods = db.cycleDao().getAllPeriodsIncludingRemoved().filter { !it.removed }.mapNotNull { it.toSpan() }
            val prediction = CyclePredictor.predict(periods, today, profile.cycleDefaultLength, profile.cyclePeriodDefaultLength)
            val notice = CycleNotices.forToday(prediction, prefs, today) ?: return
            title = notice.title
            message = notice.message
        }
        if (spec.kind == ReminderKind.FASTING) {
            val entries = db.cycleDao().getAllFasting().mapNotNull { it.toEntry() }
            val periods = db.cycleDao().getAllPeriodsIncludingRemoved().filter { !it.removed }.mapNotNull { it.toSpan() }
            val ledger = FastingLedgerBuilder.build(entries, periods, today, profile.ramadanOffsetDays)
            val notice = FastingNotices.makeUp(ledger) ?: return
            title = notice.title
            message = notice.message
        }
        NotificationHelper.showNotification(
            context = context,
            title = title,
            message = message,
            notificationId = spec.requestCode,
            targetRoute = spec.route,
            channelId = spec.channel,
            discreet = (isCycleKind && profile.cycleDiscreetNotifications) || (spec.kind == ReminderKind.MEDICINE && prefs.medicineDiscreet)
        )
    }
}
