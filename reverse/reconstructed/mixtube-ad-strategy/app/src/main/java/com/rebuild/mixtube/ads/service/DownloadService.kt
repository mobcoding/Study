package com.rebuild.mixtube.ads.service

import android.app.DownloadManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.rebuild.mixtube.ads.ServiceLocator
import java.util.Locale

class DownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENQUEUE -> {
                val url = intent.getStringExtra(EXTRA_URL).orEmpty()
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
                if (url.isBlank()) {
                    Log.d(TAG, "enqueue ignored: empty url")
                    return START_NOT_STICKY
                }
                if (!isDownloadAllowed()) {
                    Log.d(TAG, "enqueue blocked by policy")
                    return START_NOT_STICKY
                }
                enqueue(url, title.ifBlank { "Download" }, description)
            }
        }
        return START_NOT_STICKY
    }

    private fun isDownloadAllowed(): Boolean {
        val remoteConfig = runCatching { ServiceLocator.remoteConfig }.getOrNull()
            ?: return true
        val enabled = remoteConfig.getString("show_download", "1") == "1"
        if (!enabled) return false
        val blocked = remoteConfig.getString("close_download_country", "").trim()
        if (blocked.isBlank()) return true
        val country = Locale.getDefault().country.uppercase(Locale.US)
        return country != blocked.uppercase(Locale.US)
    }

    private fun enqueue(url: String, title: String, description: String) {
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(title)
            .setDescription(description)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val id = manager.enqueue(request)
        Log.d(TAG, "enqueued download id=$id url=$url")

        NotificationHelper.ensureDefaultChannel(this)
        val notification = NotificationHelper
            .defaultNotificationBuilder(this, "Mixtube", "Download queued")
            .build()
        NotificationManagerCompat.from(this).notify((id % Int.MAX_VALUE).toInt(), notification)
    }

    companion object {
        private const val TAG = "DownloadService"
        const val ACTION_ENQUEUE = "com.rebuild.mixtube.ads.action.DOWNLOAD_ENQUEUE"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "description"
    }
}
