package com.zero.health.bean

import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import com.zero.health.R

/**
 * @date:2025/8/7 11:48
 * @path:com.zero.health.bean.RemindType
 */

object RemindType {
    /**
     * 工具栏
     */
    const val HEALTH_TOOLS = 0

    /**
     * 血压
     */
    const val BLOOD_PRESSURE = 1

    /**
     * 心率
     */
    const val HEART_RATE = 2

    /**
     * 血糖
     */
    const val BLOOD_GLUCOSE = 3

    private const val TOOLS_NOTIFICATION_ID = 0x10011
    private const val ALARM_BLOOD_PRESSURE_NOTIFICATION_ID = 0x10012
    private const val ALARM_HEART_RATE_NOTIFICATION_ID = 0x10013
    private const val ALARM_BLOOD_GLUCOSE_NOTIFICATION_ID = 0x10014

    private const val CHANNEL_ID_TOOLS = "com.oasis.health.tools"
    private const val CHANNEL_ID_ALARM = "com.oasis.health.alarm"

    // 注解限制参数只能是以上常量
    @IntDef(HEALTH_TOOLS, BLOOD_PRESSURE, HEART_RATE, BLOOD_GLUCOSE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type


    fun getChannelId(@Type type: Int): String {
        return when (type) {
            BLOOD_PRESSURE, HEART_RATE, BLOOD_GLUCOSE -> CHANNEL_ID_ALARM
            HEALTH_TOOLS -> CHANNEL_ID_TOOLS
            else -> throw IllegalArgumentException("Invalid reminder type")
        }
    }

    @StringRes
    fun getChannelName(@Type type: Int): Int {
        return when (type) {
            BLOOD_PRESSURE, HEART_RATE, BLOOD_GLUCOSE -> R.string.health_alarm
            HEALTH_TOOLS -> R.string.health_tools
            else -> throw IllegalArgumentException("Invalid reminder type")
        }
    }

    @DrawableRes
    fun getTypeIcon(@Type type: Int): Int {
        return when (type) {
            BLOOD_PRESSURE -> R.drawable.ic_blood_pressure
            HEART_RATE -> R.drawable.ic_heart_rate
            BLOOD_GLUCOSE -> R.drawable.ic_blood_glucose
            HEALTH_TOOLS -> R.drawable.ic_alarm_clock
            else -> throw IllegalArgumentException("Invalid reminder type")
        }
    }

    @DrawableRes
    fun getSmallIcon(@Type type: Int): Int {
        return when (type) {
            BLOOD_PRESSURE, HEART_RATE, BLOOD_GLUCOSE, HEALTH_TOOLS -> R.drawable.ic_heart_rate
            else -> throw IllegalArgumentException("Invalid reminder type")
        }
    }


    fun getGroupKey(@Type type: Int): String {
        return when (type) {
            BLOOD_PRESSURE, HEART_RATE, BLOOD_GLUCOSE -> "TOOLS_GROUP"
            HEALTH_TOOLS -> "ALARM_GROUP"
            else -> throw IllegalArgumentException("Invalid reminder type")
        }
    }

    fun getNotificationId(@Type type: Int): Int {
        return when (type) {
            BLOOD_PRESSURE -> ALARM_BLOOD_PRESSURE_NOTIFICATION_ID
            HEART_RATE -> ALARM_HEART_RATE_NOTIFICATION_ID
            BLOOD_GLUCOSE -> ALARM_BLOOD_GLUCOSE_NOTIFICATION_ID
            HEALTH_TOOLS -> TOOLS_NOTIFICATION_ID
            else -> throw IllegalArgumentException("Invalid reminder type")
        }
    }
}