package com.study.retention.internal

import android.app.Application
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
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
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

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
        Log.d(
            RetentionLog.TAG,
            "Retention config loaded. toolbarItems=${loadedConfig.toolbar.items.size}, reminderItems=${loadedConfig.reminders.items.size}, bucketOrder=${loadedConfig.reminders.bucketOrder}",
        )
        scheduler?.refreshToolbar(app)
        scheduler?.scheduleNextAlarm(force = true)
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
    }

    fun onNotificationPermissionGranted(context: Context) {
        if (!ensureInitialized(context)) {
            Log.w(RetentionLog.TAG, "Skip permission recovery because retention engine is disabled.")
            return
        }
        Log.d(RetentionLog.TAG, "Notification permission granted. Recovering retention runtime.")
        scheduler?.refreshToolbar(application)
        scheduler?.scheduleNextAlarm(force = true)
    }

    fun toolbarNotificationOrNull() = scheduler?.buildToolbarNotification()

    fun timerInitialDelayMs(): Long = TIMER_INITIAL_DELAY_MS

    fun timerIntervalMs(): Long = TIMER_INTERVAL_MS

    fun isTimerEnabled(): Boolean = config?.policy?.timer?.enabled == true

    fun handleTimerTick() {
        Log.d(RetentionLog.TAG, "Timer trigger fired.")
        try {
            scheduler?.handleTrigger(RetentionTriggerType.TIMER)
        } catch (throwable: Throwable) {
            Log.e(RetentionLog.TAG, "Unhandled exception while processing timer trigger.", throwable)
        }
    }

    fun handleRuntimeRecovery(context: Context, action: String?) {
        if (!ensureInitialized(context)) {
            Log.w(RetentionLog.TAG, "Skip runtime recovery because retention engine is disabled. action=$action")
            return
        }
        Log.d(RetentionLog.TAG, "Handling runtime recovery broadcast. action=$action")
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> handleBoot(context)
            Intent.ACTION_USER_PRESENT -> {
                scheduler?.refreshToolbar(application)
                dispatchUnlockTrigger("user_present")
            }

            Intent.ACTION_SCREEN_ON -> {
                scheduler?.refreshToolbar(application)
                if (isDeviceLocked()) {
                    Log.d(RetentionLog.TAG, "Skip screen_on unlock proxy because device is still locked.")
                } else {
                    dispatchUnlockTrigger("screen_on")
                }
            }

            Intent.ACTION_SCREEN_OFF -> {
                scheduler?.refreshToolbar(application)
            }

            else -> {
                scheduler?.refreshToolbar(application)
            }
        }
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

    private fun dispatchUnlockTrigger(source: String) {
        backgroundHandler?.postDelayed(
            {
                Log.d(RetentionLog.TAG, "Dispatch unlock trigger after delay. source=$source")
                scheduler?.handleTrigger(RetentionTriggerType.UNLOCK)
            },
            UNLOCK_DELAY_MS,
        )
    }

    private fun isDeviceLocked(): Boolean {
        val keyguardManager =
            application.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            keyguardManager.isDeviceLocked
        } else {
            keyguardManager.isKeyguardLocked
        }
    }
}
