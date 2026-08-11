package com.zero.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class StudyAppWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        WidgetClockService.start(context)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE || intent.action == WidgetUpdater.ACTION_REFRESH) {
            WidgetUpdater.updateAll(context, refreshRunningApps = true)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.update(context, appWidgetIds, refreshRunningApps = true)
        WidgetClockService.start(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdater.updateAll(context, refreshRunningApps = true)
        WidgetClockService.start(context)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdater.stopClock()
    }
}
