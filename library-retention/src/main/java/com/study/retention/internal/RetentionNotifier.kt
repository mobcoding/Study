package com.study.retention.internal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.random.Random

internal class RetentionNotifier(
    private val context: Context,
    private val config: RetentionRuntimeConfig,
) {

    fun ensureReminderChannel() {
        ensureChannel(
            config.notification.reminderChannelId,
            context.getString(config.notification.reminderChannelNameResId),
            NotificationManager.IMPORTANCE_HIGH,
            withSound = true,
        )
    }

    fun ensureToolbarChannel() {
        ensureChannel(
            config.notification.toolbarChannelId,
            context.getString(config.notification.toolbarChannelNameResId),
            NotificationManager.IMPORTANCE_LOW,
            withSound = false,
        )
    }

    fun showToolbarNotification(items: List<ToolbarItemConfig>): Notification {
        ensureToolbarChannel()
        val notification = buildToolbarNotification(items)
        NotificationManagerCompat.from(context).notify(config.notification.toolbarNotificationId, notification)
        Log.d(RetentionLog.TAG, "Toolbar notification posted. itemCount=${items.size}")
        return notification
    }

    fun buildToolbarForegroundNotification(items: List<ToolbarItemConfig>): Notification {
        ensureToolbarChannel()
        return buildToolbarNotification(items)
    }

    fun cancelToolbarNotification() {
        NotificationManagerCompat.from(context).cancel(config.notification.toolbarNotificationId)
        Log.d(RetentionLog.TAG, "Toolbar notification cancelled.")
    }

    fun showReminderNotification(item: ReminderItemConfig, trigger: RetentionTriggerType) {
        ensureReminderChannel()
        val notification = buildReminderNotification(item, trigger)
        NotificationManagerCompat.from(context).notify(
            config.notification.reminderNotificationBaseId + item.bucketId,
            notification,
        )
        Log.d(
            RetentionLog.TAG,
            "Reminder notification posted. trigger=${trigger.extraValue}, itemId=${item.id}, bucketId=${item.bucketId}",
        )
    }

    private fun buildToolbarNotification(items: List<ToolbarItemConfig>): Notification {
        val collapsedView = buildToolbarRemoteViews(
            config.notification.toolbarCollapsedLayoutResId,
            items,
        )
        val expandedView = buildToolbarRemoteViews(
            config.notification.toolbarExpandedLayoutResId,
            items,
        )
        return NotificationCompat.Builder(context, config.notification.toolbarChannelId)
            .setSmallIcon(config.notification.smallIconResId)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()
    }

    private fun buildToolbarRemoteViews(
        layoutResId: Int,
        items: List<ToolbarItemConfig>,
    ): RemoteViews {
        return RemoteViews(context.packageName, layoutResId).apply {
            bindToolbarItem(1, items.getOrNull(0))
            bindToolbarItem(2, items.getOrNull(1))
            bindToolbarItem(3, items.getOrNull(2))
            bindToolbarItem(4, items.getOrNull(3))
        }
    }

    private fun RemoteViews.bindToolbarItem(slot: Int, item: ToolbarItemConfig?) {
        val containerId = findId("retention_toolbar_item_$slot")
        val iconId = findId("retention_toolbar_icon_$slot")
        val textId = findId("retention_toolbar_text_$slot")
        if (containerId == 0) {
            return
        }
        if (item == null) {
            setViewVisibility(containerId, View.INVISIBLE)
            return
        }
        setViewVisibility(containerId, View.VISIBLE)
        if (iconId != 0) {
            setImageViewResource(iconId, item.iconResId)
        }
        if (textId != 0) {
            setTextViewText(textId, context.getString(item.titleResId))
        }
        createPendingIntent(item.target, null)?.let {
            setOnClickPendingIntent(containerId, it)
        }
    }

    private fun buildReminderNotification(
        item: ReminderItemConfig,
        trigger: RetentionTriggerType,
    ): Notification {
        val clickIntent = createPendingIntent(item.target, trigger)
        val actionLabel = context.getString(item.actionLabelResId)
        val message = context.getString(item.messageResId)
        val expandedView = buildReminderRemoteViews(
            config.notification.reminderExpandedLayoutResId,
            item,
            message,
            actionLabel,
            clickIntent,
        )
        val builder = NotificationCompat.Builder(context, config.notification.reminderChannelId)
            .setSmallIcon(config.notification.smallIconResId)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentTitle(actionLabel)
            .setContentText(message)
        applyReminderLayouts(
            builder = builder,
            item = item,
            message = message,
            actionLabel = actionLabel,
            clickIntent = clickIntent,
            expandedView = expandedView,
        )
        if (clickIntent != null) {
            builder.setContentIntent(clickIntent)
            builder.addAction(config.notification.smallIconResId, actionLabel, clickIntent)
        }
        return builder.build()
    }

    private fun applyReminderLayouts(
        builder: NotificationCompat.Builder,
        item: ReminderItemConfig,
        message: String,
        actionLabel: String,
        clickIntent: PendingIntent?,
        expandedView: RemoteViews,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val tinyView = buildReminderRemoteViews(
                config.notification.reminderTinyLayoutResId,
                item,
                message,
                actionLabel,
                clickIntent,
            )
            builder.setCustomContentView(tinyView)
            builder.setCustomHeadsUpContentView(tinyView)
            builder.setCustomBigContentView(expandedView)
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            return
        }
        if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) {
            val middleView = buildReminderRemoteViews(
                config.notification.reminderMiddleLayoutResId,
                item,
                message,
                actionLabel,
                clickIntent,
            )
            builder.setCustomContentView(middleView)
            builder.setCustomHeadsUpContentView(middleView)
            builder.setCustomBigContentView(expandedView)
            return
        }
        builder.setCustomContentView(expandedView)
        builder.setCustomHeadsUpContentView(expandedView)
        builder.setCustomBigContentView(expandedView)
    }

    private fun buildReminderRemoteViews(
        layoutResId: Int,
        item: ReminderItemConfig,
        message: String,
        actionLabel: String,
        clickIntent: PendingIntent?,
    ): RemoteViews {
        return RemoteViews(context.packageName, layoutResId).apply {
            val rootId = findId("retention_reminder_root")
            val imageId = findId("retention_reminder_image")
            val titleId = findId("retention_reminder_title")
            val messageId = findId("retention_reminder_message")
            val actionId = findId("retention_reminder_action")
            if (imageId != 0) {
                setImageViewResource(imageId, item.largeIconResId)
            }
            if (titleId != 0) {
                setTextViewText(titleId, actionLabel)
            }
            if (messageId != 0) {
                setTextViewText(messageId, message)
            }
            if (actionId != 0) {
                setTextViewText(actionId, actionLabel)
            }
            if (clickIntent != null) {
                if (rootId != 0) {
                    setOnClickPendingIntent(rootId, clickIntent)
                }
                if (actionId != 0) {
                    setOnClickPendingIntent(actionId, clickIntent)
                }
            }
        }
    }

    private fun createPendingIntent(
        target: LaunchTarget,
        trigger: RetentionTriggerType?,
    ): PendingIntent? {
        val intent = createLaunchIntent(target, trigger) ?: return null
        return PendingIntent.getActivity(
            context,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createLaunchIntent(
        target: LaunchTarget,
        trigger: RetentionTriggerType?,
    ): Intent? {
        val baseIntent = createExplicitIntent(target.activityClassName)
            ?: context.packageManager.getLaunchIntentForPackage(context.packageName)?.also {
                Log.d(RetentionLog.TAG, "Falling back to launcher intent for target=${target.id}")
            }
            ?: return null
        return baseIntent.apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            putExtra(EXTRA_RETENTION_ENTRY, target.id)
            putExtra(EXTRA_RETENTION_SOURCE, RETENTION_SOURCE)
            if (trigger != null) {
                putExtra(EXTRA_RETENTION_TRIGGER, trigger.extraValue)
            }
            putExtras(target.extras)
        }
    }

    private fun createExplicitIntent(activityClassName: String?): Intent? {
        if (activityClassName.isNullOrBlank()) {
            Log.d(RetentionLog.TAG, "No explicit activity configured, will use launcher fallback.")
            return null
        }
        return Intent().setClassName(context.packageName, activityClassName).takeIf {
            it.resolveActivity(context.packageManager) != null
        } ?: run {
            Log.w(RetentionLog.TAG, "Configured activityClass is invalid: $activityClassName")
            null
        }
    }

    private fun ensureChannel(
        channelId: String,
        channelName: String,
        importance: Int,
        withSound: Boolean,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            if (!withSound) {
                setSound(null, null)
            }
            enableVibration(withSound)
            enableLights(withSound)
            setShowBadge(withSound)
        }
        manager.createNotificationChannel(channel)
        Log.d(
            RetentionLog.TAG,
            "Notification channel ensured. channelId=$channelId importance=$importance withSound=$withSound",
        )
    }

    private fun findId(name: String): Int {
        return context.resources.getIdentifier(name, "id", context.packageName)
    }

    companion object {
        const val EXTRA_RETENTION_ENTRY = "retention_entry"
        const val EXTRA_RETENTION_SOURCE = "retention_source"
        const val EXTRA_RETENTION_TRIGGER = "retention_trigger"
        const val RETENTION_SOURCE = "library-retention"
    }
}
