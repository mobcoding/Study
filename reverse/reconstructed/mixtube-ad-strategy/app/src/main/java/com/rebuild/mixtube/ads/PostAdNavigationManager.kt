package com.rebuild.mixtube.ads

import android.app.Activity
import android.content.Intent

object PostAdNavigationManager {
    private var pendingIntent: Intent? = null

    fun setPending(intent: Intent) {
        pendingIntent = intent
    }

    fun clear() {
        pendingIntent = null
    }

    fun consumeAndStart(activity: Activity) {
        val intent = pendingIntent ?: return
        pendingIntent = null
        activity.startActivity(intent)
    }
}
