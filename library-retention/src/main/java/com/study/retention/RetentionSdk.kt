package com.study.retention

import android.content.Context
import com.study.retention.internal.RetentionEngine

object RetentionSdk {

    fun onNotificationPermissionGranted(context: Context) {
        RetentionEngine.onNotificationPermissionGranted(context)
    }
}
