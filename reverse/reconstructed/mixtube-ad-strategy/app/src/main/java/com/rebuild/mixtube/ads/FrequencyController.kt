package com.rebuild.mixtube.ads

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.abs

class FrequencyController(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ad_frequency", Context.MODE_PRIVATE)

    fun canShow(scene: String, config: AdStrategyConfig, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val totalShown = prefs.getInt(KEY_TOTAL_SHOWN, 0)
        if (totalShown >= config.showCount) return false

        val lastScene = prefs.getString(KEY_LAST_SCENE, null)
        val lastShownAt = prefs.getLong(KEY_LAST_SHOWN_AT, 0L)
        if (lastShownAt == 0L) return true

        val elapsedSeconds = abs(nowMillis - lastShownAt) / 1000
        val requiredInterval = if (lastScene == scene) config.sameInterval else config.differentInterval
        return elapsedSeconds >= requiredInterval
    }

    fun recordShow(scene: String, nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString(KEY_LAST_SCENE, scene)
            .putLong(KEY_LAST_SHOWN_AT, nowMillis)
            .putInt(KEY_TOTAL_SHOWN, prefs.getInt(KEY_TOTAL_SHOWN, 0) + 1)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun snapshot(): String {
        return "total=${prefs.getInt(KEY_TOTAL_SHOWN, 0)}, lastScene=${prefs.getString(KEY_LAST_SCENE, "none")}, lastAt=${prefs.getLong(KEY_LAST_SHOWN_AT, 0L)}"
    }

    private companion object {
        const val KEY_TOTAL_SHOWN = "total_shown"
        const val KEY_LAST_SCENE = "last_scene"
        const val KEY_LAST_SHOWN_AT = "last_shown_at"
    }
}
