package com.study.trigger.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.study.trigger.RetentionModule
import com.study.trigger.internal.RetentionNotificationFactory
internal class ToolbarForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        startOrStop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startOrStop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startOrStop() {
        val config = RetentionModule.currentConfigOrNull() ?: run {
            stopSelf()
            return
        }
        val locale = RetentionModule.stateStore().resolvePreferredLocale(application)
        val toolbarItems = config.contentProvider.getToolbarItems(locale).take(4)
        if (toolbarItems.isEmpty()) {
            stopSelf()
            return
        }
        val notification = RetentionNotificationFactory.showToolbarNotification(toolbarItems)
        try {
            startForeground(config.notificationConfig.toolbarNotificationId, notification)
        } catch (_: Exception) {
        }
    }
}
