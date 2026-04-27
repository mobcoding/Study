package com.study.retention.internal

import android.os.Bundle
import com.google.gson.JsonObject

internal enum class RetentionTriggerType(val policyKey: String, val extraValue: String) {
    TIMER("timer", "timer"),
    UNLOCK("unlock", "unlock"),
    ALARM("alarm", "alarm"),
    BOOT("boot", "boot"),
    APP_BACKGROUND("app_background", "app_background"),
    SCREEN_OFF("screen_off", "screen_off"),
    PACKAGE_REPLACED("package_replaced", "package_replaced"),
}

internal data class RetentionRuntimeConfig(
    val notification: RetentionNotificationSection,
    val policy: RetentionPolicySection,
    val toolbar: RetentionToolbarSection,
    val reminders: RetentionReminderSection,
)

internal data class RetentionNotificationSection(
    val smallIconResId: Int,
    val toolbarChannelId: String,
    val toolbarChannelNameResId: Int,
    val reminderChannelId: String,
    val reminderChannelNameResId: Int,
    val toolbarNotificationId: Int,
    val reminderNotificationBaseId: Int,
    val toolbarCollapsedLayoutResId: Int,
    val toolbarExpandedLayoutResId: Int,
    val reminderTinyLayoutResId: Int,
    val reminderMiddleLayoutResId: Int,
    val reminderExpandedLayoutResId: Int,
)

internal data class RetentionPolicySection(
    val quietHours: QuietHours?,
    val globalCooldown: GlobalCooldownPolicy,
    val timer: TriggerPolicy,
    val unlock: TriggerPolicy,
    val alarm: TriggerPolicy,
    val boot: TriggerPolicy,
    val appBackground: TriggerPolicy,
    val screenOff: TriggerPolicy,
    val packageReplaced: TriggerPolicy,
)

internal data class GlobalCooldownPolicy(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 0,
)

internal data class TriggerPolicy(
    val enabled: Boolean = true,
    val intervalMinutes: Int,
    val dailyLimit: Int,
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

internal data class RetentionToolbarSection(
    val enabled: Boolean,
    val items: List<ToolbarItemConfig>,
)

internal data class ToolbarItemConfig(
    val id: String,
    val titleResId: Int,
    val iconResId: Int,
    val target: LaunchTarget,
)

internal data class RetentionReminderSection(
    val bucketOrder: List<Int>,
    val items: List<ReminderItemConfig>,
)

internal data class ReminderItemConfig(
    val id: String,
    val bucketId: Int,
    val messageResId: Int,
    val actionLabelResId: Int,
    val largeIconResId: Int,
    val target: LaunchTarget,
)

internal data class LaunchTarget(
    val id: String,
    val activityClassName: String?,
    val extras: Bundle,
)

internal data class RawRetentionConfig(
    val version: Int = 1,
    val notification: RawNotificationSection? = null,
    val policy: RawPolicySection? = null,
    val toolbar: RawToolbarSection? = null,
    val reminders: RawReminderSection? = null,
)

internal data class RawNotificationSection(
    val smallIcon: String? = null,
    val toolbarChannelId: String? = null,
    val toolbarChannelNameRes: String? = null,
    val reminderChannelId: String? = null,
    val reminderChannelNameRes: String? = null,
    val toolbarNotificationId: Int? = null,
    val reminderNotificationBaseId: Int? = null,
    val toolbarCollapsedLayout: String? = null,
    val toolbarExpandedLayout: String? = null,
    val reminderTinyLayout: String? = null,
    val reminderMiddleLayout: String? = null,
    val reminderExpandedLayout: String? = null,
    val reminderCollapsedLayout: String? = null,
)

internal data class RawPolicySection(
    val quietHours: RawQuietHours? = null,
    val globalCooldown: RawGlobalCooldownPolicy? = null,
    val timer: RawTriggerPolicy? = null,
    val unlock: RawTriggerPolicy? = null,
    val alarm: RawTriggerPolicy? = null,
    val boot: RawTriggerPolicy? = null,
    val appBackground: RawTriggerPolicy? = null,
    val screenOff: RawTriggerPolicy? = null,
    val packageReplaced: RawTriggerPolicy? = null,
)

internal data class RawQuietHours(
    val startHourInclusive: Int? = null,
    val endHourExclusive: Int? = null,
)

internal data class RawTriggerPolicy(
    val enabled: Boolean? = null,
    val intervalMinutes: Int? = null,
    val dailyLimit: Int? = null,
)

internal data class RawGlobalCooldownPolicy(
    val enabled: Boolean? = null,
    val intervalMinutes: Int? = null,
)

internal data class RawToolbarSection(
    val enabled: Boolean? = null,
    val items: List<RawToolbarItem>? = null,
)

internal data class RawToolbarItem(
    val id: String? = null,
    val titleRes: String? = null,
    val icon: String? = null,
    val activityClass: String? = null,
    val extras: JsonObject? = null,
)

internal data class RawReminderSection(
    val bucketOrder: List<Int>? = null,
    val items: List<RawReminderItem>? = null,
)

internal data class RawReminderItem(
    val id: String? = null,
    val bucketId: Int? = null,
    val messageRes: String? = null,
    val actionLabelRes: String? = null,
    val largeIcon: String? = null,
    val activityClass: String? = null,
    val extras: JsonObject? = null,
)
