package com.example.presentation.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.notifications.Channels

object NotificationHelper {

    private class ChannelDef(val id: String, val name: String, val description: String, val importance: Int)

    private val channelDefs = listOf(
        ChannelDef(Channels.MEALS, "Meals & nutrition", "Meal log reminders and nutrition alerts", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelDef(Channels.HYDRATION, "Hydration", "Water reminders", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelDef(Channels.ACTIVITY, "Activity & workouts", "Workout reminders", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelDef(Channels.SLEEP_MIND, "Sleep & mindfulness", "Bedtime and mindful moment reminders", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelDef(Channels.HEALTH_CHECKS, "Health checks", "Weekly check-in, blood glucose and weigh-in reminders", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelDef(Channels.GOALS, "Goals reached", "Step, water, calorie, sleep and weekly workout goals", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelDef(Channels.MEDICINE, "Medicine reminders", "Reminders to take your medicine", NotificationManager.IMPORTANCE_HIGH),
        ChannelDef(Channels.CYCLE, "Cycle tracker", "Period and fertile window reminders and the daily cycle log reminder", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelDef(Channels.SOCIAL, "Messages & Challenges", "Direct messages, friend requests and friends' fitness challenges", NotificationManager.IMPORTANCE_HIGH)
    )

    /** Creates one channel per category (safe to call again; Android keeps the user's own channel settings). */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channelDefs.forEach { def ->
                val channel = NotificationChannel(def.id, def.name, def.importance).apply {
                    description = def.description
                    if (def.id == Channels.SOCIAL) enableVibration(true)
                    if (def.id == Channels.CYCLE || def.id == Channels.MEDICINE) lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    /** False when the user (or Android 13+) has switched all KardIQ notifications off. */
    fun areNotificationsAllowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        targetRoute: String? = null,
        channelId: String = Channels.MEALS,
        /** On the lock screen only a generic text shows; the details appear after the phone is unlocked. */
        discreet: Boolean = false
    ) {
        if (!NotificationGate.isEnabled(context)) return
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            targetRoute?.let { putExtra("NAVIGATE_TO", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (discreet) {
            val generic = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("KardIQ")
                .setContentText("You have a reminder")
                .build()
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(generic)
        }

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    fun showMessageNotification(context: Context, senderName: String, messageText: String, pairId: String) {
        if (!NotificationGate.isEnabled(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "dm/$pairId/$senderName")
        }
        val notifId = (pairId.hashCode() and 0x7FFFFFFF) + 1000
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, Channels.SOCIAL)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notifId, builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    fun showChallengeNotification(context: Context, title: String, messageText: String, notificationId: Int = 3001) {
        if (!NotificationGate.isEnabled(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "friends")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, Channels.SOCIAL)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
