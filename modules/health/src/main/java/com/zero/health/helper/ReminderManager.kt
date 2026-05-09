package com.zero.health.helper

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class ReminderManager(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "定时提醒"
        val content = inputData.getString("content") ?: "这是一个每2分钟的提醒"
        val id = inputData.getInt("id", 1000)

        NotifyHelper.showNotification(context, title = title, content = content, notifyId = id)
        return Result.success()
    }

    companion object {

        fun scheduleReminders(context: Context, periodMinute: Long, title: String = "定时提醒",
                              content: String = "这是一个每2分钟的提醒", id: Int = 1000) {
            val data = Data.Builder().putString("title", title).putString("content",
                content).putInt("id", id).build()

            val workRequest = PeriodicWorkRequestBuilder<ReminderManager>(periodMinute,
                TimeUnit.MINUTES).setInputData(data).addTag("two_minute_reminder").build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork("two_minute_reminder",
                ExistingPeriodicWorkPolicy.REPLACE, workRequest)
        }

        fun cancelReminders(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("two_minute_reminder")
        }
    }
}
