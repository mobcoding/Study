package com.study.retention.internal

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.annotation.RequiresPermission
import com.google.firebase.analytics.FirebaseAnalytics

internal object RetentionAnalytics {
    private const val EVENT_RETENTION = "retention"
    private const val KEY_NAME = "name"

    @RequiresPermission(allOf = [Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE])
    fun logNotificationSent(context: Context, scene: String) {
        FirebaseAnalytics.getInstance(context).logEvent(
            EVENT_RETENTION,
            Bundle().apply {
                putString(KEY_NAME, scene)
            }
        )
    }
}
