package com.zero.study.ui.activity

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.zero.base.activity.BaseActivity
import com.zero.study.databinding.ActivityCalendarReminderBinding
import com.zero.study.receiver.CalendarReminderReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class CalendarReminderActivity : BaseActivity<ActivityCalendarReminderBinding>(ActivityCalendarReminderBinding::inflate) {

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasRequiredPermissions()) {
            createCalendarReminder()
        } else {
            updateStatus("权限不足：需要日历读写权限；Android 13+ 还需要通知权限。")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            CalendarReminderReceiver.showNotification(
                this,
                "日历提醒测试通知",
                "这是不等待日历提醒时间的即时通知，用于验证通知权限和渠道。"
            )
        } else {
            updateStatus("通知权限未授予，无法发送测试通知。")
        }
    }

    override fun initView() {
        updateStatus("点击按钮会创建一个 1 分钟后的系统日历事件，并安排同一时间发送本应用通知。")
    }

    override fun initData() = Unit

    override fun addListener() {
        binding.btnCreateReminder.setOnClickListener {
            requestPermissionsThenCreateReminder()
        }
        binding.btnSendNow.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                CalendarReminderReceiver.showNotification(
                    this,
                    "日历提醒测试通知",
                    "这是不等待日历提醒时间的即时通知，用于验证通知权限和渠道。"
                )
            }
        }
    }

    private fun requestPermissionsThenCreateReminder() {
        if (hasRequiredPermissions()) {
            createCalendarReminder()
        } else {
            permissionLauncher.launch(requiredPermissions())
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.toTypedArray()
    }

    private fun createCalendarReminder() {
        runCatching {
            val calendarId = findWritableCalendarId()
            if (calendarId == null) {
                updateStatus("没有找到可写入的系统日历账号，请先在系统日历中添加账号。")
                return
            }

            val beginMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1)
            val endMillis = beginMillis + TimeUnit.MINUTES.toMillis(10)
            val title = "ZeroStudy 日历提醒示例"
            val content = "日历提醒已触发，同时由应用发送通知。"
            val eventId = insertCalendarEvent(calendarId, title, content, beginMillis, endMillis)
            insertCalendarReminder(eventId)
            scheduleNotification(eventId, title, content, beginMillis)

            val timeText = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(beginMillis))
            updateStatus("已创建日历事件 ID=$eventId，预计 $timeText 触发提醒和通知。")
            Toast.makeText(this, "已设置 1 分钟后的日历提醒", Toast.LENGTH_SHORT).show()
        }.onFailure { throwable ->
            updateStatus("创建日历提醒失败：${throwable.message ?: throwable.javaClass.simpleName}")
        }
    }

    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        val sortOrder = "${CalendarContract.Calendars.IS_PRIMARY} DESC"
        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    private fun insertCalendarEvent(
        calendarId: Long,
        title: String,
        content: String,
        beginMillis: Long,
        endMillis: Long
    ): Long {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, content)
            put(CalendarContract.Events.DTSTART, beginMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }
        val eventUri = requireNotNull(contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)) {
            "CalendarProvider insert event returned null"
        }
        return ContentUris.parseId(eventUri)
    }

    private fun insertCalendarReminder(eventId: Long) {
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, 0)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
    }

    private fun scheduleNotification(eventId: Long, title: String, content: String, triggerAtMillis: Long) {
        val intent = Intent(this, CalendarReminderReceiver::class.java).apply {
            putExtra(CalendarReminderReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(CalendarReminderReceiver.EXTRA_TITLE, title)
            putExtra(CalendarReminderReceiver.EXTRA_CONTENT, content)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun updateStatus(text: String) {
        binding.tvStatus.text = text
    }
}
