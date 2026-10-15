package com.example.presentation.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.notifications.NotificationPrefs
import com.example.data.notifications.NotificationPrefsCodec
import com.example.data.notifications.ReminderCatalog
import com.example.data.notifications.ReminderPlanner
import com.example.data.notifications.ReminderSpec
import kotlinx.coroutines.flow.firstOrNull

/**
 * Arms every reminder that is switched on, one occurrence at a time. Each alarm re-arms the next one when it fires,
 * and everything is re-armed after a reboot, an app update, a time or time zone change and whenever the settings change.
 * `setAndAllowWhileIdle` needs no special alarm permission; the reminder arrives within a few minutes of its time.
 */
object ReminderScheduler {
    private const val ACTION = "com.example.REMINDER_ALARM"

    /** Alarm request codes the first version used (one repeating alarm per meal). */
    private val legacyMealCodes = listOf(100, 200, 300)

    fun rescheduleAll(context: Context, prefs: NotificationPrefs) {
        NotificationGate.setEnabled(context, prefs.masterEnabled)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelLegacyMealAlarms(context, alarmManager)
        ReminderCatalog.allRequestCodes().forEach { cancel(context, alarmManager, it) }
        if (!prefs.masterEnabled) {
            // everything is off: also drop the pending snoozed repeats and missed-dose checks, and what is still showing
            for (slot in 0 until com.example.data.notifications.MedicineReminders.MAX) {
                val notificationId = ReminderCatalog.medicineNotificationId(slot)
                MedicineNotifier.cancelSnooze(context, notificationId)
                MedicineNotifier.cancelMissedCheck(context, notificationId)
            }
            androidx.core.app.NotificationManagerCompat.from(context).cancelAll()
            return
        }
        val now = System.currentTimeMillis()
        ReminderCatalog.build(prefs).forEach { arm(context, alarmManager, it, now) }
    }

    /** Re-arms everything from the profile saved on this phone (used after a reboot, update or time change). */
    suspend fun rescheduleFromDatabase(context: Context) {
        val profile = AppDatabase.getDatabase(context).userDao().getUserProfile().firstOrNull() ?: return
        rescheduleAll(context, NotificationPrefsCodec.decode(profile.notificationPrefsJson))
    }

    /** Arms the next occurrence of one reminder (called when it has just fired). */
    fun armNext(context: Context, spec: ReminderSpec) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        arm(context, alarmManager, spec, System.currentTimeMillis())
    }

    private fun arm(context: Context, alarmManager: AlarmManager, spec: ReminderSpec, now: Long) {
        val trigger = ReminderPlanner.nextTrigger(spec.minuteOfDay, spec.days, now) ?: return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(context, spec.requestCode, spec.id))
    }

    private fun pendingIntent(context: Context, requestCode: Int, specId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION
            putExtra(ReminderReceiver.EXTRA_SPEC_ID, specId)
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun cancel(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION }
        val existing = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
    }

    private fun cancelLegacyMealAlarms(context: Context, alarmManager: AlarmManager) {
        legacyMealCodes.forEach { code ->
            val existing = PendingIntent.getBroadcast(
                context, code, Intent(context, MealReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (existing != null) {
                alarmManager.cancel(existing)
                existing.cancel()
            }
        }
    }
}
