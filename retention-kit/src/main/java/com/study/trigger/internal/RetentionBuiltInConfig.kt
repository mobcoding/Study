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
                // 普通提醒通知开关，控制的是标准提醒样式，不包含常驻 toolbar 通知。
                standardRemindersEnabled = true,
                // 是否允许媒体样式通知参与调度。
                // 实际是否能展示，还要同时满足 behaviorConfig.specialMediaModeEnabled。
                mediaStyleEnabled = true,
                // 按国家/地区屏蔽留存通知，命中这些国家码时不展示。
                blockedCountryCodes = setOf("KR"),
                // 预留的静默时段配置：0 点到 8 点。
                // 当前配置已定义，但现有 dispatcher 逻辑里还没有真正执行这段限制。
                quietHours = QuietHours(startHourInclusive = 0, endHourExclusive = 8),
                // TIMER 触发频控：两次展示之间至少间隔 20 分钟，每天最多展示 30 次。
                timerPolicy = ReminderPolicy(intervalMinutes = 20, dailyLimit = 30),
                // UNLOCK 触发频控：两次展示之间至少间隔 10 分钟，每天最多展示 40 次。
                unlockPolicy = ReminderPolicy(intervalMinutes = 1, dailyLimit = 40),
                // MEDIA 触发频控：两次展示之间至少间隔 40 分钟，每天最多展示 30 次。
                mediaPolicy = ReminderPolicy(intervalMinutes = 40, dailyLimit = 30),
                // 后台闹钟触发策略：每 50 分钟调度一次系统 Alarm。
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
