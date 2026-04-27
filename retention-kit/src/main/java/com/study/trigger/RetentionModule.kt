package com.study.trigger

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.study.trigger.internal.RetentionBuiltInConfigFactory
import com.study.trigger.internal.RetentionDispatcher
import com.study.trigger.internal.RetentionForegroundTracker
import com.study.trigger.internal.RetentionNotificationFactory
import com.study.trigger.internal.RetentionStateStore
import com.study.trigger.service.ToolbarForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

object RetentionModule {

    const val EXTRA_RETENTION_ENTRY = "retention_entry"
    const val EXTRA_RETENTION_SOURCE = "retention_source"
    const val EXTRA_LEGACY_ORIGIN_REMINDER_TYPE = "EXTRA_KEY_ORIGIN_REMINDER_TYPE"
    const val EXTRA_LEGACY_REMINDER_TYPE = "EXTRA_KEY_REMINDER_TYPE"
    const val EXTRA_LEGACY_JUMP_TO = "EXTRA_JUMP_TO"
    const val REQUEST_CODE_NOTIFICATION_PERMISSION = 44030
    const val REQUEST_CODE_STORAGE_PERMISSION = 44031

    private lateinit var application: Application
    private lateinit var stateStore: RetentionStateStore
    private val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var config: RetentionConfig? = null
    private var initialized = false
    private var unlockReceiverRegistered = false
    private var runtimeJob: Job? = null

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            runtimeScope.launch {
                delay(1_000L)
                triggerReminder(ReminderTrigger.UNLOCK)
            }
        }
    }

    @Synchronized
    fun initialize(application: Application) {
        this.application = application
        this.config = RetentionBuiltInConfigFactory.create(application)
        this.stateStore = RetentionStateStore(application)
        if (!initialized) {
            application.registerActivityLifecycleCallbacks(RetentionForegroundTracker)
            initialized = true
        }
        registerUnlockReceiverIfNeeded()
        ensureRunning(application, forceAlarmReschedule = true)
    }

    fun ensureRunning(context: Context = app(), forceAlarmReschedule: Boolean = false) {
        if (!isInitialized()) {
            return
        }
        refreshToolbar(context)
        RetentionDispatcher.scheduleNextAlarmIfNeeded(force = forceAlarmReschedule)
        restartRuntime()
    }

    fun refreshToolbar(context: Context = app()) {
        val safeContext = context.applicationContext
        if (stateStore().getDeviceCountryCode(Locale.getDefault().country)
                .equals("KR", ignoreCase = true)
        ) {
            NotificationManagerCompat.from(safeContext)
                .cancel(currentConfig().notificationConfig.toolbarNotificationId)
            safeContext.stopService(Intent(safeContext, ToolbarForegroundService::class.java))
            return
        }
        val toolbarItems = currentConfig().contentProvider.getToolbarItems(currentLocale()).take(4)
        if (toolbarItems.isEmpty()) {
            NotificationManagerCompat.from(safeContext)
                .cancel(currentConfig().notificationConfig.toolbarNotificationId)
            safeContext.stopService(Intent(safeContext, ToolbarForegroundService::class.java))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context is Application) {
            RetentionNotificationFactory.showToolbarNotification(toolbarItems)
            safeContext.stopService(Intent(safeContext, ToolbarForegroundService::class.java))
            return
        }
        val serviceIntent = Intent(context, ToolbarForegroundService::class.java)
        ContextCompat.startForegroundService(safeContext, serviceIntent)
    }

    fun triggerReminder(trigger: ReminderTrigger) {
        if (!isInitialized()) {
            return
        }
        RetentionDispatcher.tryShowReminder(trigger)
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_NOTIFICATION_PERMISSION,
        )
    }

    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                return
            }
            RetentionPermissionMonitor.monitorStoragePermissionPage(activity)
            openSettingsPage(
                activity = activity,
                primaryIntent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${activity.packageName}"),
                ),
                fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
            )
            return
        }
        val permissions = buildList {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_STORAGE_PERMISSION)
    }

    internal fun currentConfig(): RetentionConfig =
        requireNotNull(config) { "RetentionModule is not initialized." }

    internal fun currentConfigOrNull(): RetentionConfig? = config

    internal fun app(): Application {
        ensureInitialized()
        return application
    }

    internal fun stateStore(): RetentionStateStore {
        ensureInitialized()
        return stateStore
    }

    internal fun trackEvent(eventName: String, properties: Map<String, String> = emptyMap()) {
        currentConfig().analyticsReporter.report(eventName, properties)
    }

    private fun ensureInitialized() {
        check(isInitialized()) { "RetentionModule.initialize(application) must be called first." }
    }

    private fun openSettingsPage(
        activity: Activity,
        primaryIntent: Intent,
        fallbackIntent: Intent
    ) {
        runCatching {
            activity.startActivity(primaryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.recoverCatching {
            activity.startActivity(fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { error ->
            if (error !is ActivityNotFoundException) {
                throw error
            }
        }
    }

    private fun isInitialized(): Boolean = this::application.isInitialized && config != null

    private fun registerUnlockReceiverIfNeeded() {
        if (unlockReceiverRegistered) {
            return
        }
        application.registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        unlockReceiverRegistered = true
    }

    private fun restartRuntime() {
        runtimeJob?.cancel()
        runtimeJob = runtimeScope.launch {
            launch {
                if (!currentConfig().enableSessionPingLog) {
                    return@launch
                }
                while (isActive) {
                    trackEvent("app_session_ping")
                    delay(SESSION_PING_INTERVAL_MS)
                }
            }
            launch {
                delay(TIMER_INITIAL_DELAY_MS)
                while (isActive) {
                    triggerReminder(ReminderTrigger.TIMER)
                    delay(REMINDER_POLL_INTERVAL_MS)
                }
            }
            launch {
                delay(MEDIA_INITIAL_DELAY_MS)
                while (isActive) {
                    triggerReminder(ReminderTrigger.MEDIA)
                    delay(REMINDER_POLL_INTERVAL_MS)
                }
            }
        }
    }

    private fun currentLocale(): Locale {
        return stateStore().resolvePreferredLocale(application)
    }

    private const val MEDIA_INITIAL_DELAY_MS = 60_000L
    private const val REMINDER_POLL_INTERVAL_MS = 60_000L
    private const val SESSION_PING_INTERVAL_MS = 600_000L
    private const val TIMER_INITIAL_DELAY_MS = 20_000L
}
