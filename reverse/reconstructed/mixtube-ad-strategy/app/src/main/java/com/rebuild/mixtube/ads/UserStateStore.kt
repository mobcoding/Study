package com.rebuild.mixtube.ads

import android.content.Context

object UserStateStore {
    private const val PREFS = "mixtube_user_state"
    private const val KEY_ORGANIC_PASSED = "organic_passed"

    fun isOrganicPassed(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ORGANIC_PASSED, false)
    }

    fun setOrganicPassed(context: Context, passed: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ORGANIC_PASSED, passed).apply()
    }
}

