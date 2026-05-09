package com.zero.health.helper

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import androidx.core.app.TaskStackBuilder
import com.zero.base.ext.createNotification
import com.zero.base.ext.formatHourMinute
import com.zero.health.R
import com.zero.health.bean.RemindType
import com.zero.health.ui.activity.AlarmRemindActivity
import com.zero.health.ui.activity.HeartRateActivity
import java.util.Date

/**
 * @date:2025/8/7 11:10
 * @path:com.zero.health.helper.NotifyHelper
 */
object NotifyHelper {
    private const val MEDIA_NOTIFICATION_CHANNEL_SUFFIX = ".search"
    private const val MEDIA_NOTIFICATION_CHANNEL_NAME_SUFFIX = ".search_channel"

    /**
     * 常驻通知(静音，无悬浮，不能自动取消也无法手动删除)
     */
    fun showToolsNotification(context: Service) {
        val mainNotification = context.createNotification {
            isPermanent = true
            smallIcon = RemindType.getSmallIcon(RemindType.HEALTH_TOOLS)
            importance = NotificationManagerCompat.IMPORTANCE_LOW
            iconRes = RemindType.getTypeIcon(RemindType.HEALTH_TOOLS)
            channelId = RemindType.getChannelId(RemindType.HEALTH_TOOLS)
            channelName = context.getString(RemindType.getChannelName(RemindType.HEALTH_TOOLS))
            remoteViews = RemoteViews(context.packageName,
                R.layout.notification_main_normal).apply {
                remoteViews?.setTextViewText(R.id.tv_blood_pressure_value, "No Record")
                val flag = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
                val clickIntent = Intent(context, HeartRateActivity::class.java).setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK)
                remoteViews?.setOnClickPendingIntent(R.id.tv_blood_pressure_value,
                    PendingIntent.getActivity(context, Activity.RESULT_OK, clickIntent, flag))
            }
            bigRemoteViews = RemoteViews(context.packageName,
                R.layout.notification_main_big).apply {

            }
        }
        context.startForeground(RemindType.getNotificationId(RemindType.HEALTH_TOOLS),
            mainNotification)
    }

    /**
     * 闹钟通知(立即弹出，有声音，可以自动取消)
     *
     */
    fun showAlarmNotification(context: Context, @RemindType.Type type: Int) {
        val remindNotification = context.createNotification {
            //添加标题或者内容可以展示桌面原点
            title = context.getString(RemindType.getChannelName(type))
            content = Date().formatHourMinute
            iconRes = RemindType.getTypeIcon(type)
            smallIcon = RemindType.getSmallIcon(type)
            channelId = RemindType.getChannelId(type)
            importance = NotificationManagerCompat.IMPORTANCE_HIGH
            channelName = context.getString(RemindType.getChannelName(type))
            remoteViews = RemoteViews(context.packageName,
                R.layout.notification_remind_normal).apply {
                setImageViewResource(R.id.iv_remind_logo, iconRes)
                setTextViewText(R.id.tv_remind_content, channelName)
                setTextViewText(R.id.tv_record, context.getString(R.string.health_record))
            }
            bigRemoteViews = RemoteViews(context.packageName,
                R.layout.notification_remind_big).apply {
                setImageViewResource(R.id.iv_remind_logo, R.drawable.ic_alarm_clock)
                setTextViewText(R.id.tv_remind_title, Date().formatHourMinute)
                setTextViewText(R.id.tv_remind_content, channelName)
                setTextViewText(R.id.tv_record, context.getString(R.string.health_record))
            }
        }
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(RemindType.getNotificationId(type),
            remindNotification)
    }

    private fun createMediaNotification(context: Context): Notification {
        return context.createNotification {
            isPermanent = true
            importance = IMPORTANCE_LOW
            iconRes = R.drawable.ic_alarm_clock
            channelId = "${context.packageName}$MEDIA_NOTIFICATION_CHANNEL_SUFFIX"
            channelName = "${context.packageName}$MEDIA_NOTIFICATION_CHANNEL_NAME_SUFFIX"
            val flag = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            val clickIntent = Intent(context, AlarmRemindActivity::class.java)
            val pendingIntent = TaskStackBuilder.create(context).run {
                addNextIntentWithParentStack(clickIntent)
                getPendingIntent(Activity.RESULT_OK, flag)
            }
            remoteViews = RemoteViews(context.packageName,
                R.layout.notification_search_normal).apply {
                setOnClickPendingIntent(R.id.tv_search, pendingIntent)
            }
            bigRemoteViews = RemoteViews(context.packageName,
                R.layout.notification_search_normal).apply {
                setOnClickPendingIntent(R.id.tv_search, pendingIntent)
            }
        }
    }

    fun startMediaForeground(service: Service, notifyId: Int) {
        service.startForeground(notifyId, createMediaNotification(service))
    }

    fun showMediaNotification(context: Context, notifyId: Int) {
        val searchNotification = createMediaNotification(context)
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(notifyId, searchNotification)
    }

    fun showNotification(context: Context, title: String, content: String, notifyId: Int) {
        val remindNotification = context.createNotification {
            this.title = title
            this.content = content
            this.iconRes = R.drawable.ic_alarm_clock
            this.importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
            this.remoteViews = RemoteViews(context.packageName,
                R.layout.notification_remind_normal).apply {
                setImageViewResource(R.id.iv_remind_logo, iconRes)
                setTextViewText(R.id.tv_remind_content, content)
                setTextViewText(R.id.tv_record, context.getString(R.string.health_record))
            }
            this.bigRemoteViews = RemoteViews(context.packageName,
                R.layout.notification_remind_big).apply {
                setImageViewResource(R.id.iv_remind_logo, iconRes)
                setTextViewText(R.id.tv_remind_title, Date().formatHourMinute)
                setTextViewText(R.id.tv_remind_content, content)
                setTextViewText(R.id.tv_record, context.getString(R.string.health_record))
            }
        }
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(notifyId, remindNotification)
    }
}
