package com.zero.health.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * @date:2025/12/29 12:15
 * @path:com.zero.health.service.MyNotificationListenerService
 */
class MyNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
    }

}