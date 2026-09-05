package com.example.presentation.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ReminderManager {
    fun scheduleMealReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Schedule Breakfast at 8:00 AM
        scheduleAlarmForMeal(context, alarmManager, "Breakfast", 8, 0, 100)
        
        // Schedule Lunch at 1:30 PM (13:30)
        scheduleAlarmForMeal(context, alarmManager, "Lunch", 13, 30, 200)
        
        // Schedule Dinner at 8:00 PM (20:00)
        scheduleAlarmForMeal(context, alarmManager, "Dinner", 20, 0, 300)
    }

    private fun scheduleAlarmForMeal(
        context: Context,
        alarmManager: AlarmManager,
        mealType: String,
        hourOfDay: Int,
        minute: Int,
        requestCode: Int
    ) {
        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            putExtra("MEAL_TYPE", mealType)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Use setInexactRepeating as it doesn't require exact alarm permission which is restricted in newer Android versions
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
