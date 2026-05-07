package com.study.retention.internal

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresPermission

internal object RetentionEngine {

    private const val APP_BACKGROUND_DELAY_MS = 1_000L
    private const val TIMER_INITIAL_DELAY_MS = 20_000L
    private const val UNLOCK_DELAY_MS = 1_000L
    private const val UNLOCK_MONITOR_START_DELAY_MS = 1_000L
    private const val UNLOCK_MONITOR_INTERVAL_MS = 500L
    private const val UNLOCK_MONITOR_TIMEOUT_MS = 15_000L
    private const val UNLOCK_MONITOR_STABLE_UNLOCK_COUNT = 2
    private const val UNLOCK_DUPLICATE_SUPPRESSION_MS = 2_000L
    private const val MILLIS_PER_MINUTE = 60_000L

    private enum class RetentionRuntimeMode {
        BASIC, FOREGROUND,
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
    private var unlockMonitorSource: String? = null
    private var unlockMonitorDeadlineAt = 0L
    private var unlockMonitorUnlockedCount = 0
    private var lastUnlockDispatchAt = 0L
    private val timerRunnable = object : Runnable {
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        override fun run() {
            if (runtimeMode != RetentionRuntimeMode.FOREGROUND) {
                Log.d(RetentionLog.TAG, "Stop timer loop because runtime mode is basic.")
                return
            }
            if (config?.policy?.timer?.enabled != true) {
                Log.d(RetentionLog.TAG, "Stop timer loop because timer policy is disabled.")
                return
            }
            val intervalMs = configuredTimerIntervalMs()
            if (intervalMs <= 0L) {
                Log.d(
                    RetentionLog.TAG,
                    "Stop timer loop because configured timer interval is invalid."
                )
                return
            }
            handleTimerTick()
            backgroundHandler?.postDelayed(this, intervalMs)
            Log.d(
                RetentionLog.TAG,
                "Next foreground runtime timer tick scheduled. intervalMinutes=${config?.policy?.timer?.intervalMinutes} intervalMs=$intervalMs",
            )
        }
    }
    private val unlockMonitorRunnable = object : Runnable {
        override fun run() {
            val source = unlockMonitorSource ?: return
            if (!isKeyguardLocked()) {
                unlockMonitorUnlockedCount += 1
                Log.d(
                    RetentionLog.TAG,
                    "Unlock monitor detected keyguard dismissed. source=$source stableCount=$unlockMonitorUnlockedCount",
                )
                if (unlockMonitorUnlockedCount >= UNLOCK_MONITOR_STABLE_UNLOCK_COUNT) {
                    stopUnlockMonitor("detected_unlocked")
                    dispatchUnlockTrigger("${source}_monitor")
                    return
                }
            } else if (unlockMonitorUnlockedCount != 0) {
                Log.d(
                    RetentionLog.TAG,
                    "Unlock monitor reset stable count because keyguard is visible again. source=$source",
                )
                unlockMonitorUnlockedCount = 0
            }
            if (SystemClock.elapsedRealtime() >= unlockMonitorDeadlineAt) {
                Log.d(
                    RetentionLog.TAG,
                    "Stop unlock monitor because timeout reached. source=$source timeoutMs=$UNLOCK_MONITOR_TIMEOUT_MS",
                )
                stopUnlockMonitor("timeout")
                return
            }
            backgroundHandler?.postDelayed(this, UNLOCK_MONITOR_INTERVAL_MS)
        }
    }

    private val runtimeReceiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        override fun onReceive(context: Context?, intent: Intent?) {
            val safeContext = context ?: return
            handleRuntimeRecovery(safeContext, intent?.action)
        }
    }

    @Synchronized
    fun initialize(context: Context): Boolean {
        if (initialized) {
            Log.d(
                RetentionLog.TAG, "Retention engine already initialized. enabled=${config != null}"
            )
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

    @androidx.annotation.RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun handleAlarm(context: Context) {
        if (!ensureInitialized(context)) {
            Log.w(RetentionLog.TAG, "Skip alarm handling because retention engine is disabled.")
            return
        }
        Log.d(RetentionLog.TAG, "Handling alarm trigger.")
        scheduler?.handleTrigger(RetentionTriggerType.ALARM)
        scheduler?.scheduleNextAlarm(force = true)
    }

    @androidx.annotation.RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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
            Log.w(
                RetentionLog.TAG, "Skip permission recovery because retention engine is disabled."
            )
            return
        }
        Log.d(RetentionLog.TAG, "Notification permission granted. Recovering retention runtime.")
        scheduler?.refreshToolbar(application)
        scheduler?.scheduleNextAlarm(force = true)
    }

