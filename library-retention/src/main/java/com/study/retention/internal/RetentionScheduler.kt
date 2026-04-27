package com.study.retention.internal

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.study.retention.receiver.RetentionAlarmReceiver
import com.study.retention.service.RetentionToolbarService
import java.util.Calendar

internal class RetentionScheduler(
    private val application: Application,
    private val config: RetentionRuntimeConfig,
    private val store: RetentionStore,
) {

    private val notifier = RetentionNotifier(application, config)

    fun refreshToolbar(context: Context = application) {
        val safeContext = context.applicationContext
        val hasPermission = hasNotificationPermission()
        Log.d(
            RetentionLog.TAG,
            "Refreshing toolbar. enabled=${config.toolbar.enabled}, itemCount=${config.toolbar.items.size}, hasPermission=$hasPermission",
        )
        if (!config.toolbar.enabled || config.toolbar.items.isEmpty() || !hasPermission) {
            Log.d(RetentionLog.TAG, "Skip toolbar notification. enabled=${config.toolbar.enabled}, itemCount=${config.toolbar.items.size}, hasPermission=$hasPermission")
            notifier.cancelToolbarNotification()
            safeContext.stopService(Intent(safeContext, RetentionToolbarService::class.java))
            return
        }
        val items = config.toolbar.items.take(MAX_TOOLBAR_ITEMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context is Application) {
            runCatching { notifier.showToolbarNotification(items) }
                .onSuccess { Log.d(RetentionLog.TAG, "Toolbar notification shown directly. itemCount=${items.size}") }
                .onFailure { Log.w(RetentionLog.TAG, "Failed to show toolbar notification", it) }
            safeContext.stopService(Intent(safeContext, RetentionToolbarService::class.java))
            return
        }
        val serviceIntent = Intent(safeContext, RetentionToolbarService::class.java)
        ContextCompat.startForegroundService(safeContext, serviceIntent)
        Log.d(RetentionLog.TAG, "Toolbar foreground service started.")
    }

    fun handleTrigger(trigger: RetentionTriggerType) {
        val hasPermission = hasNotificationPermission()
        if (!hasPermission) {
            Log.d(RetentionLog.TAG, "Skip ${trigger.extraValue} trigger because notification permission is missing.")
            return
        }
        if (RetentionAppVisibilityTracker.isAppForeground()) {
            Log.d(RetentionLog.TAG, "Skip ${trigger.extraValue} trigger because app is in foreground.")
            return
        }
        if (isQuietHours()) {
            Log.d(RetentionLog.TAG, "Skip ${trigger.extraValue} trigger because current time is within quiet hours.")
            return
        }
        if (!canPassGlobalCooldown()) {
            Log.d(RetentionLog.TAG, "Skip ${trigger.extraValue} trigger because global cooldown is active.")
            return
        }
        val policy = policyFor(trigger)
        if (!canShow(trigger, policy)) {
            Log.d(RetentionLog.TAG, "Skip ${trigger.extraValue} trigger because policy check failed.")
            return
        }
        val item = nextReminderItem() ?: return
        runCatching {
            notifier.showReminderNotification(item, trigger)
            store.recordShown(trigger)
            Log.d(RetentionLog.TAG, "Reminder notification shown. trigger=${trigger.extraValue}, itemId=${item.id}, bucketId=${item.bucketId}")
        }.onFailure {
            Log.w(RetentionLog.TAG, "Failed to show reminder for ${trigger.extraValue}", it)
        }
    }

    fun scheduleNextAlarm(force: Boolean = false) {
        val policy = config.policy.alarm
        if (!policy.enabled || policy.intervalMinutes <= 0) {
            Log.d(RetentionLog.TAG, "Skip alarm scheduling. enabled=${policy.enabled}, intervalMinutes=${policy.intervalMinutes}")
            return
        }
        val now = System.currentTimeMillis()
        val scheduledAt = store.getNextScheduledAlarmAt()
        if (!force && scheduledAt > now) {
            Log.d(RetentionLog.TAG, "Keep existing alarm. scheduledAt=$scheduledAt now=$now")
            return
        }
        val alarmManager =
            application.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = now + policy.intervalMinutes * MILLIS_PER_MINUTE
        val intent = Intent(application, RetentionAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        store.setNextScheduledAlarmAt(triggerAt)
        Log.d(RetentionLog.TAG, "Alarm scheduled. force=$force triggerAt=$triggerAt intervalMinutes=${policy.intervalMinutes}")
    }

    fun buildToolbarNotification(): android.app.Notification {
        Log.d(RetentionLog.TAG, "Building toolbar notification for foreground service.")
        return notifier.showToolbarNotification(config.toolbar.items.take(MAX_TOOLBAR_ITEMS))
    }

    private fun canShow(trigger: RetentionTriggerType, policy: TriggerPolicy): Boolean {
        if (!policy.enabled) {
            Log.d(RetentionLog.TAG, "Policy disabled for trigger=${trigger.extraValue}")
            return false
        }
        val lastShownAt = store.getLastShownAt(trigger)
        val todayCount = store.getTodayCount(trigger)
        val intervalSatisfied =
            policy.intervalMinutes <= 0 ||
                System.currentTimeMillis() - lastShownAt >=
                policy.intervalMinutes * MILLIS_PER_MINUTE
        val countSatisfied = policy.dailyLimit <= 0 || todayCount < policy.dailyLimit
        Log.d(
            RetentionLog.TAG,
            "Policy check. trigger=${trigger.extraValue}, lastShownAt=$lastShownAt, todayCount=$todayCount, intervalMinutes=${policy.intervalMinutes}, dailyLimit=${policy.dailyLimit}, intervalSatisfied=$intervalSatisfied, countSatisfied=$countSatisfied",
        )
        return intervalSatisfied && countSatisfied
    }

    private fun canPassGlobalCooldown(): Boolean {
        val policy = config.policy.globalCooldown
        if (!policy.enabled || policy.intervalMinutes <= 0) {
            Log.d(
                RetentionLog.TAG,
                "Global cooldown disabled. enabled=${policy.enabled} intervalMinutes=${policy.intervalMinutes}",
            )
            return true
        }
        val lastShownAt = store.getGlobalLastShownAt()
        val satisfied =
            lastShownAt <= 0L ||
                System.currentTimeMillis() - lastShownAt >=
                policy.intervalMinutes * MILLIS_PER_MINUTE
        Log.d(
            RetentionLog.TAG,
            "Global cooldown check. lastShownAt=$lastShownAt intervalMinutes=${policy.intervalMinutes} satisfied=$satisfied",
        )
        return satisfied
    }

    private fun nextReminderItem(): ReminderItemConfig? {
        val allItems = config.reminders.items
        if (allItems.isEmpty()) {
            Log.d(RetentionLog.TAG, "No reminder items available.")
            return null
        }
        val bucketOrder = config.reminders.bucketOrder
        if (bucketOrder.isEmpty()) {
            val item = allItems[store.nextNaturalReminderIndex(allItems.size)]
            Log.d(RetentionLog.TAG, "Selected reminder by natural order. itemId=${item.id}, bucketId=${item.bucketId}")
            return item
        }
        val startIndex = store.advanceBucketCursor(bucketOrder.size)
        Log.d(RetentionLog.TAG, "Selecting reminder by bucketOrder=$bucketOrder startIndex=$startIndex")
        for (offset in bucketOrder.indices) {
            val index = (startIndex + offset) % bucketOrder.size
            val bucketId = bucketOrder[index]
            val bucketItems = allItems.filter { it.bucketId == bucketId }
            if (bucketItems.isEmpty()) {
                Log.d(RetentionLog.TAG, "Bucket $bucketId has no items, skip.")
                continue
            }
            store.setBucketCursor(index)
            val rotationIndex = store.nextBucketRotationIndex(bucketId, bucketItems.size)
            val item = bucketItems[rotationIndex]
            Log.d(RetentionLog.TAG, "Selected reminder item. bucketId=$bucketId rotationIndex=$rotationIndex itemId=${item.id}")
            return item
        }
        Log.d(RetentionLog.TAG, "No reminder item matched current bucket order.")
        return null
    }

    private fun policyFor(trigger: RetentionTriggerType): TriggerPolicy {
        return when (trigger) {
            RetentionTriggerType.TIMER -> config.policy.timer
            RetentionTriggerType.UNLOCK -> config.policy.unlock
            RetentionTriggerType.ALARM, RetentionTriggerType.BOOT -> config.policy.alarm
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(application).areNotificationsEnabled()
        } else {
            application.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isQuietHours(): Boolean {
        val quietHours = config.policy.quietHours ?: return false
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val inQuietHours = quietHours.containsHour(currentHour)
        Log.d(
            RetentionLog.TAG,
            "Quiet hours check. currentHour=$currentHour start=${quietHours.startHourInclusive} end=${quietHours.endHourExclusive} inQuietHours=$inQuietHours",
        )
        return inQuietHours
    }

    private companion object {
        private const val ALARM_REQUEST_CODE = 55_031
        private const val MAX_TOOLBAR_ITEMS = 4
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
