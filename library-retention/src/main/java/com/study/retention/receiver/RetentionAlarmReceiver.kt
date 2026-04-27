package com.study.retention.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.study.retention.internal.RetentionEngine

internal class RetentionAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val safeContext = context ?: return
        RetentionEngine.handleAlarm(safeContext)
    }
}
