package com.study.retention

import android.content.Context
import com.study.retention.internal.RetentionEngine

object RetentionSdk {

    fun initialize(context: Context): Boolean {
        return RetentionEngine.initialize(context)
    }

    fun enterForegroundRuntime(
        context: Context,
        sessionId: String,
        source: String = "host",
    ) {
        RetentionEngine.enterForegroundRuntime(context, sessionId, source)
    }

    fun exitForegroundRuntime(sessionId: String? = null) {
        RetentionEngine.exitForegroundRuntime(sessionId)
    }

    fun onNotificationPermissionGranted(context: Context) {
        RetentionEngine.onNotificationPermissionGranted(context)
    }
}
