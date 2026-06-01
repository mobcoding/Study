package com.rebuild.mixtube.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import android.util.Log

class AppForegroundTracker : Application.ActivityLifecycleCallbacks {
    private var startedCount = 0
    private var appWasBackgrounded = false
    private var lastBackgroundAt = 0L
    private var pendingForegroundReturn = false

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        val wasBackground = startedCount == 0 && appWasBackgrounded
        startedCount += 1
        if (wasBackground) {
            pendingForegroundReturn = true
            Log.d(TAG, "app returned to foreground by ${activity.localClassName}")
        }
    }

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        startedCount = (startedCount - 1).coerceAtLeast(0)
        if (startedCount == 0) {
            appWasBackgrounded = true
            lastBackgroundAt = SystemClock.elapsedRealtime()
            Log.d(TAG, "app moved to background from ${activity.localClassName}")
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    fun consumeForegroundReturn(minBackgroundMillis: Long = 1500L): Boolean {
        if (!pendingForegroundReturn) return false
        val backgroundDuration = SystemClock.elapsedRealtime() - lastBackgroundAt
        if (backgroundDuration < minBackgroundMillis) {
            Log.d(TAG, "ignore foreground return: backgroundDuration=$backgroundDuration")
            pendingForegroundReturn = false
            return false
        }
        pendingForegroundReturn = false
        Log.d(TAG, "consume foreground return: backgroundDuration=$backgroundDuration")
        return true
    }

    fun markColdStartHandled() {
        appWasBackgrounded = false
        pendingForegroundReturn = false
        lastBackgroundAt = 0L
    }

    private companion object {
        const val TAG = "AppForegroundTracker"
    }
}
