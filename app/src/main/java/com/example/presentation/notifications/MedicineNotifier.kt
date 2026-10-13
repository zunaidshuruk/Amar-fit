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
import com.example.data.notifications.Channels
import com.example.data.notifications.MedicineTaken
import java.time.LocalDate

/** The medicine reminder notification with its two buttons, Taken and Snooze (10 minutes), and the snoozed repeat. */
object MedicineNotifier {
    const val ACTION_TAKEN = "com.example.MEDICINE_TAKEN"
    const val ACTION_SNOOZE = "com.example.MEDICINE_SNOOZE"
    const val ACTION_SNOOZE_FIRE = "com.example.MEDICINE_SNOOZE_FIRE"
    const val EXTRA_ID = "MED_ID"
    const val EXTRA_NOTIFICATION_ID = "MED_NOTIFICATION_ID"
    const val EXTRA_TITLE = "MED_TITLE"
    const val EXTRA_MESSAGE = "MED_MESSAGE"
    const val EXTRA_DISCREET = "MED_DISCREET"

    // request codes: the button PendingIntents and the snooze alarm get their own range per notification id
    private fun takenCode(notificationId: Int) = notificationId + 200
    private fun snoozeCode(notificationId: Int) = notificationId + 400
    private fun fireCode(notificationId: Int) = notificationId + 600

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

    fun show(context: Context, medicineId: String, notificationId: Int, title: String, message: String, discreet: Boolean) {
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
    }

    /** Hides the notification and shows it again in [MedicineTaken.SNOOZE_MINUTES] minutes unless the dose was marked taken meanwhile. */
    fun scheduleSnooze(context: Context, medicineId: String, notificationId: Int, title: String, message: String, discreet: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fire = actionIntent(context, ACTION_SNOOZE_FIRE, fireCode(notificationId), medicineId, notificationId, title, message, discreet)
        val at = System.currentTimeMillis() + MedicineTaken.SNOOZE_MINUTES * 60_000L
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, fire)
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

/** Handles the Taken and Snooze buttons and the snoozed repeat. */
class MedicineActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(MedicineNotifier.EXTRA_ID) ?: return
        val notificationId = intent.getIntExtra(MedicineNotifier.EXTRA_NOTIFICATION_ID, -1)
        val title = intent.getStringExtra(MedicineNotifier.EXTRA_TITLE) ?: "Time for your medicine"
        val message = intent.getStringExtra(MedicineNotifier.EXTRA_MESSAGE) ?: ""
        val discreet = intent.getBooleanExtra(MedicineNotifier.EXTRA_DISCREET, true)
        val store = MedicineTakenStore(context)
        val manager = NotificationManagerCompat.from(context)

        when (intent.action) {
            MedicineNotifier.ACTION_TAKEN -> {
                store.markTaken(id, LocalDate.now())
                manager.cancel(notificationId)
                MedicineNotifier.cancelSnooze(context, notificationId)
            }
            MedicineNotifier.ACTION_SNOOZE -> {
                manager.cancel(notificationId)
                MedicineNotifier.scheduleSnooze(context, id, notificationId, title, message, discreet)
            }
            MedicineNotifier.ACTION_SNOOZE_FIRE -> {
                if (!store.isTaken(id, LocalDate.now())) {
                    MedicineNotifier.show(context, id, notificationId, title, message, discreet)
                }
            }
        }
    }
}
