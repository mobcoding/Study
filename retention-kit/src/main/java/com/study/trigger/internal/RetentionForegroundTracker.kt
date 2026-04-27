package com.study.trigger.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.study.trigger.RetentionPermissionMonitor
import java.util.concurrent.atomic.AtomicInteger

internal object RetentionForegroundTracker : Application.ActivityLifecycleCallbacks {

    private val foregroundCount = AtomicInteger(0)

    fun isAppForeground(): Boolean = foregroundCount.get() > 0

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        foregroundCount.incrementAndGet()
    }

    override fun onActivityResumed(activity: Activity) = RetentionPermissionMonitor.onHostAppForegrounded()

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        val current = foregroundCount.decrementAndGet()
        if (current < 0) {
            foregroundCount.set(0)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
