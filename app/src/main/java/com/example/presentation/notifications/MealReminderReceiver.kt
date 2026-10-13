package com.example.presentation.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the repeating meal alarms of the first version. After an update such an alarm may still fire once:
 * it moves the schedule to the new reminders (which cancels the old alarms) and shows that meal's reminder.
 */
class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val meal = intent.getStringExtra("MEAL_TYPE")?.lowercase() ?: return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.rescheduleFromDatabase(appContext)
                ReminderRunner.run(appContext, "meal_$meal")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pending.finish()
            }
        }
    }
}
