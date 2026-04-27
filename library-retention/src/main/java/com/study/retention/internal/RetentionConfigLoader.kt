package com.study.retention.internal

import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import java.io.InputStreamReader

internal object RetentionConfigLoader {

    private const val ASSET_FILE_NAME = "retention_config.json"

    fun load(application: Application): RetentionRuntimeConfig? {
        return runCatching {
            Log.d(RetentionLog.TAG, "Loading retention config from assets/$ASSET_FILE_NAME")
            application.assets.open(ASSET_FILE_NAME).use { input ->
                InputStreamReader(input).use { reader ->
                    parse(application, Gson().fromJson(reader, RawRetentionConfig::class.java))
                }
            }
        }.onFailure {
            Log.e(RetentionLog.TAG, "Failed to load $ASSET_FILE_NAME", it)
        }.getOrNull()
    }

    private fun parse(
        application: Application,
        rawConfig: RawRetentionConfig?
    ): RetentionRuntimeConfig? {
        if (rawConfig == null) {
            Log.e(RetentionLog.TAG, "Retention config JSON is empty.")
            return null
        }
        val rawNotification = rawConfig.notification ?: return null
        val reminderTinyLayoutName =
            rawNotification.reminderTinyLayout ?: rawNotification.reminderCollapsedLayout
        val reminderMiddleLayoutName =
            rawNotification.reminderMiddleLayout ?: rawNotification.reminderCollapsedLayout
        val reminderExpandedLayoutName =
            rawNotification.reminderExpandedLayout ?: rawNotification.reminderCollapsedLayout
        val notification = RetentionNotificationSection(
            smallIconResId = resolveIcon(application, rawNotification.smallIcon) ?: return null,
            toolbarChannelId = rawNotification.toolbarChannelId.orEmpty().ifBlank { "retention_toolbar" },
            toolbarChannelNameResId = resolveString(
                application,
                rawNotification.toolbarChannelNameRes,
            ) ?: return null,
            reminderChannelId = rawNotification.reminderChannelId.orEmpty().ifBlank { "retention_reminder" },
            reminderChannelNameResId = resolveString(
                application,
                rawNotification.reminderChannelNameRes,
            ) ?: return null,
            toolbarNotificationId = rawNotification.toolbarNotificationId ?: 105_031,
            reminderNotificationBaseId = rawNotification.reminderNotificationBaseId ?: 500_000,
            toolbarCollapsedLayoutResId = resolveLayout(
                application,
                rawNotification.toolbarCollapsedLayout,
            ) ?: return null,
            toolbarExpandedLayoutResId = resolveLayout(
                application,
                rawNotification.toolbarExpandedLayout,
            ) ?: return null,
            reminderTinyLayoutResId = resolveLayout(
                application,
                reminderTinyLayoutName,
            ) ?: return null,
            reminderMiddleLayoutResId = resolveLayout(
                application,
                reminderMiddleLayoutName,
            ) ?: return null,
            reminderExpandedLayoutResId = resolveLayout(
                application,
                reminderExpandedLayoutName,
            ) ?: return null,
        )
        val rawPolicy = rawConfig.policy ?: return null
        val policy = RetentionPolicySection(
            quietHours = rawPolicy.quietHours?.toQuietHours(),
            globalCooldown = rawPolicy.globalCooldown.toGlobalCooldownPolicy(),
            timer = rawPolicy.timer.toPolicy(defaultInterval = 20, defaultLimit = 30),
            unlock = rawPolicy.unlock.toPolicy(defaultInterval = 10, defaultLimit = 40),
            alarm = rawPolicy.alarm.toPolicy(defaultInterval = 50, defaultLimit = 30),
            boot = rawPolicy.boot.toPolicy(defaultInterval = 0, defaultLimit = 1, defaultEnabled = false),
            appBackground = rawPolicy.appBackground.toPolicy(defaultInterval = 30, defaultLimit = 10, defaultEnabled = false),
            screenOff = rawPolicy.screenOff.toPolicy(defaultInterval = 30, defaultLimit = 10, defaultEnabled = false),
            packageReplaced = rawPolicy.packageReplaced.toPolicy(defaultInterval = 0, defaultLimit = 1, defaultEnabled = false),
        )
        val toolbar = RetentionToolbarSection(
            enabled = rawConfig.toolbar?.enabled ?: true,
            items = rawConfig.toolbar?.items.orEmpty().mapNotNull { item ->
                val iconResId = resolveIcon(application, item.icon) ?: return@mapNotNull null
                val titleResId = resolveString(application, item.titleRes) ?: return@mapNotNull null
                val id = item.id.orEmpty()
                if (id.isBlank()) {
                    return@mapNotNull null
                }
                ToolbarItemConfig(
                    id = id,
                    titleResId = titleResId,
                    iconResId = iconResId,
                    target = LaunchTarget(
                        id = id,
                        activityClassName = item.activityClass,
                        extras = item.extras.toBundle(),
                    ),
                )
            },
        )
        val reminders = RetentionReminderSection(
            bucketOrder = rawConfig.reminders?.bucketOrder.orEmpty(),
            items = rawConfig.reminders?.items.orEmpty().mapNotNull { item ->
                val iconResId = resolveLargeIcon(application, item.largeIcon) ?: return@mapNotNull null
                val bucketId = item.bucketId ?: return@mapNotNull null
                val id = item.id.orEmpty()
                if (id.isBlank()) {
                    return@mapNotNull null
                }
                val messageResId = resolveString(application, item.messageRes) ?: return@mapNotNull null
                val actionLabelResId =
                    resolveString(application, item.actionLabelRes) ?: return@mapNotNull null
                if (id.isBlank()) {
                    return@mapNotNull null
                }
                ReminderItemConfig(
                    id = id,
                    bucketId = bucketId,
                    messageResId = messageResId,
                    actionLabelResId = actionLabelResId,
                    largeIconResId = iconResId,
                    target = LaunchTarget(
                        id = id,
                        activityClassName = item.activityClass,
                        extras = item.extras.toBundle(),
                    ),
                )
            },
        )
        val runtimeConfig = RetentionRuntimeConfig(
            notification = notification,
            policy = policy,
            toolbar = toolbar,
            reminders = reminders,
        )
        Log.d(
            RetentionLog.TAG,
            "Retention config parsed. toolbarEnabled=${toolbar.enabled}, toolbarItems=${toolbar.items.size}, reminderItems=${reminders.items.size}",
        )
        return runtimeConfig
    }

