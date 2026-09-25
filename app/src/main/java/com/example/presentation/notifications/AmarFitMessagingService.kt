package com.example.presentation.notifications

import android.util.Log
import com.example.data.repository.FirebaseManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AmarFitMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        FirebaseManager.updateFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        NotificationHelper.createNotificationChannel(applicationContext)

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val type = data["type"] ?: data["notification_type"] ?: ""

        when {
            type == "direct_message" || type == "dm" || type == "message" || data.containsKey("pairId") -> {
                val pairId = data["pairId"] ?: ""
                val senderName = data["senderName"] ?: notification?.title ?: "Friend"
                val messageText = data["text"] ?: data["message"] ?: notification?.body ?: "Sent you a message"
                NotificationHelper.showMessageNotification(
                    context = applicationContext,
                    senderName = senderName,
                    messageText = messageText,
                    pairId = pairId
                )
            }
            type == "challenge" || type == "new_challenge" || type == "health_challenge" || data.containsKey("challengeId") -> {
                val title = data["title"] ?: notification?.title ?: "New Health Challenge ⚔️"
                val messageText = data["message"] ?: data["body"] ?: notification?.body ?: "A new health challenge has been assigned!"
                NotificationHelper.showChallengeNotification(
                    context = applicationContext,
                    title = title,
                    messageText = messageText
                )
            }
            else -> {
                val title = notification?.title ?: data["title"] ?: "Amar Fit"
                val messageText = notification?.body ?: data["message"] ?: data["body"] ?: "New update available"
                val targetRoute = data["targetRoute"] ?: data["route"]
                NotificationHelper.showNotification(
                    context = applicationContext,
                    title = title,
                    message = messageText,
                    notificationId = System.currentTimeMillis().toInt(),
                    targetRoute = targetRoute
                )
            }
        }
    }

    companion object {
        private const val TAG = "AmarFitFCM"
    }
}
