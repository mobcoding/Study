package com.zero.health.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zero.health.provider.HealthContextProvider
import com.zero.health.receiver.AlarmReceiver
import com.zero.health.ui.activity.AlarmRemindActivity
import java.util.Calendar

interface AlarmSetter {
    fun removeAlarm()
    fun setUpAlarm(id: Int, typeName: String, calendar: Calendar)
    fun fireNow(id: Int, typeName: String)
}

fun pendingIntentUpdateCurrentFlag(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }

}

class AlarmHelper(private val mContext: Context) : AlarmSetter {
    private val am = mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun removeAlarm() {
        val pendingAlarm = PendingIntent.getBroadcast(mContext, 1001, Intent(ACTION_FIRED).apply {
            setClass(mContext, AlarmReceiver::class.java)
        }, pendingIntentUpdateCurrentFlag())
        am.cancel(pendingAlarm)
    }

    override fun setUpAlarm(id: Int, typeName: String, calendar: Calendar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !am.canScheduleExactAlarms()) {
            return
        }
        val pendingAlarm = Intent(ACTION_FIRED).apply {
            setClass(mContext, AlarmReceiver::class.java)
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TYPE, typeName)
        }.let {
            PendingIntent.getBroadcast(mContext, 0, it, pendingIntentUpdateCurrentFlag())
        }
        // 2. Android 8.0+使用setAlarmClock，低版本使用setExactAndAllowWhileIdle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 显示在系统时钟界面的闹钟（更优先）
            val pendingShowList = PendingIntent.getActivity(mContext, 100500, Intent(mContext, AlarmRemindActivity::class.java), pendingIntentUpdateCurrentFlag())
            am.setAlarmClock(AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingShowList), pendingAlarm)
        } else {
            // Android 7.0-7.1：使用精确且允许低电量模式的API
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingAlarm)
        }
    }

    override fun fireNow(id: Int, typeName: String) {
        val intent = Intent(ACTION_FIRED).apply {
            setClass(mContext, AlarmReceiver::class.java)
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TYPE, typeName)
        }
        mContext.sendBroadcast(intent)
    }

    companion object {
        const val ACTION_FIRED = "ACTION_FIRED"
        const val EXTRA_ID = "ALARM.EXTRA_ID"
        const val EXTRA_TYPE = " ALARM.EXTRA_TYPE"
    }
}


