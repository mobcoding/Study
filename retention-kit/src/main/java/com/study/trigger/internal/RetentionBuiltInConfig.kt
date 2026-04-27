package com.study.trigger.internal

import android.app.Application
import android.content.Context
import android.content.Intent
import com.study.trigger.AlarmPolicy
import com.study.trigger.NavigationTarget
import com.study.trigger.R
import com.study.trigger.ReminderPolicy
import com.study.trigger.RetentionAnalyticsReporter
import com.study.trigger.RetentionBehaviorConfig
import com.study.trigger.RetentionConfig
import com.study.trigger.RetentionContentProvider
import com.study.trigger.RetentionModule
import com.study.trigger.RetentionNavigationIntentFactory
import com.study.trigger.RetentionNotificationConfig
import com.study.trigger.RetentionStrategyConfig
import com.study.trigger.ToolbarItem
import com.study.trigger.QuietHours
import java.util.Locale

internal object RetentionBuiltInConfigFactory {

    fun create(application: Application): RetentionConfig {
        val behaviorConfig = RetentionBehaviorConfig(
            specialMediaModeEnabled = false,
            fileRefererReadBlocked = false,
        )
        val contentProvider = BuiltInContentProvider(application)
        return RetentionConfig(
            notificationConfig = RetentionNotificationConfig(
                smallIconResId = R.drawable.pdf_notification_small_icon,
                reminderChannelId = "scan_reminder",
                reminderChannelName = "scan_reminder",
                toolbarChannelId = "scan_nav",
                toolbarChannelName = "scan_nav",
                bucketOrder = RetentionStaticContent.bucketOrder(behaviorConfig),
            ),
            strategyConfig = RetentionStrategyConfig(
                standardRemindersEnabled = true,
                mediaStyleEnabled = true,
                blockedCountryCodes = setOf("KR"),
                quietHours = QuietHours(startHourInclusive = 0, endHourExclusive = 8),
                timerPolicy = ReminderPolicy(intervalMinutes = 20, dailyLimit = 30),
                unlockPolicy = ReminderPolicy(intervalMinutes = 10, dailyLimit = 40),
                mediaPolicy = ReminderPolicy(intervalMinutes = 40, dailyLimit = 30),
                alarmPolicy = AlarmPolicy(enabled = true, intervalMinutes = 50),
            ),
            behaviorConfig = behaviorConfig,
            contentProvider = contentProvider,
            navigationIntentFactory = RetentionNavigationIntentFactory(::createLaunchIntent),
            analyticsReporter = RetentionAnalyticsReporter { _, _ -> },
        )
    }

    private fun createLaunchIntent(context: Context, target: NavigationTarget): Intent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        return launchIntent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(RetentionModule.EXTRA_RETENTION_ENTRY, target.id)
            putExtras(target.extras)
        }
    }

    private class BuiltInContentProvider(
        private val application: Application,
    ) : RetentionContentProvider {

        override fun getToolbarItems(locale: Locale): List<ToolbarItem> {
            return RetentionStaticContent.buildToolbarItems(application, locale)
        }

        override fun getReminderContents(locale: Locale) =
            RetentionStaticContent.buildReminderContents(locale)
    }
}
