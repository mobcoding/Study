package com.study.trigger.internal

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import com.study.trigger.ReminderContent
import com.study.trigger.ReminderPolicy
import com.study.trigger.ReminderTrigger
import com.study.trigger.RetentionModule
import com.study.trigger.RetentionStrategyConfig
import com.study.trigger.receiver.AlarmReceiver
import java.util.Locale

internal object RetentionDispatcher {

    fun scheduleNextAlarmIfNeeded(force: Boolean = false) {
        val config = RetentionModule.currentConfig()
        val alarmPolicy = config.strategyConfig.alarmPolicy
        if (!alarmPolicy.enabled || alarmPolicy.intervalMinutes <= 0) {
            return
        }
        val stateStore = RetentionModule.stateStore()
        val now = System.currentTimeMillis()
        val scheduledAt = stateStore.getNextScheduledAlarmAt()
        if (!force && scheduledAt > now) {
            return
        }
        val triggerAt = now + alarmPolicy.intervalMinutes * MILLIS_PER_MINUTE
        val alarmManager =
            RetentionModule.app().getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(RetentionModule.app(), AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            RetentionModule.app(),
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        stateStore.setNextScheduledAlarmAt(triggerAt)
    }

    fun tryShowReminder(trigger: ReminderTrigger) {
        val config = RetentionModule.currentConfig()
        val stateStore = RetentionModule.stateStore()
        val countryCode = stateStore.getDeviceCountryCode(Locale.getDefault().country)
        if (config.strategyConfig.blockedCountryCodes.contains(countryCode.uppercase(Locale.US))) {
            return
        }
        if (RetentionForegroundTracker.isAppForeground()) {
            return
        }
        when (trigger) {
            ReminderTrigger.TIMER -> {
                if (config.strategyConfig.standardRemindersEnabled && hasNotificationPermission() && canShowStandard(
                        trigger
                    )
                ) {
                    showStandardNotification(trigger)
                }
            }

            ReminderTrigger.UNLOCK -> {
                val canShow = canShowStandard(trigger)
                if (canShow) {
                    showMediaStyleNotification(trigger)
                }
                if (canShow && config.strategyConfig.standardRemindersEnabled && hasNotificationPermission()) {
                    showStandardNotification(trigger)
                }
            }

            ReminderTrigger.ALARM -> {
                showMediaStyleNotification(trigger)
                if (config.strategyConfig.standardRemindersEnabled && hasNotificationPermission()) {
                    showStandardNotification(trigger)
                }
            }

            ReminderTrigger.MEDIA -> {
                if (canShowStandard(trigger)) {
                    showMediaStyleNotification(trigger)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showStandardNotification(trigger: ReminderTrigger) {
        val content = nextReminderContent() ?: return
        RetentionNotificationFactory.ensureReminderChannel()
        acquireWakeLock()
        NotificationManagerCompat.from(RetentionModule.app()).notify(
            RetentionNotificationFactory.resolveStandardNotificationId(content.bucketId),
            RetentionNotificationFactory.buildStandardReminderNotification(content, trigger),
        )
        RetentionReport.report(
            "reminder_standard_shown",
            bundleOf("has_permission" to hasNotificationPermission())
        )
        RetentionModule.stateStore().recordShown(trigger)
        RetentionModule.trackEvent(
            "reminder_standard_shown",
            mapOf("trigger" to trigger.name.lowercase(Locale.US), "target" to content.target.id),
        )
    }

    @SuppressLint("MissingPermission")
    private fun showMediaStyleNotification(trigger: ReminderTrigger) {
        val config = RetentionModule.currentConfig()
        if (!config.behaviorConfig.specialMediaModeEnabled || !config.strategyConfig.mediaStyleEnabled) {
            return
        }
        val content = nextReminderContent() ?: return
        RetentionNotificationFactory.ensureReminderChannel()
        acquireWakeLock()
        NotificationManagerCompat.from(RetentionModule.app()).notify(
            config.notificationConfig.mediaStyleNotificationId,
            RetentionNotificationFactory.buildMediaReminderNotification(content, trigger),
        )
        RetentionModule.stateStore().recordShown(trigger)
        RetentionModule.trackEvent(
            "reminder_media_shown",
            mapOf("trigger" to trigger.name.lowercase(Locale.US), "target" to content.target.id),
        )
    }

    private fun canShowStandard(trigger: ReminderTrigger): Boolean {
        val policy = standardPolicyFor(trigger, RetentionModule.currentConfig().strategyConfig)
            ?: return false
        return canShow(trigger, policy)
    }

    private fun canShow(trigger: ReminderTrigger, policy: ReminderPolicy): Boolean {
        if (!policy.enabled) {
            return false
        }
        val stateStore = RetentionModule.stateStore()
        val lastShownAt = stateStore.getLastShownAt(trigger)
        val todayCount = stateStore.getTodayCount(trigger)
        val intervalSatisfied =
            policy.intervalMinutes <= 0 || System.currentTimeMillis() - lastShownAt >= policy.intervalMinutes * MILLIS_PER_MINUTE
        val countSatisfied = policy.dailyLimit <= 0 || todayCount < policy.dailyLimit
        return intervalSatisfied && countSatisfied
    }

    private fun nextReminderContent(): ReminderContent? {
        val config = RetentionModule.currentConfig()
        val locales = RetentionModule.app().resources.configuration.locales
        val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
        val allContents = config.contentProvider.getReminderContents(locale)
        if (allContents.isEmpty()) {
            return null
        }
        val bucketId =
            RetentionModule.stateStore().nextBucketId(config.notificationConfig.bucketOrder)
                ?: return null
        val bucketContents = allContents.filter { it.bucketId == bucketId }
        if (bucketContents.isEmpty()) {
            return allContents.firstOrNull()
        }
        val rotationIndex =
            RetentionModule.stateStore().nextRotationIndex(bucketId, bucketContents.size)
        return bucketContents[rotationIndex]
    }

    private fun standardPolicyFor(
        trigger: ReminderTrigger,
        strategyConfig: RetentionStrategyConfig
    ): ReminderPolicy? =
        when (trigger) {
            ReminderTrigger.TIMER -> strategyConfig.timerPolicy
            ReminderTrigger.UNLOCK -> strategyConfig.unlockPolicy
            ReminderTrigger.ALARM -> ReminderPolicy(
                enabled = strategyConfig.alarmPolicy.enabled,
                intervalMinutes = 0,
                dailyLimit = 0,
            )

            ReminderTrigger.MEDIA -> strategyConfig.mediaPolicy
        }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(RetentionModule.app()).areNotificationsEnabled()
        } else {
            RetentionModule.app()
                .checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun acquireWakeLock() {
        val powerManager =
            RetentionModule.app().getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        runCatching {
            @Suppress("DEPRECATION")
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "retention-kit:notify",
            ).acquire(WAKE_LOCK_TIMEOUT_MS)
        }.onFailure {
            Log.w("RetentionDispatcher", "Failed to acquire wake lock", it)
        }
    }

    private const val ALARM_REQUEST_CODE = 44_031
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val WAKE_LOCK_TIMEOUT_MS = 3_000L
}
