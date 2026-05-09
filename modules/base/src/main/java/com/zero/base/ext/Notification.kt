package com.zero.base.ext

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import com.zero.library_base.R


fun Context.createNotificationChannelIfNeeded(channelId: String, name: String, importance: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val serviceChannel = NotificationChannel(channelId, name, importance)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}

/**
 * 通过服务启动常驻通知 所以返回通知对象
 */
fun Context.createNotification(configBlock: NotificationConfig.() -> Unit): Notification {
    val config = NotificationConfig().apply(configBlock)
    createNotificationChannelIfNeeded(config.channelId, config.channelName, config.importance)
    val pendingIntent = config.clickIntent?.let {
        PendingIntent.getActivity(this, System.currentTimeMillis().hashCode(), it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    val notification = NotificationCompat.Builder(this, config.channelId).setSmallIcon(config.smallIcon).setOngoing(config.isPermanent).setContentTitle(config.title).apply {
        setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (config.isPermanent) {
            setPriority(NotificationCompat.PRIORITY_LOW)
        } else {
            setPriority(NotificationCompat.PRIORITY_HIGH)
            setAutoCancel(true)
        }
        if (config.groupKey.isNotEmpty()) {
            setGroup(config.groupKey)
            setGroupSummary(true)
        }
        if (config.content.isNotEmpty()) {
            setContentText(config.content)
        }
        if (config.remoteViews != null) {
            setCustomContentView(config.remoteViews)
        }
        if (config.bigRemoteViews != null) {
            setCustomBigContentView(config.bigRemoteViews)
        }
        if (pendingIntent != null) {
            setContentIntent(pendingIntent)
        }
    }.build()
    config.notification = notification
    return notification

}


data class NotificationConfig(
    /**
     * 通知的标题
     */
    var title: String = "",
    /**
     * 常规的通知内容
     */
    var content: String = "",
    /**
     * 通知渠道id
     */
    var channelId: String = "default",
    /***
     * 通知渠道名称
     */
    var channelName: String = "default",
    /**
     * 自定义图片资源
     */
    @param:DrawableRes var iconRes: Int = 0,

    /**
     * 通知图标
     */
    @param:DrawableRes var smallIcon: Int = R.drawable.base_ic_circle,
    /**
     * 通知id
     */
    var notificationId: Int = 1001,
    /**
     * 通知重要程度(会影响悬浮/横幅)
     */
    var importance: Int = IMPORTANCE_LOW,
    /**
     * 自定义通知视图 大视图
     */
    var bigRemoteViews: RemoteViews? = null,
    /**
     * 自定义通知视图 小视图
     */
    var remoteViews: RemoteViews? = null,
    /**
     * 跳转对象
     */
    var clickIntent: Intent? = null,
    /**
     * 通知对象 用于更新通知
     */
    var notification: Notification? = null,
    /**
     * 是否是常驻通知(android13及以下可以滑动删除)
     */
    var isPermanent: Boolean = false,

    var groupKey: String = ""

)