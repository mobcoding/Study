package com.study.retention.internal

import android.os.Bundle
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

internal enum class RetentionTriggerType(val policyKey: String, val extraValue: String) {
    TIMER("timer", "timer"),
    UNLOCK("unlock", "unlock"),
    ALARM("alarm", "alarm"),
    BOOT("boot", "boot"),
    APP_BACKGROUND("app_background", "app_background"),
    SCREEN_OFF("screen_off", "screen_off"),
    PACKAGE_REPLACED("package_replaced", "package_replaced"),
    CHARGING("charging", "charging"),
}

internal data class RetentionRuntimeConfig(
    val notification: RetentionNotificationSection,
    val runtime: RetentionRuntimeSection,
    val policy: RetentionPolicySection,
    val toolbar: RetentionToolbarSection,
    val reminders: RetentionReminderSection,
)

internal data class RetentionRuntimeSection(
    val workManagerEnabled: Boolean,
    val heartbeatIntervalMinutes: Int,
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
    val charging: TriggerPolicy,
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
    val primaryTarget: LaunchTarget?,
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
    val imageResId: Int,
    val collapsedPreviewImageResId: Int? = null,
    val expandedImageResId: Int? = null,
    val target: LaunchTarget,
)

internal data class LaunchTarget(
    val id: String,
    val activityClassName: String?,
    val extras: Bundle,
)

internal data class RawRetentionConfig(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("notification") val notification: RawNotificationSection? = null,
    @SerializedName("runtime") val runtime: RawRuntimeSection? = null,
    @SerializedName("policy") val policy: RawPolicySection? = null,
    @SerializedName("toolbar") val toolbar: RawToolbarSection? = null,
    @SerializedName("reminders") val reminders: RawReminderSection? = null,
)

internal data class RawRuntimeSection(
    @SerializedName("workManagerEnabled") val workManagerEnabled: Boolean? = null,
    @SerializedName("heartbeatIntervalMinutes") val heartbeatIntervalMinutes: Int? = null,
)

internal data class RawNotificationSection(
    @SerializedName("smallIcon") val smallIcon: String? = null,
    @SerializedName("toolbarChannelId") val toolbarChannelId: String? = null,
    @SerializedName("toolbarChannelNameRes") val toolbarChannelNameRes: String? = null,
    @SerializedName("reminderChannelId") val reminderChannelId: String? = null,
    @SerializedName("reminderChannelNameRes") val reminderChannelNameRes: String? = null,
    @SerializedName("toolbarNotificationId") val toolbarNotificationId: Int? = null,
    @SerializedName("reminderNotificationBaseId") val reminderNotificationBaseId: Int? = null,
    @SerializedName("toolbarCollapsedLayout") val toolbarCollapsedLayout: String? = null,
    @SerializedName("toolbarExpandedLayout") val toolbarExpandedLayout: String? = null,
    @SerializedName("reminderTinyLayout") val reminderTinyLayout: String? = null,
    @SerializedName("reminderMiddleLayout") val reminderMiddleLayout: String? = null,
    @SerializedName("reminderExpandedLayout") val reminderExpandedLayout: String? = null,
    @SerializedName("reminderCollapsedLayout") val reminderCollapsedLayout: String? = null,
)

internal data class RawPolicySection(
    @SerializedName("quietHours") val quietHours: RawQuietHours? = null,
    @SerializedName("globalCooldown") val globalCooldown: RawGlobalCooldownPolicy? = null,
    @SerializedName("timer") val timer: RawTriggerPolicy? = null,
    @SerializedName("unlock") val unlock: RawTriggerPolicy? = null,
    @SerializedName("alarm") val alarm: RawTriggerPolicy? = null,
    @SerializedName("boot") val boot: RawTriggerPolicy? = null,
    @SerializedName("appBackground") val appBackground: RawTriggerPolicy? = null,
    @SerializedName("screenOff") val screenOff: RawTriggerPolicy? = null,
    @SerializedName("packageReplaced") val packageReplaced: RawTriggerPolicy? = null,
    @SerializedName("charging") val charging: RawTriggerPolicy? = null,
)

internal data class RawQuietHours(
    @SerializedName("startHourInclusive") val startHourInclusive: Int? = null,
    @SerializedName("endHourExclusive") val endHourExclusive: Int? = null,
)

internal data class RawTriggerPolicy(
    @SerializedName("enabled") val enabled: Boolean? = null,
    @SerializedName("intervalMinutes") val intervalMinutes: Int? = null,
    @SerializedName("dailyLimit") val dailyLimit: Int? = null,
)

internal data class RawGlobalCooldownPolicy(
    @SerializedName("enabled") val enabled: Boolean? = null,
    @SerializedName("intervalMinutes") val intervalMinutes: Int? = null,
)

internal data class RawToolbarSection(
    @SerializedName("enabled") val enabled: Boolean? = null,
    @SerializedName("items") val items: List<RawToolbarItem>? = null,
    @SerializedName("primaryTarget") val primaryTarget: RawLaunchTarget? = null,
)

internal data class RawLaunchTarget(
    @SerializedName("id") val id: String? = null,
    @SerializedName("activityClass") val activityClass: String? = null,
    @SerializedName("extras") val extras: JsonObject? = null,
)

internal data class RawToolbarItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("titleRes") val titleRes: String? = null,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("activityClass") val activityClass: String? = null,
    @SerializedName("extras") val extras: JsonObject? = null,
)

internal data class RawReminderSection(
    @SerializedName("bucketOrder") val bucketOrder: List<Int>? = null,
    @SerializedName("items") val items: List<RawReminderItem>? = null,
)

internal data class RawReminderItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("bucketId") val bucketId: Int? = null,
    @SerializedName("messageRes") val messageRes: String? = null,
    @SerializedName("actionLabelRes") val actionLabelRes: String? = null,
    @SerializedName("largeIcon") val largeIcon: String? = null,
    @SerializedName("smallImage") val smallImage: String? = null,
    @SerializedName("expandedImage") val expandedImage: String? = null,
    @SerializedName("activityClass") val activityClass: String? = null,
    @SerializedName("extras") val extras: JsonObject? = null,
)
