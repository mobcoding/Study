package com.study.trigger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import java.util.Locale

enum class ReminderTrigger {
    TIMER,
    UNLOCK,
    ALARM,
    MEDIA,
}

internal data class ReminderPolicy(
    val enabled: Boolean = true,
    val intervalMinutes: Int,
    val dailyLimit: Int,
)

internal data class AlarmPolicy(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 50,
)

internal data class QuietHours(
    val startHourInclusive: Int,
    val endHourExclusive: Int,
) {
    fun containsHour(hour: Int): Boolean {
        if (startHourInclusive == endHourExclusive) {
            return false
        }
        return if (startHourInclusive < endHourExclusive) {
            hour in startHourInclusive until endHourExclusive
        } else {
            hour >= startHourInclusive || hour < endHourExclusive
        }
    }
}

internal data class RetentionStrategyConfig(
    val standardRemindersEnabled: Boolean = true,
    val mediaStyleEnabled: Boolean = true,
    val blockedCountryCodes: Set<String> = setOf("KR"),
    val quietHours: QuietHours? = null,
    val timerPolicy: ReminderPolicy = ReminderPolicy(intervalMinutes = 20, dailyLimit = 30),
    val unlockPolicy: ReminderPolicy = ReminderPolicy(intervalMinutes = 10, dailyLimit = 40),
    val mediaPolicy: ReminderPolicy = ReminderPolicy(intervalMinutes = 40, dailyLimit = 30),
    val alarmPolicy: AlarmPolicy = AlarmPolicy(),
)

internal data class RetentionNotificationConfig(
    val smallIconResId: Int,
    val reminderChannelId: String = "retention_reminder",
    val reminderChannelName: String = "Retention Reminder",
    val toolbarChannelId: String = "retention_toolbar",
    val toolbarChannelName: String = "Retention Toolbar",
    val mediaStyleNotificationId: Int = 100089,
    val toolbarNotificationId: Int = 105031,
    val bucketOrder: List<Int> = listOf(500020, 500030, 500001, 500002, 500004),
)

internal data class RetentionBehaviorConfig(
    val specialMediaModeEnabled: Boolean = true,
    val fileRefererReadBlocked: Boolean = false,
)

internal data class NavigationTarget(
    val id: String,
    val extras: Bundle = Bundle(),
)

internal data class ToolbarItem(
    val iconResId: Int,
    val title: String,
    val target: NavigationTarget,
)

internal data class ReminderContent(
    val bucketId: Int,
    val iconResId: Int,
    val message: String,
    val actionLabel: String,
    val target: NavigationTarget,
)

internal fun interface RetentionNavigationIntentFactory {
    fun createIntent(context: Context, target: NavigationTarget): Intent
}

internal fun interface RetentionAnalyticsReporter {
    fun report(eventName: String, properties: Map<String, String>)
}

internal interface RetentionContentProvider {
    fun getToolbarItems(locale: Locale): List<ToolbarItem>

    fun getReminderContents(locale: Locale): List<ReminderContent>
}

internal data class RetentionConfig(
    val notificationConfig: RetentionNotificationConfig,
    val strategyConfig: RetentionStrategyConfig = RetentionStrategyConfig(),
    val behaviorConfig: RetentionBehaviorConfig = RetentionBehaviorConfig(),
    val contentProvider: RetentionContentProvider,
    val navigationIntentFactory: RetentionNavigationIntentFactory,
    val analyticsReporter: RetentionAnalyticsReporter = RetentionAnalyticsReporter { _, _ -> },
    val excludedForegroundActivityKeywords: Set<String> = RetentionDefaults.excludedForegroundActivityKeywords,
    val enableSessionPingLog: Boolean = false,
)

internal object RetentionDefaults {
    val excludedForegroundActivityKeywords: Set<String> = linkedSetOf(
        "AdActivity",
        "Interstitial",
        "Rewarded",
        "FullscreenAd",
        "UnityAds",
        "UnityPlayer",
        "IronSource",
        "AppLovin",
        "Vungle",
        "Chartboost",
        "AudienceNetwork",
        "Mintegral",
        "Pangle",
        "TT",
    )
}
