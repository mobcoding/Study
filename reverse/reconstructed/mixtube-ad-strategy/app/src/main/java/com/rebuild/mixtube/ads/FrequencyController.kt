package com.rebuild.mixtube.ads

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class FrequencyController(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ad_frequency", Context.MODE_PRIVATE)

    fun canShow(
        scene: String,
        config: AdStrategyConfig,
        unit: AdUnitConfig?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return canShow(
            scene = scene,
            config = config,
            unit = unit,
            trigger = triggerFor(scene, unit),
            nowMillis = nowMillis,
        )
    }

    fun canShow(
        scene: String,
        config: AdStrategyConfig,
        unit: AdUnitConfig?,
        trigger: FrequencyTrigger,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        resetIfNewDay(nowMillis)

        val totalShown = prefs.getInt(KEY_TOTAL_SHOWN, 0)
        if (totalShown >= config.showCount) return false

        val coolI = config.coolISeconds.coerceAtLeast(0)
        val coolN = config.coolNSeconds.coerceAtLeast(0)
        val coolP = config.coolPSeconds.coerceAtLeast(0)

        val tsM = prefs.getLong(KEY_TS_M, 0L)
        val tsN = prefs.getLong(KEY_TS_N, 0L)
        val tsO = prefs.getLong(KEY_TS_O, 0L)
        val tsP = prefs.getLong(KEY_TS_P, 0L)

        val legacy = legacySceneFor(scene, trigger)
        if (legacy != null && !passesLegacyCooldowns(legacy, nowMillis, coolI, coolN, coolP, tsM, tsN, tsO, tsP)) return false

        val lastScene = prefs.getString(KEY_LAST_SCENE, null)
        val lastShownAt = prefs.getLong(KEY_LAST_SHOWN_AT, 0L)
        if (lastShownAt == 0L) return true

        val elapsedSeconds = abs(nowMillis - lastShownAt) / 1000
        val requiredInterval = if (lastScene == scene) config.sameInterval else config.differentInterval
        if (elapsedSeconds < requiredInterval) return false

        val slot = slotFor(scene, unit)

        val openCooldown = config.openivtime.coerceAtLeast(0)
        val downloadCooldown = config.downloadivtime.coerceAtLeast(0)

        val lastAnyAt = prefs.getLong(KEY_LAST_ANY_SHOWN_AT, 0L)
        if (lastAnyAt > 0) {
            val lastAnyElapsed = abs(nowMillis - lastAnyAt) / 1000
            val globalCooldown = when (slot) {
                Slot.OPEN -> openCooldown
                Slot.DOWNLOAD -> downloadCooldown
                else -> 0
            }
            if (globalCooldown > 0 && lastAnyElapsed < globalCooldown) return false
        }

        val lastOpenAt = prefs.getLong(KEY_LAST_OPEN_AT, 0L)
        if (openCooldown > 0 && lastOpenAt > 0) {
            val elapsed = abs(nowMillis - lastOpenAt) / 1000
            if (elapsed < openCooldown) return false
        }

        val lastDownloadAt = prefs.getLong(KEY_LAST_DOWNLOAD_AT, 0L)
        if (downloadCooldown > 0 && lastDownloadAt > 0) {
            val elapsed = abs(nowMillis - lastDownloadAt) / 1000
            if (elapsed < downloadCooldown) return false
        }

        val slotCooldown = when (slot) {
            Slot.OPEN -> openCooldown
            Slot.DOWNLOAD -> downloadCooldown
            else -> 0
        }
        if (slotCooldown > 0) {
            val lastSlotAt = prefs.getLong(slot.key, 0L)
            if (lastSlotAt > 0) {
                val elapsed = abs(nowMillis - lastSlotAt) / 1000
                if (elapsed < slotCooldown) return false
            }
        }

        return true
    }

    fun recordShow(scene: String, unit: AdUnitConfig?, nowMillis: Long = System.currentTimeMillis()) {
        recordShow(scene, unit, triggerFor(scene, unit), nowMillis)
    }

    fun recordShow(
        scene: String,
        unit: AdUnitConfig?,
        trigger: FrequencyTrigger,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        resetIfNewDay(nowMillis)
        val slot = slotFor(scene, unit)
        val legacy = legacySceneFor(scene, trigger)
        val editor = prefs.edit()
            .putString(KEY_LAST_SCENE, scene)
            .putLong(KEY_LAST_SHOWN_AT, nowMillis)
            .putLong(KEY_LAST_ANY_SHOWN_AT, nowMillis)
            .putLong(slot.key, nowMillis)
            .apply {
                when (slot) {
                    Slot.OPEN -> putLong(KEY_LAST_OPEN_AT, nowMillis)
                    Slot.DOWNLOAD -> putLong(KEY_LAST_DOWNLOAD_AT, nowMillis)
                    else -> Unit
                }
            }
            .putInt(KEY_TOTAL_SHOWN, prefs.getInt(KEY_TOTAL_SHOWN, 0) + 1)
        if (legacy != null) applyLegacyCooldownUpdate(editor, legacy, nowMillis)
        editor.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun snapshot(): String {
        return buildString {
            append("day=").append(prefs.getString(KEY_DAY, "none"))
            append(", total=").append(prefs.getInt(KEY_TOTAL_SHOWN, 0))
            append(", lastScene=").append(prefs.getString(KEY_LAST_SCENE, "none"))
            append(", lastAt=").append(prefs.getLong(KEY_LAST_SHOWN_AT, 0L))
            append(", lastAnyAt=").append(prefs.getLong(KEY_LAST_ANY_SHOWN_AT, 0L))
            append(", tsM=").append(prefs.getLong(KEY_TS_M, 0L))
            append(", tsN=").append(prefs.getLong(KEY_TS_N, 0L))
            append(", tsO=").append(prefs.getLong(KEY_TS_O, 0L))
            append(", tsP=").append(prefs.getLong(KEY_TS_P, 0L))
        }
    }

    private fun resetIfNewDay(nowMillis: Long) {
        val today = DAY_FORMAT.format(Date(nowMillis))
        val stored = prefs.getString(KEY_DAY, null)
        if (stored == today) return
        prefs.edit()
            .clear()
            .putString(KEY_DAY, today)
            .apply()
    }

    private companion object {
        val DAY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        const val KEY_DAY = "day"
        const val KEY_TOTAL_SHOWN = "total_shown"
        const val KEY_LAST_SCENE = "last_scene"
        const val KEY_LAST_SHOWN_AT = "last_shown_at"
        const val KEY_LAST_ANY_SHOWN_AT = "last_any_shown_at"
        const val KEY_LAST_OPEN_AT = "last_open_at"
        const val KEY_LAST_DOWNLOAD_AT = "last_download_at"
        const val KEY_TS_M = "ts_m"
        const val KEY_TS_N = "ts_n"
        const val KEY_TS_O = "ts_o"
        const val KEY_TS_P = "ts_p"
    }

    enum class FrequencyTrigger {
        STARTUP,
        SWITCHBACK,
        PLAY,
        DOWNLOAD,
        OTHER,
        NATIVE,
        BANNER,
    }

    private enum class LegacyScene(val key: String) {
        HOT_SPLASH_AD("hot_splash_ad"),
        PLAY_INTERSTITIAL_AD("play_interstitial_ad"),
        DOWNLOAD_INTERSTITIAL_AD("download_interstitial_ad"),
        PLAYLIST_INTERSTITIAL_AD("playlist_interstitial_ad"),
        CLEAN_UP("clean up"),
        PAUSE_INTERSTITIAL_AD("pause_interstitial_ad"),
    }

    private enum class Slot(val key: String) {
        OPEN("slot_open_at"),
        DOWNLOAD("slot_download_at"),
        INTERSTITIAL("slot_interstitial_at"),
        NATIVE("slot_native_at"),
        BANNER("slot_banner_at"),
        OTHER("slot_other_at"),
    }

    private fun slotFor(scene: String, unit: AdUnitConfig?): Slot {
        val type = unit?.adtype.orEmpty()
        return when {
            type.equals("open", ignoreCase = true) -> Slot.OPEN
            scene.equals("mixIVDownload", ignoreCase = true) -> Slot.DOWNLOAD
            scene.equals("mixnative", ignoreCase = true) || scene.equals("NVsearch", ignoreCase = true) -> Slot.NATIVE
            scene.contains("Banner", ignoreCase = true) || type.contains("banner", ignoreCase = true) -> Slot.BANNER
            type.contains("inter", ignoreCase = true) || type.contains("reward", ignoreCase = true) -> Slot.INTERSTITIAL
            else -> Slot.OTHER
        }
    }

    private fun triggerFor(scene: String, unit: AdUnitConfig?): FrequencyTrigger {
        val slot = slotFor(scene, unit)
        return when (slot) {
            Slot.OPEN -> FrequencyTrigger.SWITCHBACK
            Slot.DOWNLOAD -> FrequencyTrigger.DOWNLOAD
            Slot.NATIVE -> FrequencyTrigger.NATIVE
            Slot.BANNER -> FrequencyTrigger.BANNER
            else -> {
                when {
                    scene.equals("mixIVplay", ignoreCase = true) -> FrequencyTrigger.PLAY
                    else -> FrequencyTrigger.OTHER
                }
            }
        }
    }

    private fun legacySceneFor(scene: String, trigger: FrequencyTrigger): LegacyScene? {
        val normalized = scene.trim()
        LegacyScene.entries.firstOrNull { it.key.equals(normalized, ignoreCase = true) }?.let { return it }

        if (normalized.startsWith("pause_interstitial", ignoreCase = true)) return LegacyScene.PAUSE_INTERSTITIAL_AD

        return when {
            trigger == FrequencyTrigger.STARTUP -> LegacyScene.HOT_SPLASH_AD
            normalized.equals(AdScene.Play.key, ignoreCase = true) -> LegacyScene.PLAY_INTERSTITIAL_AD
            normalized.equals(AdScene.Download.key, ignoreCase = true) -> LegacyScene.DOWNLOAD_INTERSTITIAL_AD
            normalized.equals(AdScene.CleanBoost.key, ignoreCase = true) -> LegacyScene.CLEAN_UP
            else -> null
        }
    }

    private fun passesLegacyCooldowns(
        legacy: LegacyScene,
        nowMillis: Long,
        coolISeconds: Int,
        coolNSeconds: Int,
        coolPSeconds: Int,
        tsM: Long,
        tsN: Long,
        tsO: Long,
        tsP: Long,
    ): Boolean {
        fun sinceSeconds(ts: Long): Long {
            if (ts <= 0) return Long.MAX_VALUE
            return abs(nowMillis - ts) / 1000
        }

        val sinceM = sinceSeconds(tsM)
        val sinceN = sinceSeconds(tsN)
        val sinceO = sinceSeconds(tsO)
        val sinceP = sinceSeconds(tsP)

        return when (legacy) {
            LegacyScene.DOWNLOAD_INTERSTITIAL_AD -> {
                if (coolISeconds <= 0) true
                else sinceM >= coolISeconds && sinceP >= coolISeconds && sinceN >= coolISeconds
            }
            LegacyScene.HOT_SPLASH_AD -> {
                if (coolPSeconds > 0 && sinceN < coolPSeconds) return false
                if (coolNSeconds <= 0) return true
                sinceM >= coolNSeconds && sinceP >= coolNSeconds && sinceO >= coolNSeconds
            }
            LegacyScene.PLAYLIST_INTERSTITIAL_AD -> {
                if (coolPSeconds > 0 && sinceP < coolPSeconds) return false
                if (coolISeconds > 0 && (sinceM < coolISeconds || sinceO < coolISeconds)) return false
                if (coolNSeconds > 0 && sinceN < coolNSeconds) return false
                true
            }
            LegacyScene.PLAY_INTERSTITIAL_AD,
            LegacyScene.PAUSE_INTERSTITIAL_AD,
            LegacyScene.CLEAN_UP -> {
                if (coolPSeconds > 0 && sinceM < coolPSeconds) return false
                if (coolISeconds > 0 && (sinceP < coolISeconds || sinceO < coolISeconds)) return false
                if (coolNSeconds > 0 && sinceN < coolNSeconds) return false
                true
            }
        }
    }

    private fun applyLegacyCooldownUpdate(
        editor: SharedPreferences.Editor,
        legacy: LegacyScene,
        nowMillis: Long,
    ) {
        when (legacy) {
            LegacyScene.HOT_SPLASH_AD -> editor.putLong(KEY_TS_N, nowMillis)
            LegacyScene.DOWNLOAD_INTERSTITIAL_AD -> editor.putLong(KEY_TS_O, nowMillis)
            LegacyScene.PLAY_INTERSTITIAL_AD,
            LegacyScene.PAUSE_INTERSTITIAL_AD -> editor.putLong(KEY_TS_M, nowMillis)
            LegacyScene.PLAYLIST_INTERSTITIAL_AD,
            LegacyScene.CLEAN_UP -> editor.putLong(KEY_TS_P, nowMillis)
        }
    }
}
