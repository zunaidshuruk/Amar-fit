package com.example.presentation.notifications

import android.content.Context
import com.example.data.health.HealthConnectManager
import com.example.data.notifications.Channels
import com.example.data.notifications.HealthConnectNudgePolicy
import com.example.data.notifications.NotificationPrefs
import com.example.data.notifications.QuietHours
import java.time.LocalDate

/**
 * Asks the person to connect Health Connect: once on the first login (when the app opens), then every 3 days from the daily check,
 * until it is connected. The rule itself is HealthConnectNudgePolicy; this class reads the phone's state and remembers the last nudge.
 */
object HealthConnectNudge {
    private const val PREFS = "HealthConnectNudge"
    private const val KEY_LAST = "last_date"
    private const val NOTIFICATION_ID = 7803

    /**
     * Shows the nudge when it is due and returns true if it did. [firstOnly] limits it to the very first nudge (used when the app opens);
     * nothing is remembered when the notification could not be shown (quiet hours, notifications not allowed yet), so it is simply tried again.
     */
    suspend fun maybeShow(context: Context, prefs: NotificationPrefs, today: LocalDate, minuteNow: Int, firstOnly: Boolean): Boolean {
        if (!prefs.masterEnabled || !prefs.healthConnectNudge) return false
        if (QuietHours.isQuiet(prefs, minuteNow)) return false
        if (!NotificationHelper.areNotificationsAllowed(context)) return false
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = sp.getString(KEY_LAST, null)?.let { try { LocalDate.parse(it) } catch (e: Exception) { null } }
        if (firstOnly && last != null) return false
        val available = HealthConnectManager.isAvailable(context)
        val connected = available && HealthConnectManager.hasAnyPermissions(context)
        if (!HealthConnectNudgePolicy.shouldNudge(prefs.healthConnectNudge, available, connected, last, today)) return false
        NotificationHelper.showNotification(
            context = context,
            title = HealthConnectNudgePolicy.TITLE,
            message = HealthConnectNudgePolicy.MESSAGE,
            notificationId = NOTIFICATION_ID,
            targetRoute = "settings",
            channelId = Channels.HEALTH_CHECKS
        )
        sp.edit().putString(KEY_LAST, today.toString()).apply()
        return true
    }
}
