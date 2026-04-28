package com.study.retention.internal

import android.app.Application
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

internal object RetentionEngine {

    private const val APP_BACKGROUND_DELAY_MS = 1_000L
    private const val TIMER_INITIAL_DELAY_MS = 20_000L
    private const val TIMER_INTERVAL_MS = 60_000L
    private const val UNLOCK_DELAY_MS = 1_000L

    private enum class RetentionRuntimeMode {
        BASIC,
        FOREGROUND,
    }

    @Volatile
    private var config: RetentionRuntimeConfig? = null
    private lateinit var application: Application
    private lateinit var store: RetentionStore
    private var scheduler: RetentionScheduler? = null
    private var initialized = false
    private var lifecycleRegistered = false
    private var runtimeReceiverRegistered = false
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val foregroundSessions = linkedSetOf<String>()
    private var runtimeMode = RetentionRuntimeMode.BASIC
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (runtimeMode != RetentionRuntimeMode.FOREGROUND) {
                Log.d(RetentionLog.TAG, "Stop timer loop because runtime mode is basic.")
                return
            }
            if (config?.policy?.timer?.enabled != true) {
                Log.d(RetentionLog.TAG, "Stop timer loop because timer policy is disabled.")
                return
            }
            handleTimerTick()
            backgroundHandler?.postDelayed(this, TIMER_INTERVAL_MS)
            Log.d(RetentionLog.TAG, "Next foreground runtime timer tick scheduled after ${TIMER_INTERVAL_MS}ms.")
        }
    }

    private val runtimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val safeContext = context ?: return
            handleRuntimeRecovery(safeContext, intent?.action)
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
        registerRuntimeReceivers()
        Log.d(
            RetentionLog.TAG,
            "Retention config loaded. toolbarItems=${loadedConfig.toolbar.items.size}, reminderItems=${loadedConfig.reminders.items.size}, bucketOrder=${loadedConfig.reminders.bucketOrder}",
        )
        scheduler?.scheduleNextAlarm(force = true)
        RetentionWorkScheduler.sync(app, loadedConfig)
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

    fun enterForegroundRuntime(context: Context, sessionId: String, source: String) {
        if (!ensureInitialized(context)) {
            Log.w(RetentionLog.TAG, "Skip entering foreground runtime because retention engine is disabled.")
            return
        }
        if (sessionId.isBlank()) {
            Log.w(RetentionLog.TAG, "Skip entering foreground runtime because sessionId is blank.")
            return
        }
        foregroundSessions += sessionId
        runtimeMode = RetentionRuntimeMode.FOREGROUND
        Log.d(
            RetentionLog.TAG,
            "Foreground runtime entered. sessionId=$sessionId source=$source activeSessions=${foregroundSessions.size}",
        )
        startTimerLoop()
    }

    fun exitForegroundRuntime(sessionId: String?) {
        if (sessionId.isNullOrBlank()) {
            foregroundSessions.clear()
        } else {
            foregroundSessions -= sessionId
        }
        if (foregroundSessions.isEmpty()) {
            runtimeMode = RetentionRuntimeMode.BASIC
            stopTimerLoop()
        }
        Log.d(
            RetentionLog.TAG,
            "Foreground runtime exited. sessionId=$sessionId activeSessions=${foregroundSessions.size} mode=$runtimeMode",
        )
    }

    fun onAppForegroundEntered() {
        if (config == null) {
            return
        }
        Log.d(RetentionLog.TAG, "App entered foreground. Refreshing retention runtime surfaces.")
        scheduler?.refreshToolbar(application)
    }

    fun handleTimerTick() {
        Log.d(RetentionLog.TAG, "Timer trigger fired.")
        try {
            scheduler?.handleTrigger(RetentionTriggerType.TIMER)
        } catch (throwable: Throwable) {
            Log.e(RetentionLog.TAG, "Unhandled exception while processing timer trigger.", throwable)
        }
    }

    fun handleHeartbeatWork(context: Context) {
        if (!ensureInitialized(context)) {
            Log.w(RetentionLog.TAG, "Skip WorkManager heartbeat because retention engine is disabled.")
            return
        }
        Log.d(RetentionLog.TAG, "Handling WorkManager heartbeat. mode=$runtimeMode")
        scheduler?.refreshToolbar(application)
        scheduler?.scheduleNextAlarm(force = false)
        if (runtimeMode == RetentionRuntimeMode.BASIC && config?.policy?.timer?.enabled == true) {
            Log.d(RetentionLog.TAG, "Dispatch low-frequency timer tick from WorkManager heartbeat.")
            handleTimerTick()
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
            Intent.ACTION_POWER_CONNECTED -> {
                scheduler?.refreshToolbar(application)
                handleImmediateTrigger(RetentionTriggerType.CHARGING, "power_connected")
            }

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
                handleImmediateTrigger(RetentionTriggerType.SCREEN_OFF, "screen_off")
            }

            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                scheduler?.refreshToolbar(application)
                scheduler?.scheduleNextAlarm(force = true)
                handleImmediateTrigger(RetentionTriggerType.PACKAGE_REPLACED, "package_replaced")
            }

            else -> {
                scheduler?.refreshToolbar(application)
            }
        }
    }

    fun onAppBackground() {
        val handler = backgroundHandler ?: return
        handler.postDelayed(
            {
                if (RetentionAppVisibilityTracker.isAppForeground()) {
                    Log.d(RetentionLog.TAG, "Skip app_background trigger because app returned to foreground.")
                    return@postDelayed
                }
                handleImmediateTrigger(RetentionTriggerType.APP_BACKGROUND, "app_background")
            },
            APP_BACKGROUND_DELAY_MS,
        )
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

    private fun startTimerLoop() {
        ensureHandler()
        if (config?.policy?.timer?.enabled != true) {
            Log.d(RetentionLog.TAG, "Skip starting timer loop because timer policy is disabled.")
            return
        }
        val handler = backgroundHandler ?: return
        handler.removeCallbacks(timerRunnable)
        handler.postDelayed(timerRunnable, TIMER_INITIAL_DELAY_MS)
        Log.d(
            RetentionLog.TAG,
            "Foreground runtime timer loop started. firstDelayMs=$TIMER_INITIAL_DELAY_MS intervalMs=$TIMER_INTERVAL_MS",
        )
    }

    private fun stopTimerLoop() {
        backgroundHandler?.removeCallbacks(timerRunnable)
        Log.d(RetentionLog.TAG, "Foreground runtime timer loop stopped.")
    }

    private fun registerLifecycleCallbacks() {
        if (lifecycleRegistered) {
            return
        }
        application.registerActivityLifecycleCallbacks(RetentionAppVisibilityTracker)
        lifecycleRegistered = true
        Log.d(RetentionLog.TAG, "Activity lifecycle callbacks registered.")
    }

    private fun registerRuntimeReceivers() {
        if (runtimeReceiverRegistered) {
            return
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(runtimeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(runtimeReceiver, filter)
        }
        runtimeReceiverRegistered = true
        Log.d(RetentionLog.TAG, "Runtime receivers registered.")
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

    private fun handleImmediateTrigger(trigger: RetentionTriggerType, source: String) {
        Log.d(RetentionLog.TAG, "Dispatch ${trigger.extraValue} trigger. source=$source")
        scheduler?.handleTrigger(trigger)
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
