package com.study.retention.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.study.retention.internal.RetentionEngine

internal class RetentionBootReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context?, intent: Intent?) {
        val safeContext = context ?: return
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            RetentionEngine.handleBoot(safeContext)
        }
    }
}
