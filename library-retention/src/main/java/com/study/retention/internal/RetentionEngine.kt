package com.study.retention.internal

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

internal object RetentionEngine {

    private const val TIMER_INITIAL_DELAY_MS = 20_000L
    private const val TIMER_INTERVAL_MS = 60_000L
    private const val UNLOCK_DELAY_MS = 1_000L

    @Volatile
    private var config: RetentionRuntimeConfig? = null
    private lateinit var application: Application
    private lateinit var store: RetentionStore
    private var scheduler: RetentionScheduler? = null
    private var initialized = false
    private var lifecycleRegistered = false
    private var unlockReceiverRegistered = false
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            Log.d(RetentionLog.TAG, "Timer trigger fired.")
            scheduler?.handleTrigger(RetentionTriggerType.TIMER)
            backgroundHandler?.postDelayed(this, TIMER_INTERVAL_MS)
        }
    }

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(RetentionLog.TAG, "Received unlock broadcast: ${intent?.action}")
            backgroundHandler?.postDelayed(
                {
                    Log.d(RetentionLog.TAG, "Dispatch unlock trigger after delay.")
                    scheduler?.handleTrigger(RetentionTriggerType.UNLOCK)
                },
                UNLOCK_DELAY_MS,
            )
        }
    }

    @Synchronized
    fun initialize(context: Context): Boolean {
        if (initialized) {
            Log.d(RetentionLog.TAG, "Retention engine already initialized. enabled=${config != null}")
            return config != null
        }
        val app = context.applicationContext as? Application ?: return false
        application = app
        store = RetentionStore(app)
        Log.d(RetentionLog.TAG, "Initializing retention engine.")
        val loadedConfig = RetentionConfigLoader.load(app)
        if (loadedConfig == null) {
            initialized = true
            Log.e(RetentionLog.TAG, "Retention config unavailable, SDK disabled.")
            return false
        }
        config = loadedConfig
        scheduler = RetentionScheduler(app, loadedConfig, store)
        ensureHandler()
        registerLifecycleCallbacks()
        registerUnlockReceiver()
        Log.d(
            RetentionLog.TAG,
            "Retention config loaded. toolbarItems=${loadedConfig.toolbar.items.size}, reminderItems=${loadedConfig.reminders.items.size}, bucketOrder=${loadedConfig.reminders.bucketOrder}",
        )
        scheduler?.refreshToolbar(app)
        scheduler?.scheduleNextAlarm(force = true)
        restartRuntimeLoops()
        initialized = true
        Log.d(RetentionLog.TAG, "Retention engine initialized successfully.")
        return true
    }

    fun ensureInitialized(context: Context): Boolean {
        return if (initialized) {
            config != null
        } else {
            initialize(context)
        }
    }

    fun handleAlarm(context: Context) {
        if (!ensureInitialized(context)) {
            Log.w(RetentionLog.TAG, "Skip alarm handling because retention engine is disabled.")
            return
        }
        Log.d(RetentionLog.TAG, "Handling alarm trigger.")
        scheduler?.handleTrigger(RetentionTriggerType.ALARM)
        scheduler?.scheduleNextAlarm(force = true)
    }

    fun handleBoot(context: Context) {
        if (!ensureInitialized(context)) {
            Log.w(RetentionLog.TAG, "Skip boot handling because retention engine is disabled.")
            return
        }
        Log.d(RetentionLog.TAG, "Handling boot trigger.")
        scheduler?.refreshToolbar(application)
        scheduler?.handleTrigger(RetentionTriggerType.BOOT)
        scheduler?.scheduleNextAlarm(force = true)
        restartRuntimeLoops()
    }

    fun onNotificationPermissionGranted(context: Context) {
        if (!ensureInitialized(context)) {
            Log.w(RetentionLog.TAG, "Skip permission recovery because retention engine is disabled.")
            return
        }
        Log.d(RetentionLog.TAG, "Notification permission granted. Recovering retention runtime.")
        scheduler?.refreshToolbar(application)
        scheduler?.scheduleNextAlarm(force = true)
        restartRuntimeLoops()
    }

    fun toolbarNotificationOrNull() = scheduler?.buildToolbarNotification()

    private fun restartRuntimeLoops() {
        val handler = backgroundHandler ?: return
        handler.removeCallbacks(timerRunnable)
        handler.postDelayed(timerRunnable, TIMER_INITIAL_DELAY_MS)
        Log.d(RetentionLog.TAG, "Runtime loop scheduled. firstDelayMs=$TIMER_INITIAL_DELAY_MS intervalMs=$TIMER_INTERVAL_MS")
    }

    private fun ensureHandler() {
        if (handlerThread != null) {
            return
        }
        val thread = HandlerThread("library-retention-runtime").apply { start() }
        handlerThread = thread
        backgroundHandler = Handler(thread.looper)
        Log.d(RetentionLog.TAG, "Background handler thread started.")
    }

    private fun registerLifecycleCallbacks() {
        if (lifecycleRegistered) {
            return
        }
        application.registerActivityLifecycleCallbacks(RetentionAppVisibilityTracker)
        lifecycleRegistered = true
        Log.d(RetentionLog.TAG, "Activity lifecycle callbacks registered.")
    }

    private fun registerUnlockReceiver() {
        if (unlockReceiverRegistered) {
            return
        }
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(unlockReceiver, filter)
        }
        unlockReceiverRegistered = true
        Log.d(RetentionLog.TAG, "Unlock receiver registered.")
    }
}
