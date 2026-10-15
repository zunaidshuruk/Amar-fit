package com.example.presentation.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.notifications.Channels
import com.example.data.notifications.MedicineMissed
import com.example.data.notifications.NotificationPrefsCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import com.example.data.notifications.MedicineTaken
import java.time.LocalDate

/** The medicine reminder notification with its two buttons, Taken and Snooze (10 minutes), and the snoozed repeat. */
object MedicineNotifier {
    const val ACTION_TAKEN = "com.example.MEDICINE_TAKEN"
    const val ACTION_SNOOZE = "com.example.MEDICINE_SNOOZE"
    const val ACTION_SNOOZE_FIRE = "com.example.MEDICINE_SNOOZE_FIRE"
    const val ACTION_SKIP = "com.example.MEDICINE_SKIP"
    const val ACTION_MISSED_FIRE = "com.example.MEDICINE_MISSED_FIRE"
    const val EXTRA_ID = "MED_ID"
    const val EXTRA_NOTIFICATION_ID = "MED_NOTIFICATION_ID"
    const val EXTRA_TITLE = "MED_TITLE"
    const val EXTRA_MESSAGE = "MED_MESSAGE"
    const val EXTRA_DISCREET = "MED_DISCREET"

    // request codes: the button PendingIntents and the snooze alarm get their own range per notification id
    private fun takenCode(notificationId: Int) = notificationId + 200
    private fun snoozeCode(notificationId: Int) = notificationId + 400
    private fun fireCode(notificationId: Int) = notificationId + 600
    private fun skipCode(notificationId: Int) = notificationId + 1500
    private fun missedCode(notificationId: Int) = notificationId + 1100

    /** The "Missed dose?" notification of a reminder has its own id, so it never replaces the reminder itself. */
    fun missedNotificationId(notificationId: Int) = notificationId + 800

    private fun actionIntent(
        context: Context, action: String, code: Int, medicineId: String, notificationId: Int,
        title: String, message: String, discreet: Boolean
    ): PendingIntent {
        val intent = Intent(context, MedicineActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ID, medicineId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_DISCREET, discreet)
        }
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun show(
        context: Context, medicineId: String, notificationId: Int, title: String, message: String, discreet: Boolean,
        /** Minutes until the "Missed dose?" alert is checked; 0 = no alert (used for the snoozed repeat and when the alert is off). */
        missedAfterMinutes: Int = 0
    ) {
        if (!NotificationGate.isEnabled(context)) return
        NotificationHelper.createNotificationChannel(context)
        val open = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAVIGATE_TO", "today")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val taken = actionIntent(context, ACTION_TAKEN, takenCode(notificationId), medicineId, notificationId, title, message, discreet)
        val snooze = actionIntent(context, ACTION_SNOOZE, snoozeCode(notificationId), medicineId, notificationId, title, message, discreet)

        val builder = NotificationCompat.Builder(context, Channels.MEDICINE)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Taken", taken)
            .addAction(0, "Snooze ${MedicineTaken.SNOOZE_MINUTES} min", snooze)

        if (discreet) {
            // on the lock screen only a generic text shows (and no buttons) until the phone is unlocked
            val generic = NotificationCompat.Builder(context, Channels.MEDICINE)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("KardIQ")
                .setContentText("You have a reminder")
                .build()
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(generic)
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        if (missedAfterMinutes > 0) scheduleMissedCheck(context, medicineId, notificationId, title, message, discreet, missedAfterMinutes)
    }

    /** Checks again after [minutes] whether the dose was marked taken or skipped; if not, the "Missed dose?" alert is shown. */
    fun scheduleMissedCheck(context: Context, medicineId: String, notificationId: Int, title: String, message: String, discreet: Boolean, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fire = actionIntent(context, ACTION_MISSED_FIRE, missedCode(notificationId), medicineId, notificationId, title, message, discreet)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, fire)
    }

