package com.rebuild.mixtube.ads.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rebuild.mixtube.ads.MainActivity
import com.rebuild.mixtube.ads.ServiceLocator
import com.rebuild.mixtube.ads.Tracking

class MyFirebaseMessageService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken: ${token.take(12)}")
        Tracking.log(this, "push_token", mapOf("tokenPrefix" to token.take(12)))
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val from = message.from.orEmpty()
        val messageType = message.messageType.orEmpty()
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Mixtube"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""

        val remoteConfig = runCatching { ServiceLocator.remoteConfig }.getOrNull()
        val notificationEnabled = remoteConfig?.getString("show_notification_and", "1") != "0"
        if (!notificationEnabled) {
            Tracking.log(
                this,
                "push_suppressed",
                mapOf("from" to from, "messageType" to messageType, "reason" to "show_notification_and=0"),
            )
            return
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            for ((k, v) in message.data) {
                putExtra(k, v)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationHelper
            .remoteMessageNotificationBuilder(this, title, body)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "message received: from=$from messageType=$messageType title=$title bodyLength=${body.length} dataKeys=${message.data.keys}")
        Tracking.log(
            this,
            "push_received",
            mapOf(
                "from" to from,
                "messageType" to messageType,
                "title" to title,
                "dataKeys" to message.data.keys.joinToString(","),
            ),
        )
    }

    companion object {
        private const val TAG = "MyFcmService"
        private const val NOTIFICATION_ID = 2001
    }
}
