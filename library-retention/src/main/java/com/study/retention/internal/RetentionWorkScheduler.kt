package com.study.retention.internal

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.study.retention.worker.RetentionHeartbeatWorker
import java.util.concurrent.TimeUnit

internal object RetentionWorkScheduler {

    private const val UNIQUE_WORK_NAME = "library_retention_heartbeat"

    fun sync(context: Context, config: RetentionRuntimeConfig) {
        val appContext = context.applicationContext
        if (!config.runtime.workManagerEnabled) {
            WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_WORK_NAME)
            Log.d(RetentionLog.TAG, "WorkManager heartbeat disabled. uniqueWork=$UNIQUE_WORK_NAME")
            return
        }
        val request =
            PeriodicWorkRequestBuilder<RetentionHeartbeatWorker>(
                config.runtime.heartbeatIntervalMinutes.toLong(),
                TimeUnit.MINUTES,
            ).addTag(UNIQUE_WORK_NAME)
                .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        Log.d(
            RetentionLog.TAG,
            "WorkManager heartbeat scheduled. uniqueWork=$UNIQUE_WORK_NAME intervalMinutes=${config.runtime.heartbeatIntervalMinutes}",
        )
    }
}
