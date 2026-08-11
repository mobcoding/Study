package com.zero.widget

import android.app.ActivityManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.StatFs
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

internal object WidgetUpdater {
    const val ACTION_REFRESH = "com.zero.widget.action.REFRESH"
    private val clockHandler = Handler(Looper.getMainLooper())
    private var clockTick: Runnable? = null

    fun updateAll(context: Context, refreshRunningApps: Boolean = false) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, StudyAppWidgetProvider::class.java))
        update(context, ids, refreshRunningApps)
    }

    fun update(context: Context, ids: IntArray, refreshRunningApps: Boolean = false) {
        if (ids.isEmpty()) return
        AppWidgetManager.getInstance(context).updateAppWidget(ids, buildFullView(context, refreshRunningApps))
    }

    fun updateClock(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, StudyAppWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val view = RemoteViews(context.packageName, R.layout.widget_main).apply {
            setTextViewText(R.id.tv_date, formatDate())
            setImageViewBitmap(R.id.iv_widget_clock, WidgetClockRenderer.render(context))
        }
        manager.partiallyUpdateAppWidget(ids, view)
    }

    fun hasWidgets(context: Context): Boolean = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, StudyAppWidgetProvider::class.java))
        .isNotEmpty()

    @Synchronized
    fun startClock(context: Context) {
        if (clockTick != null) return

        val appContext = context.applicationContext
        val tick = object : Runnable {
            override fun run() {
                try {
                    if (!hasWidgets(appContext)) {
                        stopClock()
                        return
                    }
                    updateClock(appContext)
                } catch (throwable: Throwable) {
                    Log.e("WidgetUpdater", "Unable to update the widget clock", throwable)
                } finally {
                    if (clockTick === this) {
                        clockHandler.postAtTime(
                            this,
                            SystemClock.uptimeMillis() + 1_000L - System.currentTimeMillis() % 1_000L,
                        )
                    }
                }
            }
        }
        clockTick = tick
        tick.run()
    }

    @Synchronized
    fun stopClock() {
        clockTick?.let(clockHandler::removeCallbacks)
        clockTick = null
    }

    private fun buildFullView(context: Context, refreshRunningApps: Boolean): RemoteViews {
        val memoryPercent = memoryUsedPercent(context)
        val storagePercent = storageUsedPercent(context)
        return RemoteViews(context.packageName, R.layout.widget_main).apply {
            setTextViewText(R.id.tv_date, formatDate())
            setImageViewBitmap(R.id.iv_widget_clock, WidgetClockRenderer.render(context))
            setTextViewText(R.id.tv_mem, "$memoryPercent%")
            setProgressBar(R.id.pb_mem, 100, memoryPercent, false)
            setTextColor(R.id.tv_mem, statusColor(context, memoryPercent))
            setTextViewText(R.id.tv_disk, "$storagePercent%")
            setProgressBar(R.id.pb_disk, 100, storagePercent, false)
            setTextColor(R.id.tv_disk, statusColor(context, storagePercent))
            setTextViewText(R.id.tv_apps, "${runningAppCount(context)} apps")
            setOnClickPendingIntent(R.id.rl_mem, setupPendingIntent(context, DesktopWidgetActivity.SECTION_MEMORY, 0))
            setOnClickPendingIntent(R.id.rl_disk, setupPendingIntent(context, DesktopWidgetActivity.SECTION_STORAGE, 1))
            setOnClickPendingIntent(R.id.rl_apps, setupPendingIntent(context, DesktopWidgetActivity.SECTION_APPS, 2))
            setOnClickPendingIntent(R.id.rl_time, setupPendingIntent(context, DesktopWidgetActivity.SECTION_CLOCK, 3))
        }
    }

    private fun setupPendingIntent(context: Context, section: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, DesktopWidgetActivity::class.java)
            .putExtra(DesktopWidgetActivity.EXTRA_SECTION, section)
            .setData(android.net.Uri.parse("study-widget://section/$section"))
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun memoryUsedPercent(context: Context): Int {
        val info = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
        return ((1 - info.availMem.toDouble() / info.totalMem) * 100).roundToInt().coerceIn(0, 100)
    }

    private fun storageUsedPercent(context: Context): Int {
        val stats = StatFs(context.filesDir.absolutePath)
        val total = stats.blockCountLong * stats.blockSizeLong
        val available = stats.availableBlocksLong * stats.blockSizeLong
        return ((1 - available.toDouble() / total) * 100).roundToInt().coerceIn(0, 100)
    }

    private fun runningAppCount(context: Context): Int = runCatching {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses?.size ?: 0
    }.getOrDefault(0)

    private fun statusColor(context: Context, percent: Int): Int = when {
        percent >= 90 -> ContextCompat.getColor(context, R.color.colorProgressUrgent)
        percent >= 75 -> ContextCompat.getColor(context, R.color.colorProgressWarning)
        else -> ContextCompat.getColor(context, R.color.colorProgressNormal)
    }

    private fun formatDate(): String = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
}
