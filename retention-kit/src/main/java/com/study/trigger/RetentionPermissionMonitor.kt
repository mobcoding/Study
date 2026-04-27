package com.study.trigger

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

internal object RetentionPermissionMonitor {

    private val handler = Handler(Looper.getMainLooper())
    private var storageRunnable: Runnable? = null

    fun monitorStoragePermissionPage(activity: Activity) {
        stopStoragePolling()
        storageRunnable = object : Runnable {
            override fun run() {
                if (hasStoragePermission(activity)) {
                    bringAppToFront(activity)
                    stopStoragePolling()
                    return
                }
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }.also { handler.postDelayed(it, POLL_INTERVAL_MS) }
    }

    fun onHostAppForegrounded() {
        stopStoragePolling()
    }

    private fun stopStoragePolling() {
        storageRunnable?.let(handler::removeCallbacks)
        storageRunnable = null
    }

    private fun hasStoragePermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun bringAppToFront(activity: Activity) {
        val launchIntent = activity.packageManager.getLaunchIntentForPackage(activity.packageName) ?: return
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        activity.startActivity(launchIntent)
    }

    private const val POLL_INTERVAL_MS = 300L
}
