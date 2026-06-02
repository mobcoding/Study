package com.rebuild.mixtube.ads

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.util.Locale
import java.util.UUID

object Tracking {
    private const val TAG = "Tracking"
    private const val FILE_NAME = "tracking.log"
    private val gson = Gson()
    private var sessionId: String? = null
    private var cachedEnv: Map<String, Any?>? = null

    fun initSession(): String {
        val id = UUID.randomUUID().toString()
        sessionId = id
        return id
    }

    fun log(context: Context, event: String, props: Map<String, Any?> = emptyMap()) {
        val env = cachedEnv ?: buildEnv(context).also { cachedEnv = it }
        val payload = linkedMapOf<String, Any?>(
            "ts" to System.currentTimeMillis(),
            "sid" to (sessionId ?: initSession()),
            "event" to event
        )
        payload.putAll(env)
        payload.putAll(props)
        val line = gson.toJson(payload)
        Log.d(TAG, line)
        runCatching {
            val file = File(context.filesDir, FILE_NAME)
            file.appendText(line + "\n")
        }
    }

    private fun buildEnv(context: Context): Map<String, Any?> {
        val pm = context.packageManager
        val pkg = context.packageName
        val pInfo = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull()
        val verName = pInfo?.versionName
        val verCode = if (Build.VERSION.SDK_INT >= 28) pInfo?.longVersionCode else pInfo?.versionCode?.toLong()
        val firstInstallTime = pInfo?.firstInstallTime
        val lastUpdateTime = pInfo?.lastUpdateTime

        val locale = Locale.getDefault()
        return linkedMapOf(
            "pkg" to pkg,
            "verName" to verName,
            "verCode" to verCode,
            "firstInstallTime" to firstInstallTime,
            "lastUpdateTime" to lastUpdateTime,
            "country" to locale.country,
            "lang" to locale.language,
            "sdkInt" to Build.VERSION.SDK_INT,
            "manufacturer" to Build.MANUFACTURER,
            "brand" to Build.BRAND,
            "model" to Build.MODEL,
            "device" to Build.DEVICE,
        )
    }
}
