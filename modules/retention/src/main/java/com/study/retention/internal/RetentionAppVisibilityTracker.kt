package com.study.retention.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger

internal object RetentionAppVisibilityTracker : Application.ActivityLifecycleCallbacks {

    private val foregroundCount = AtomicInteger(0)

    fun isAppForeground(): Boolean = foregroundCount.get() > 0

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        val previous = foregroundCount.getAndIncrement()
        if (previous == 0) {
            RetentionEngine.onAppForegroundEntered()
        }
    }

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        val current = foregroundCount.decrementAndGet()
        if (current < 0) {
            foregroundCount.set(0)
            return
        }
        if (current == 0 && !activity.isChangingConfigurations) {
            RetentionEngine.onAppBackground()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
