package com.rebuild.mixtube.ads.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val enabled = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
        if (!enabled) return
        Log.d(TAG, "boot completed -> start LocalService")
        val serviceIntent = Intent(context, LocalService::class.java).setAction(LocalService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val PREFS = "mixtube_keepalive"
        private const val KEY_ENABLED = "enabled"
    }
}

