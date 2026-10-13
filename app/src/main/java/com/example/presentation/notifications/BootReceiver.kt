package com.example.presentation.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-arms every reminder after a reboot, an app update, or a change of the clock or time zone. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val appContext = context.applicationContext
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ReminderScheduler.rescheduleFromDatabase(appContext)
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
