package com.study.retention.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.study.retention.internal.RetentionEngine
import com.study.retention.internal.RetentionLog
import com.study.retention.internal.RetentionNotifier

internal class RetentionToolbarForegroundService : Service() {

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
        val config = RetentionEngine.currentConfigOrNull() ?: run {
            Log.d(RetentionLog.TAG, "Stop toolbar foreground service because retention config is unavailable.")
            stopSelf()
            return
        }
        val items = config.toolbar.items.take(MAX_TOOLBAR_ITEMS)
        if (!config.toolbar.enabled || items.isEmpty()) {
            Log.d(
                RetentionLog.TAG,
                "Stop toolbar foreground service because toolbar is disabled or empty. enabled=${config.toolbar.enabled} itemCount=${items.size}",
            )
            stopSelf()
            return
        }
        val notification = RetentionNotifier(applicationContext, config)
            .buildToolbarForegroundNotification(items)
        try {
            startForeground(config.notification.toolbarNotificationId, notification)
            Log.d(RetentionLog.TAG, "Toolbar foreground service started. itemCount=${items.size}")
        } catch (throwable: Throwable) {
            Log.w(RetentionLog.TAG, "Failed to enter foreground for toolbar service.", throwable)
//            stopSelf()
        }
    }

    private companion object {
        private const val MAX_TOOLBAR_ITEMS = 4
    }
}