    fun enterForegroundRuntime(context: Context, sessionId: String, source: String) {
        if (!ensureInitialized(context)) {
            Log.w(
                RetentionLog.TAG,
                "Skip entering foreground runtime because retention engine is disabled."
            )
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

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun handleTimerTick() {
        Log.d(RetentionLog.TAG, "Timer trigger fired.")
        try {
            scheduler?.handleTrigger(RetentionTriggerType.TIMER)
        } catch (throwable: Throwable) {
            Log.e(
                RetentionLog.TAG, "Unhandled exception while processing timer trigger.", throwable
            )
        }
    }

    fun handleHeartbeatWork(context: Context) {
        if (!ensureInitialized(context)) {
            Log.w(
                RetentionLog.TAG, "Skip WorkManager heartbeat because retention engine is disabled."
            )
            return
        }
        Log.d(RetentionLog.TAG, "Handling WorkManager heartbeat. mode=$runtimeMode")
        scheduler?.refreshToolbar(application)
        scheduler?.scheduleNextAlarm(force = false)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun handleRuntimeRecovery(context: Context, action: String?) {
        if (!ensureInitialized(context)) {
            Log.w(
                RetentionLog.TAG,
                "Skip runtime recovery because retention engine is disabled. action=$action"
            )
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
                stopUnlockMonitor("user_present")
                dispatchUnlockTrigger("user_present")
            }

            Intent.ACTION_SCREEN_ON -> {
                scheduler?.refreshToolbar(application)
                startUnlockMonitor("screen_on")
            }

            Intent.ACTION_SCREEN_OFF -> {
                stopUnlockMonitor("screen_off")
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

    @SuppressLint("MissingPermission")
    fun onAppBackground() {
        val handler = backgroundHandler ?: return
        handler.postDelayed(
            @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS) {
                if (RetentionAppVisibilityTracker.isAppForeground()) {
                    Log.d(
                        RetentionLog.TAG,
                        "Skip app_background trigger because app returned to foreground."
                    )
                    return@postDelayed
                }
                handleImmediateTrigger(RetentionTriggerType.APP_BACKGROUND, "app_background")
            },
            APP_BACKGROUND_DELAY_MS,
        )
    }

    fun currentConfigOrNull(): RetentionRuntimeConfig? = config

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
        val intervalMs = configuredTimerIntervalMs()
        if (intervalMs <= 0L) {
            Log.d(
                RetentionLog.TAG,
                "Skip starting timer loop because configured timer interval is invalid. intervalMinutes=${config?.policy?.timer?.intervalMinutes}",
            )
            return
        }
        handler.removeCallbacks(timerRunnable)
        handler.postDelayed(timerRunnable, TIMER_INITIAL_DELAY_MS)
        Log.d(
            RetentionLog.TAG,
            "Foreground runtime timer loop started. firstDelayMs=$TIMER_INITIAL_DELAY_MS intervalMinutes=${config?.policy?.timer?.intervalMinutes} intervalMs=$intervalMs",
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

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun dispatchUnlockTrigger(source: String) {
        backgroundHandler?.postDelayed(
            {
                val now = SystemClock.elapsedRealtime()
                if (now - lastUnlockDispatchAt < UNLOCK_DUPLICATE_SUPPRESSION_MS) {
                    Log.d(
                        RetentionLog.TAG,
                        "Skip duplicate unlock trigger. source=$source lastDispatchAgoMs=${now - lastUnlockDispatchAt}",
                    )
                    return@postDelayed
                }
                lastUnlockDispatchAt = now
                Log.d(RetentionLog.TAG, "Dispatch unlock trigger after delay. source=$source")
                scheduler?.handleTrigger(RetentionTriggerType.UNLOCK)
            },
            UNLOCK_DELAY_MS,
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun handleImmediateTrigger(trigger: RetentionTriggerType, source: String) {
        Log.d(RetentionLog.TAG, "Dispatch ${trigger.extraValue} trigger. source=$source")
        scheduler?.handleTrigger(trigger)
    }

    private fun startUnlockMonitor(source: String) {
        ensureHandler()
        unlockMonitorSource = source
        unlockMonitorDeadlineAt = SystemClock.elapsedRealtime() + UNLOCK_MONITOR_TIMEOUT_MS
        unlockMonitorUnlockedCount = 0
        backgroundHandler?.removeCallbacks(unlockMonitorRunnable)
        Log.d(
            RetentionLog.TAG,
            "Start unlock monitor. source=$source isKeyguardLocked=${isKeyguardLocked()} startDelayMs=$UNLOCK_MONITOR_START_DELAY_MS intervalMs=$UNLOCK_MONITOR_INTERVAL_MS stableUnlockCount=$UNLOCK_MONITOR_STABLE_UNLOCK_COUNT timeoutMs=$UNLOCK_MONITOR_TIMEOUT_MS",
        )
        backgroundHandler?.postDelayed(unlockMonitorRunnable, UNLOCK_MONITOR_START_DELAY_MS)
    }

    private fun stopUnlockMonitor(reason: String) {
        if (unlockMonitorSource == null) {
            return
        }
        Log.d(
            RetentionLog.TAG,
            "Stop unlock monitor. source=$unlockMonitorSource reason=$reason",
        )
        unlockMonitorSource = null
        unlockMonitorDeadlineAt = 0L
        unlockMonitorUnlockedCount = 0
        backgroundHandler?.removeCallbacks(unlockMonitorRunnable)
    }

    private fun isKeyguardLocked(): Boolean {
        val keyguardManager =
            application.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                ?: return false
        return keyguardManager.isKeyguardLocked
    }

    private fun configuredTimerIntervalMs(): Long {
        val intervalMinutes = config?.policy?.timer?.intervalMinutes ?: return 0L
        return intervalMinutes.coerceAtLeast(0).toLong() * MILLIS_PER_MINUTE
    }
}
