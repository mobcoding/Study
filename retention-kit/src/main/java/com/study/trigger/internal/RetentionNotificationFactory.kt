package com.study.trigger.internal

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.Settings
import android.support.v4.media.session.MediaSessionCompat
import android.text.Html
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.media.app.NotificationCompat.MediaStyle
import com.study.trigger.NavigationTarget
import com.study.trigger.R
import com.study.trigger.ReminderContent
import com.study.trigger.ReminderTrigger
import com.study.trigger.RetentionModule
import com.study.trigger.ToolbarItem
import kotlin.random.Random

internal object RetentionNotificationFactory {

    fun ensureReminderChannel() {
        val config = RetentionModule.currentConfig().notificationConfig
        ensureChannel(
            context = RetentionModule.app(),
            channelId = config.reminderChannelId,
            channelName = config.reminderChannelName,
            importance = NotificationManager.IMPORTANCE_HIGH,
            withSound = true,
            showBadge = true,
        )
    }

    fun ensureToolbarChannel() {
        val config = RetentionModule.currentConfig().notificationConfig
        ensureChannel(
            context = RetentionModule.app(),
            channelId = config.toolbarChannelId,
            channelName = config.toolbarChannelName,
            importance = if (Build.VERSION.SDK_INT > 35 && Build.MANUFACTURER.equals(
                    "Google",
                    ignoreCase = true
                )
            ) {
                NotificationManager.IMPORTANCE_MIN
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            },
            withSound = false,
            showBadge = false,
        )
    }

    @SuppressLint("MissingPermission")
    fun showToolbarNotification(toolbarItems: List<ToolbarItem>): Notification {
        val notification = buildToolbarNotification(toolbarItems)
        try {
            NotificationManagerCompat.from(RetentionModule.app()).notify(
                RetentionModule.currentConfig().notificationConfig.toolbarNotificationId,
                notification,
            )
        } catch (e: Exception) {
        }
        RetentionReport.report(
            "toolbar_notification_shown",
            bundleOf("has_permission" to RetentionDispatcher.hasNotificationPermission())
        )
        return notification
    }

