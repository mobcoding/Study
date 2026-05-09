package com.zero.base.theme

import androidx.appcompat.app.AppCompatDelegate

/**
 * @date:2024/7/5 18:55
 * @path:com.zero.base.theme.AppTheme
 */
enum class AppTheme(val mode: Int) {
    AUTO(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        fun byMode(mode: Int): AppTheme {
            return entries.firstOrNull { it.mode == mode } ?: AUTO
        }
    }
}
