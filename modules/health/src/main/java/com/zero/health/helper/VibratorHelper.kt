package com.zero.health.helper

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * @date:2025/7/25 16:57
 * @path:com.zero.study.service.Vibrator
 */
class VibratorHelper(context: Context) : CoroutineScope {
    private val job = Job()
    private var vibrationJob: Job? = null
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // 开始周期性震动
    fun schedule(duration: Long = 100L, interval: Long = 1000L, strength: Int = 255) {
        if (!vibrator.hasVibrator()) return
        vibrationJob = launch {
            while (isActive) {
                vibrate(duration, strength)
                delay(interval)
            }
        }
    }

    // 停止震动
    fun stop() {
        vibrationJob?.cancel()
        vibrationJob = null
        vibrator.cancel()
    }

    /**
     * 选择震动
     */
    fun select() {
        vibrate(18L, 235)
    }

    /**
     * 心率震动
     */
    fun heartRate() {
        vibrate(50L, 180)
    }

    private fun vibrate(mills: Long, strength: Int) {
        if (!vibrator.hasVibrator()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = if (strength in 1..255) strength else VibrationEffect.DEFAULT_AMPLITUDE
            val effect = VibrationEffect.createOneShot(mills, amplitude)
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(mills)
        }
    }

    fun destroy() {
        stop()
        job.cancel()
    }
}