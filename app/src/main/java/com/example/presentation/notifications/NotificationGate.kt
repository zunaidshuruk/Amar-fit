package com.example.presentation.notifications

import android.content.Context

/**
 * The master switch ("Allow KardIQ notifications") as a flag every notification passes through, so that when it is off NOTHING is shown:
 * reminders, goal alerts, medicine reminders (and their snoozed repeats and missed-dose alerts), the Health Connect reminder, nutrition alerts,
 * the test notification and pushes from friends. It is updated whenever the notification settings change (ReminderScheduler.rescheduleAll).
 * On by default, so a fresh install is not silent. NotificationGateGuardTest fails the build if a notification is posted without checking it.
 */
object NotificationGate {
    private const val PREFS = "NotificationGate"
    private const val KEY = "master_enabled"

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, enabled).apply()
    }

    fun isEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)
}
