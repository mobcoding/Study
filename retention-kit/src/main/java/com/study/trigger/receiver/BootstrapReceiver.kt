package com.study.trigger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.study.trigger.RetentionModule
import com.google.firebase.FirebaseApp

internal class BootstrapReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val safeContext = context ?: return
        FirebaseApp.initializeApp(safeContext)
        if (Build.MANUFACTURER.equals("Samsung", ignoreCase = true)) {
            return
        }
        RetentionModule.ensureRunning(safeContext, forceAlarmReschedule = true)
    }
}