    fun cancelMissedCheck(context: Context, notificationId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val existing = PendingIntent.getBroadcast(
            context, missedCode(notificationId),
            Intent(context, MedicineActionReceiver::class.java).apply { action = ACTION_MISSED_FIRE },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
    }

    fun showMissed(context: Context, medicineId: String, notificationId: Int, message: String, discreet: Boolean) {
        if (!NotificationGate.isEnabled(context)) return
        NotificationHelper.createNotificationChannel(context)
        val open = PendingIntent.getActivity(
            context, missedNotificationId(notificationId),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAVIGATE_TO", "medicine_history")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = com.example.data.notifications.MedicineMissed.messageFor(message)
        val taken = actionIntent(context, ACTION_TAKEN, takenCode(notificationId), medicineId, notificationId, "", message, discreet)
        val skip = actionIntent(context, ACTION_SKIP, skipCode(notificationId), medicineId, notificationId, "", message, discreet)
        val builder = NotificationCompat.Builder(context, Channels.MEDICINE)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(com.example.data.notifications.MedicineMissed.TITLE)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Taken", taken)
            .addAction(0, "Skip today", skip)
        if (discreet) {
            val generic = NotificationCompat.Builder(context, Channels.MEDICINE)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("KardIQ")
                .setContentText("You have a reminder")
                .build()
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(generic)
        }
        try {
            NotificationManagerCompat.from(context).notify(missedNotificationId(notificationId), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /** Hides the notification and shows it again in [MedicineTaken.SNOOZE_MINUTES] minutes unless the dose was marked taken meanwhile. */
    fun scheduleSnooze(context: Context, medicineId: String, notificationId: Int, title: String, message: String, discreet: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fire = actionIntent(context, ACTION_SNOOZE_FIRE, fireCode(notificationId), medicineId, notificationId, title, message, discreet)
        val at = System.currentTimeMillis() + MedicineTaken.SNOOZE_MINUTES * 60_000L
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, fire)
    }

    /** Marks the dose taken today and clears its notification, snoozed repeat and missed-dose check (used by the Today card). */
    fun markTaken(context: Context, store: MedicineTakenStore, medicineId: String, notificationId: Int) {
        store.markTaken(medicineId, LocalDate.now())
        if (notificationId >= 0) {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(notificationId)
            manager.cancel(missedNotificationId(notificationId))
            cancelSnooze(context, notificationId)
            cancelMissedCheck(context, notificationId)
        }
    }

    fun cancelSnooze(context: Context, notificationId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val existing = PendingIntent.getBroadcast(
            context, fireCode(notificationId),
            Intent(context, MedicineActionReceiver::class.java).apply { action = ACTION_SNOOZE_FIRE },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
    }
}

/** Handles the Taken, Snooze and Skip buttons, the snoozed repeat and the "Missed dose?" check. */
class MedicineActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(MedicineNotifier.EXTRA_ID) ?: return
        val notificationId = intent.getIntExtra(MedicineNotifier.EXTRA_NOTIFICATION_ID, -1)
        val title = intent.getStringExtra(MedicineNotifier.EXTRA_TITLE) ?: "Time for your medicine"
        val message = intent.getStringExtra(MedicineNotifier.EXTRA_MESSAGE) ?: ""
        val discreet = intent.getBooleanExtra(MedicineNotifier.EXTRA_DISCREET, true)
        val store = MedicineTakenStore(context)
        val manager = NotificationManagerCompat.from(context)

        fun clearAll() {
            manager.cancel(notificationId)
            manager.cancel(MedicineNotifier.missedNotificationId(notificationId))
            MedicineNotifier.cancelSnooze(context, notificationId)
            MedicineNotifier.cancelMissedCheck(context, notificationId)
        }

        when (intent.action) {
            MedicineNotifier.ACTION_TAKEN -> {
                store.markTaken(id, LocalDate.now())
                clearAll()
            }
            MedicineNotifier.ACTION_SKIP -> {
                store.markSkipped(id, LocalDate.now())
                clearAll()
            }
            MedicineNotifier.ACTION_SNOOZE -> {
                manager.cancel(notificationId)
                MedicineNotifier.scheduleSnooze(context, id, notificationId, title, message, discreet)
            }
            MedicineNotifier.ACTION_SNOOZE_FIRE -> {
                if (!store.isTaken(id, LocalDate.now()) && !store.isSkipped(id, LocalDate.now())) {
                    MedicineNotifier.show(context, id, notificationId, title, message, discreet)
                }
            }
            MedicineNotifier.ACTION_MISSED_FIRE -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val profile = AppDatabase.getDatabase(context).userDao().getUserProfile().firstOrNull()
                        val prefs = NotificationPrefsCodec.decode(profile?.notificationPrefsJson)
                        val today = LocalDate.now()
                        if (MedicineMissed.needed(prefs, store.isTaken(id, today), store.isSkipped(id, today))) {
                            manager.cancel(notificationId) // the reminder itself is out of date now
                            MedicineNotifier.showMissed(context, id, notificationId, message, discreet)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
