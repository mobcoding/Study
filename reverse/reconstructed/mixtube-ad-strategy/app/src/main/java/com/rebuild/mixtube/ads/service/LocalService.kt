package com.rebuild.mixtube.ads.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.rebuild.mixtube.ads.Tracking
import java.util.Calendar

class LocalService : Service() {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_ALARM -> {
                    Log.d(TAG, "alarm tick")
                    clearFrequencyPrefs()
                    Tracking.log(this@LocalService, "keepalive_alarm_tick")
                }
                ACTION_NOTIFY_PREV,
                ACTION_NOTIFY_NEXT,
                ACTION_NOTIFY_PLAY_PAUSE,
                ACTION_NOTIFY_LIKE,
                ACTION_NOTIFY_ALL_LIKE,
                ACTION_NOTIFY_SEEK_TO,
                ACTION_NOTIFY,
                ACTION_TOOLBAR_NOTIFY,
                Intent.ACTION_MEDIA_BUTTON,
                "android.net.conn.CONNECTIVITY_CHANGE",
                "android.bluetooth.device.action.ACL_DISCONNECTED" -> {
                    Tracking.log(
                        this@LocalService,
                        "keepalive_action",
                        mapOf("action" to intent.action),
                    )
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        registerRuntimeReceiver()
        ensureForeground()
        scheduleDailyTick()
        markServiceEnabledOnce()
        Log.d(TAG, "LocalService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "start")
                ensureForeground()
                scheduleDailyTick()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(receiver) }
        Log.d(TAG, "LocalService destroyed")
    }

    private fun ensureForeground() {
        val notification = NotificationHelper
            .foregroundServiceNotificationBuilder(this, "Background service active")
            .addAction(
                android.R.drawable.ic_media_previous,
                "Prev",
                PendingIntent.getBroadcast(
                    this,
                    100,
                    Intent(ACTION_NOTIFY_PREV).setPackage(packageName),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(
                android.R.drawable.ic_media_play,
                "Play/Pause",
                PendingIntent.getBroadcast(
                    this,
                    101,
                    Intent(ACTION_NOTIFY_PLAY_PAUSE).setPackage(packageName),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                PendingIntent.getBroadcast(
                    this,
                    102,
                    Intent(ACTION_NOTIFY_NEXT).setPackage(packageName),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(
                android.R.drawable.btn_star_big_off,
                "Like",
                PendingIntent.getBroadcast(
                    this,
                    103,
                    Intent(ACTION_NOTIFY_LIKE).setPackage(packageName),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun scheduleDailyTick() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            DAILY_TICK_REQUEST_CODE,
            Intent(ACTION_ALARM).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            86_400_000L,
            pendingIntent
        )
    }

    private fun registerRuntimeReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_NOTIFY)
            addAction(ACTION_TOOLBAR_NOTIFY)
            addAction(ACTION_NOTIFY_PREV)
            addAction(ACTION_NOTIFY_PLAY_PAUSE)
            addAction(ACTION_NOTIFY_NEXT)
            addAction(ACTION_NOTIFY_LIKE)
            addAction(ACTION_NOTIFY_ALL_LIKE)
            addAction(ACTION_NOTIFY_SEEK_TO)
            addAction(Intent.ACTION_MEDIA_BUTTON)
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction(ACTION_ALARM)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
    }

    private fun markServiceEnabledOnce() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, true).apply()
    }

    private fun clearFrequencyPrefs() {
        getSharedPreferences("ad_frequency", MODE_PRIVATE).edit().clear().apply()
    }

    companion object {
        private const val TAG = "LocalService"
        private const val PREFS = "mixtube_keepalive"
        private const val KEY_ENABLED = "enabled"
        private const val NOTIFICATION_ID = 1001
        private const val DAILY_TICK_REQUEST_CODE = 15

        const val ACTION_START = "com.rebuild.mixtube.ads.action.LOCAL_SERVICE_START"
        const val ACTION_ALARM = "com.rebuild.mixtube.ads.alarm"

        const val ACTION_NOTIFY = "com.rebuild.mixtube.ads.notify"
        const val ACTION_TOOLBAR_NOTIFY = "com.rebuild.mixtube.ads.toolbar.notify"
        const val ACTION_NOTIFY_PREV = "com.rebuild.mixtube.ads.notify.prev"
        const val ACTION_NOTIFY_PLAY_PAUSE = "com.rebuild.mixtube.ads.notify.play_pause"
        const val ACTION_NOTIFY_NEXT = "com.rebuild.mixtube.ads.notify.next"
        const val ACTION_NOTIFY_LIKE = "com.rebuild.mixtube.ads.notify.like"
        const val ACTION_NOTIFY_ALL_LIKE = "com.rebuild.mixtube.ads.notify.all.like"
        const val ACTION_NOTIFY_SEEK_TO = "com.rebuild.mixtube.ads.notify.seek.to"
    }
}