    private fun RawTriggerPolicy?.toPolicy(
        defaultInterval: Int,
        defaultLimit: Int,
        defaultEnabled: Boolean = true,
    ): TriggerPolicy {
        return TriggerPolicy(
            enabled = this?.enabled ?: defaultEnabled,
            intervalMinutes = (this?.intervalMinutes ?: defaultInterval).coerceAtLeast(0),
            dailyLimit = (this?.dailyLimit ?: defaultLimit).coerceAtLeast(0),
        )
    }

    private fun RawGlobalCooldownPolicy?.toGlobalCooldownPolicy(): GlobalCooldownPolicy {
        return GlobalCooldownPolicy(
            enabled = this?.enabled ?: false,
            intervalMinutes = (this?.intervalMinutes ?: 0).coerceAtLeast(0),
        )
    }

    private fun RawQuietHours.toQuietHours(): QuietHours? {
        val start = startHourInclusive ?: return null
        val end = endHourExclusive ?: return null
        if (start !in 0..23 || end !in 0..23) {
            return null
        }
        return QuietHours(start, end)
    }

    private fun resolveIcon(application: Application, name: String?): Int? {
        if (name.isNullOrBlank()) {
            return null
        }
        return resolveResource(application, name, "drawable")
            ?: resolveResource(application, name, "mipmap")
            ?: run {
                Log.e(RetentionLog.TAG, "Unable to resolve icon resource: $name")
                null
            }
    }

    private fun resolveLargeIcon(application: Application, name: String?): Int? {
        if (name.isNullOrBlank()) {
            return null
        }
        return resolveResource(application, name, "drawable")
            ?: resolveResource(application, name, "mipmap")
            ?: run {
                Log.e(RetentionLog.TAG, "Unable to resolve large icon resource: $name")
                null
            }
    }

    private fun resolveString(application: Application, name: String?): Int? {
        if (name.isNullOrBlank()) {
            return null
        }
        return resolveResource(application, name, "string")
            ?: run {
                Log.e(RetentionLog.TAG, "Unable to resolve string resource: $name")
                null
            }
    }

    private fun resolveLayout(application: Application, name: String?): Int? {
        if (name.isNullOrBlank()) {
            return null
        }
        return resolveResource(application, name, "layout")
            ?: run {
                Log.e(RetentionLog.TAG, "Unable to resolve layout resource: $name")
                null
            }
    }

    private fun resolveResource(application: Application, name: String, type: String): Int? {
        val resId = application.resources.getIdentifier(name, type, application.packageName)
        return resId.takeIf { it != 0 }
    }

    private fun com.google.gson.JsonObject?.toBundle(): Bundle {
        val bundle = Bundle()
        if (this == null) {
            return bundle
        }
        for ((key, value) in entrySet()) {
            putPrimitive(bundle, key, value)
        }
        return bundle
    }

    private fun putPrimitive(bundle: Bundle, key: String, value: JsonElement) {
        if (!value.isJsonPrimitive) {
            return
        }
        val primitive = value.asJsonPrimitive
        when {
            primitive.isBoolean -> bundle.putBoolean(key, primitive.asBoolean)
            primitive.isString -> bundle.putString(key, primitive.asString)
            primitive.isNumber -> {
                val raw = primitive.asString
                if (raw.contains('.') || raw.contains('e', ignoreCase = true)) {
                    bundle.putDouble(key, primitive.asDouble)
                } else {
                    val longValue = primitive.asLong
                    if (longValue in Int.MIN_VALUE..Int.MAX_VALUE) {
                        bundle.putInt(key, longValue.toInt())
                    } else {
                        bundle.putLong(key, longValue)
                    }
                }
            }
        }
    }
}
