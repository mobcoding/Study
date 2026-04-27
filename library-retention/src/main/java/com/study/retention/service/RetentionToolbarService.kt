package com.study.retention.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.study.retention.internal.RetentionEngine

internal class RetentionToolbarService : Service() {

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
        if (!RetentionEngine.ensureInitialized(this)) {
            stopSelf()
            return
        }
        val notification = RetentionEngine.toolbarNotificationOrNull() ?: run {
            stopSelf()
            return
        }
        runCatching {
            startForeground(NOTIFICATION_ID, notification)
        }.onFailure {
            Log.w(TAG, "Unable to start retention toolbar foreground service", it)
            stopSelf()
        }
    }

    private companion object {
        private const val NOTIFICATION_ID = 105_031
        private const val TAG = "RetentionToolbarSvc"
    }
}
