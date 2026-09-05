package com.example.presentation.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mealType = intent.getStringExtra("MEAL_TYPE") ?: "Meal"
        
        NotificationHelper.showNotification(
            context = context,
            title = "Time for $mealType!",
            message = "Don't forget to log your $mealType using the AI scanner.",
            notificationId = mealType.hashCode()
        )
    }
}
