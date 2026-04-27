package com.study.retention.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import com.study.retention.internal.RetentionEngine
import com.study.retention.internal.RetentionLog

internal class RetentionToolbarService : Service() {

    private var timerThread: HandlerThread? = null
    private var timerHandler: Handler? = null
    private val timerRunnable = object : Runnable {
        override fun run() {
            RetentionEngine.handleTimerTick()
            val intervalMs = RetentionEngine.timerIntervalMs()
            timerHandler?.postDelayed(this, intervalMs)
            Log.d(RetentionLog.TAG, "Next timer trigger scheduled after ${intervalMs}ms.")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(RetentionLog.TAG, "Retention toolbar service created.")
        startOrStop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(RetentionLog.TAG, "Retention toolbar service onStartCommand. startId=$startId flags=$flags")
        startOrStop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTimerLoop()
        super.onDestroy()
    }

    @SuppressLint("ForegroundServiceType") private fun startOrStop() {
        if (!RetentionEngine.ensureInitialized(this)) {
            stopTimerLoop()
            stopSelf()
            return
        }
        val notification = RetentionEngine.toolbarNotificationOrNull() ?: run {
            Log.d(RetentionLog.TAG, "No toolbar notification available, stopping service.")
            stopTimerLoop()
            stopSelf()
            return
        }
        runCatching {
            startForeground(NOTIFICATION_ID, notification)
            Log.d(RetentionLog.TAG, "Retention toolbar foreground service active.")
            ensureTimerLoop()
        }.onFailure {
            Log.w(RetentionLog.TAG, "Unable to start retention toolbar foreground service", it)
            stopTimerLoop()
            stopSelf()
        }
    }

    private fun ensureTimerLoop() {
        if (!RetentionEngine.isTimerEnabled()) {
            stopTimerLoop()
            return
        }
        if (timerThread == null) {
            val thread = HandlerThread("library-retention-fgs-timer").apply { start() }
            timerThread = thread
            timerHandler = Handler(thread.looper)
            Log.d(RetentionLog.TAG, "Foreground service timer thread started.")
        }
        val handler = timerHandler ?: return
        handler.removeCallbacks(timerRunnable)
        val initialDelayMs = RetentionEngine.timerInitialDelayMs()
        handler.postDelayed(timerRunnable, initialDelayMs)
        Log.d(
            RetentionLog.TAG,
            "Foreground service timer loop scheduled. firstDelayMs=$initialDelayMs intervalMs=${RetentionEngine.timerIntervalMs()}",
        )
    }

    private fun stopTimerLoop() {
        timerHandler?.removeCallbacks(timerRunnable)
        timerThread?.quitSafely()
        timerHandler = null
        timerThread = null
    }

    private companion object {
        private const val NOTIFICATION_ID = 105_031
    }
}