    fun buildToolbarNotification(toolbarItems: List<ToolbarItem>): Notification {
        val context = RetentionModule.app()
        val config = RetentionModule.currentConfig().notificationConfig
        ensureToolbarChannel()
        val standardRemoteViews = buildToolbarRemoteViews(R.layout.layout_toolbar, toolbarItems)
        val builder = NotificationCompat.Builder(context, config.toolbarChannelId)
            .setSmallIcon(config.smallIconResId)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setCustomContentView(
                buildToolbarRemoteViews(
                    R.layout.layout_toolbar_tiny,
                    toolbarItems
                )
            )
            builder.setCustomBigContentView(standardRemoteViews)
            builder.setCustomHeadsUpContentView(standardRemoteViews)
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        } else {
            builder.setCustomContentView(standardRemoteViews)
            builder.setCustomBigContentView(standardRemoteViews)
            builder.setCustomHeadsUpContentView(standardRemoteViews)
        }
        return builder.build()
    }

    fun buildStandardReminderNotification(
        content: ReminderContent,
        trigger: ReminderTrigger
    ): Notification {
        val context = RetentionModule.app()
        val config = RetentionModule.currentConfig().notificationConfig
        ensureReminderChannel()
        val pendingIntent = createReminderPendingIntent(content.target, trigger)
        val builder = NotificationCompat.Builder(context, config.reminderChannelId)
            .setSmallIcon(config.smallIconResId)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setGroup("important")
            .setContentTitle(content.actionLabel)
            .setContentText(content.message)
            .setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= 31) {
            val compactRemoteViews =
                buildReminderRemoteViews(R.layout.layout_reminder_tiny, content, pendingIntent)
            val expandedRemoteViews =
                buildReminderRemoteViews(R.layout.layout_reminder_large, content, pendingIntent)
            builder.setCustomContentView(compactRemoteViews)
            builder.setCustomHeadsUpContentView(compactRemoteViews)
            builder.setCustomBigContentView(expandedRemoteViews)
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        } else {
            val defaultRemoteViews =
                buildReminderRemoteViews(R.layout.layout_reminder_large, content, pendingIntent)
            if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) {
                val collapsedRemoteViews = buildReminderRemoteViews(
                    R.layout.layout_reminder_middle,
                    content,
                    pendingIntent
                )
                builder.setCustomContentView(collapsedRemoteViews)
                builder.setCustomHeadsUpContentView(collapsedRemoteViews)
                builder.setCustomBigContentView(defaultRemoteViews)
            } else {
                builder.setCustomContentView(defaultRemoteViews)
                builder.setCustomHeadsUpContentView(defaultRemoteViews)
                builder.setCustomBigContentView(defaultRemoteViews)
            }
        }
        return builder.build()
    }

    fun buildMediaReminderNotification(
        content: ReminderContent,
        trigger: ReminderTrigger
    ): Notification {
        val context = RetentionModule.app()
        val config = RetentionModule.currentConfig().notificationConfig
        ensureReminderChannel()
        val mediaSession = MediaSessionCompat(context, "PhotoMediaRecoverySession").apply {
            isActive = true
        }
        return NotificationCompat.Builder(context, config.reminderChannelId)
            .setSmallIcon(config.smallIconResId)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup("important")
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, content.iconResId))
            .setContentTitle(content.actionLabel)
            .setContentText(content.message)
            .setContentIntent(createMediaReminderPendingIntent(content.target, trigger))
            .setStyle(MediaStyle().setMediaSession(mediaSession.sessionToken))
            .build()
    }

    private fun buildReminderRemoteViews(
        layoutResId: Int,
        content: ReminderContent,
        pendingIntent: PendingIntent,
    ): RemoteViews {
        return RemoteViews(RetentionModule.app().packageName, layoutResId).apply {
            setImageViewResource(R.id.image_reminder, content.iconResId)
            setTextViewText(R.id.text_content, Html.fromHtml(content.message))
            setTextViewText(R.id.text_button, Html.fromHtml(content.actionLabel))
            setOnClickPendingIntent(R.id.layout_reminder_root, pendingIntent)
            setOnClickPendingIntent(R.id.layout_reminder_action, pendingIntent)
        }
    }

    private fun buildToolbarRemoteViews(
        layoutResId: Int,
        toolbarItems: List<ToolbarItem>
    ): RemoteViews {
        val remoteViews = RemoteViews(RetentionModule.app().packageName, layoutResId)
        bindToolbarItem(
            remoteViews,
            R.id.image_toolbar_1,
            R.id.text_toolbar_1,
            R.id.layout_toolbar_1,
            toolbarItems.getOrNull(0)
        )
        bindToolbarItem(
            remoteViews,
            R.id.image_toolbar_2,
            R.id.text_toolbar_2,
            R.id.layout_toolbar_2,
            toolbarItems.getOrNull(1)
        )
        bindToolbarItem(
            remoteViews,
            R.id.image_toolbar_3,
            R.id.text_toolbar_3,
            R.id.layout_toolbar_3,
            toolbarItems.getOrNull(2)
        )
        bindToolbarItem(
            remoteViews,
            R.id.image_toolbar_4,
            R.id.text_toolbar_4,
            R.id.layout_toolbar_4,
            toolbarItems.getOrNull(3)
        )
        return remoteViews
    }

    private fun bindToolbarItem(
        remoteViews: RemoteViews,
        imageViewId: Int,
        textViewId: Int,
        containerViewId: Int,
        item: ToolbarItem?,
    ) {
        val target = item?.target ?: return
        remoteViews.setImageViewResource(imageViewId, item.iconResId)
        remoteViews.setTextViewText(textViewId, item.title)
        remoteViews.setOnClickPendingIntent(containerViewId, createToolbarPendingIntent(target))
    }

    private fun createReminderPendingIntent(
        target: NavigationTarget,
        trigger: ReminderTrigger
    ): PendingIntent {
        val intent = createBaseNavigationIntent(target).apply {
            putExtra(RetentionModule.EXTRA_LEGACY_REMINDER_TYPE, trigger.ordinal)
        }
        return PendingIntent.getActivity(
            RetentionModule.app(),
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createMediaReminderPendingIntent(
        target: NavigationTarget,
        trigger: ReminderTrigger
    ): PendingIntent {
        val intent = createBaseNavigationIntent(target).apply {
            putExtra(RetentionModule.EXTRA_LEGACY_ORIGIN_REMINDER_TYPE, trigger.ordinal)
            putExtra(RetentionModule.EXTRA_LEGACY_REMINDER_TYPE, 3)
        }
        return PendingIntent.getActivity(
            RetentionModule.app(),
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createToolbarPendingIntent(target: NavigationTarget): PendingIntent {
        return PendingIntent.getActivity(
            RetentionModule.app(),
            Random.nextInt(),
            createBaseNavigationIntent(target),
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    internal fun resolveStandardNotificationId(bucketId: Int): Int {
        return if (Build.VERSION.SDK_INT > 35 && Build.MANUFACTURER.equals(
                "Google",
                ignoreCase = true
            )
        ) {
            GOOGLE_DEVICE_REMINDER_NOTIFICATION_ID
        } else {
            bucketId
        }
    }

    private fun createBaseNavigationIntent(target: NavigationTarget): Intent {
        return RetentionModule.currentConfig().navigationIntentFactory.createIntent(
            RetentionModule.app(),
            target
        ).apply {
            addFlags(NAVIGATION_FLAGS)
            putExtras(target.extras)
        }
    }

    private fun ensureChannel(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int,
        withSound: Boolean,
        showBadge: Boolean,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            setShowBadge(showBadge)
            enableLights(withSound)
            enableVibration(withSound)
            if (withSound) {
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    Notification.AUDIO_ATTRIBUTES_DEFAULT
                )
            } else {
                setSound(null, null)
            }
        }
        manager.createNotificationChannel(channel)
    }

    private const val GOOGLE_DEVICE_REMINDER_NOTIFICATION_ID = 401298
    private const val NAVIGATION_FLAGS =
        Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
}
