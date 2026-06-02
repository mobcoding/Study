package com.rebuild.mixtube.ads.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rebuild.mixtube.ads.R

object NotificationHelper {
    fun ensureDefaultChannel(context: Context): String {
        return ensureChannel(
            context = context,
            channelId = context.getString(R.string.channelId),
            channelName = context.getString(R.string.channelName),
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        )
    }

    fun ensureRemoteMessageChannel(context: Context): String {
        return ensureChannel(
            context = context,
            channelId = context.getString(R.string.remote_message_channelId),
            channelName = context.getString(R.string.channelName),
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        )
    }

    private fun ensureChannel(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int,
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId
        val manager = context.getSystemService(NotificationManager::class.java) ?: return channelId
        val existing = manager.getNotificationChannel(channelId)
        if (existing != null) return channelId
        val channel = NotificationChannel(channelId, channelName, importance)
        manager.createNotificationChannel(channel)
        return channelId
    }

    fun defaultNotificationBuilder(context: Context, title: String, body: String): NotificationCompat.Builder {
        val channelId = ensureDefaultChannel(context)
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    fun remoteMessageNotificationBuilder(context: Context, title: String, body: String): NotificationCompat.Builder {
        val channelId = ensureRemoteMessageChannel(context)
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    fun foregroundServiceNotificationBuilder(context: Context, contentText: String): NotificationCompat.Builder {
        val channelId = ensureDefaultChannel(context)
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(context.getString(R.string.channelName))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }
}
