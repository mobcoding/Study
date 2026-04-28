package com.study.retention.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.study.retention.internal.RetentionEngine
import com.study.retention.internal.RetentionLog

internal class RetentionHeartbeatWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(RetentionLog.TAG, "WorkManager heartbeat started.")
        if (!RetentionEngine.ensureInitialized(applicationContext)) {
            Log.w(RetentionLog.TAG, "Skip WorkManager heartbeat because retention engine is disabled.")
            return Result.success()
        }
        return runCatching {
            RetentionEngine.handleHeartbeatWork(applicationContext)
            Log.d(RetentionLog.TAG, "WorkManager heartbeat finished.")
            Result.success()
        }.getOrElse { throwable ->
            Log.e(RetentionLog.TAG, "WorkManager heartbeat failed.", throwable)
            Result.retry()
        }
    }
}
