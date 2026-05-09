package com.zero.health.helper

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class HeartRateMonitorUtils private constructor() {
    // ------------------------------ 配置参数 ------------------------------
    /** 红色通道有效范围（0-1） */
    var redValidRange = 0.7..0.95

    /** 绿色通道有效范围（0-1） */
    var greenValidRange = 0.0002..0.085

    /** 蓝色通道有效范围（0-1） */
    var blueValidRange = 0.0001..0.15

    /** 色调有效范围1（0-10度） */
    var hueRange1 = 0.0..10.0

    /** 色调有效范围2（350-360度） */
    var hueRange2 = 350.0..360.0

    /** 连续无效帧数阈值（超过则判定手指移开） */
    var invalidFrameThreshold = 5

    /** 测量总时长（毫秒） */
    var measurementDuration = 10000L // 10秒

    // ------------------------------ 内部状态 ------------------------------
    private var isMeasuring = false
    private var isFingerOn = true
    private var invalidFrameCount = 0
    private var currentSecondHeartbeats = mutableListOf<Double>()
    private var startTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var callback: HeartRateMonitorCallback? = null

    // 心率计算内部状态
    private var lastBrightness = 0.0
    private var heartBeats = 0.0
    private var lastHeartBeatsTimestamp = 0.0
    private var showMaxOutsizeCount = 0
    private var showMinOutsizeCount = 0

    // 新增属性：手指移开恢复等待状态
    private var isWaitingForFingerRecovery = false

    companion object {
        @Volatile
        private var instance: HeartRateMonitorUtils? = null

        fun getInstance(): HeartRateMonitorUtils {
            return instance ?: synchronized(this) {
                instance ?: HeartRateMonitorUtils().also { instance = it }
            }
        }
    }

    // ------------------------------ 核心方法 ------------------------------
    fun startMeasurement(callback: HeartRateMonitorCallback) {
        this.callback = callback
        resetState()
        isMeasuring = true
        startTime = System.currentTimeMillis()
        lastBrightness = 0.0  // 重置亮度跟踪

        // 启动每秒回调计时器
        handler.postDelayed(measureTimerRunnable, 1000)
    }

    fun stopMeasurement() {
        isMeasuring = false
        handler.removeCallbacks(measureTimerRunnable)
        callback?.onMeasurementCompleted(calculateFinalAverage())
    }

    fun processPreviewFrame(yuvBytes: ByteArray, width: Int, height: Int) {
        if (!isMeasuring) return

        try {
            // 1. 解析中心区域像素的YUV值（降噪）
            val (yAvg, uAvg, vAvg) = getCenterYuvAverages(yuvBytes, width, height)

            // 2. YUV转RGB（0-255）
            val (red, green, blue) = yuvToRgb(yAvg, uAvg, vAvg)

            // 3. 归一化到0-1
            val redNorm = red / 255.0
            val greenNorm = green / 255.0
            val blueNorm = blue / 255.0

            // 4. 计算色调（0-360度）
            val hue = rgbToHue(redNorm, greenNorm, blueNorm)

            // 5. 检测手指是否在位
            val isColorValid = isColorInRange(redNorm, greenNorm, blueNorm)
            val isHueValid = isHueInRange(hue)
            val isFrameValid = isColorValid && isHueValid

            // 6. 更新手指状态与心率数据
            updateFingerState(isFrameValid)
            if (isFrameValid || isWaitingForFingerRecovery) {
                processValidFrame(redNorm, greenNorm, blueNorm, hue)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ------------------------------ 内部辅助方法 ------------------------------
    private fun resetState() {
        isMeasuring = false
        isFingerOn = true
        invalidFrameCount = 0
        currentSecondHeartbeats.clear()
        lastBrightness = 0.0
        heartBeats = 0.0
        lastHeartBeatsTimestamp = 0.0
        showMaxOutsizeCount = 0
        showMinOutsizeCount = 0
        handler.removeCallbacks(measureTimerRunnable)
    }

    fun getCenterYuvAverages(yuvBytes: ByteArray, width: Int, height: Int): Triple<Byte, Byte, Byte> {
        val centerX = width / 2
        val centerY = height / 2
        val halfSize = 1  // 3x3区域的半宽（可调整为2增大采样范围）

        var ySum = 0.0
        var uSum = 0.0
        var vSum = 0.0
        var count = 0

        for (yOffset in -halfSize..halfSize) {
            val y = centerY + yOffset
            if (y < 0 || y >= height) continue  // Y方向边界检查

            for (xOffset in -halfSize..halfSize) {
                val x = centerX + xOffset
                if (x < 0 || x >= width) continue  // X方向边界检查

                val yIndex = y * width + x
                val uvRowIndex = width * height + (y / 2) * width
                val uvColIndex = (x / 2) * 2

                // Y分量处理（0-255）
                ySum += (yuvBytes[yIndex].toInt() and 0xFF).toDouble()

                // UV分量处理（NV21格式：U和V交替存储，需要分开读取）
                if (y % 2 == 0 && x % 2 == 0) {  // 仅处理UV分量（隔行存储）
                    val u = (yuvBytes[uvRowIndex + uvColIndex].toInt() and 0xFF) - 128
                    val v = (yuvBytes[uvRowIndex + uvColIndex + 1].toInt() and 0xFF) - 128
                    uSum += u.toDouble()
                    vSum += v.toDouble()
                }
                count++
            }
        }

        return Triple(
            (ySum / count).toInt().toByte(),  // 显式转换为Int再转Byte
            (uSum / count).toInt().toByte(),  // 显式转换为Int再转Byte
            (vSum / count).toInt().toByte()   // 显式转换为Int再转Byte
        )
    }


    private fun yuvToRgb(y: Byte, u: Byte, v: Byte): Triple<Int, Int, Int> {
        val yNorm = (y.toInt() and 0xFF) / 255.0
        val uNorm = (u.toInt() and 0xFF - 128) / 255.0 * 2.0
        val vNorm = (v.toInt() and 0xFF - 128) / 255.0 * 2.0

        val r = (yNorm + 1.402 * vNorm).coerceIn(0.0, 1.0)
        val g = (yNorm - 0.344136 * uNorm - 0.714136 * vNorm).coerceIn(0.0, 1.0)
        val b = (yNorm + 1.772 * uNorm).coerceIn(0.0, 1.0)

        return Triple(
            (r * 255).toInt(),
            (g * 255).toInt(),
            (b * 255).toInt()
        )
    }

    private fun isColorInRange(red: Double, green: Double, blue: Double): Boolean {
        return red in redValidRange &&
                green in greenValidRange &&
                blue in blueValidRange
    }

    private fun isHueInRange(hue: Double): Boolean {
        return hue in hueRange1 || hue in hueRange2
    }

    private fun updateFingerState(isCurrentValid: Boolean) {
        if (isCurrentValid) {
            invalidFrameCount = 0
            isFingerOn = true
            isWaitingForFingerRecovery = false
        } else {
            invalidFrameCount++
            if (invalidFrameCount >= invalidFrameThreshold) {
                if (!isWaitingForFingerRecovery) {
                    isWaitingForFingerRecovery = true
                    // 可选：触发自定义回调（如提示用户调整手指）
                }
            }
        }
    }

    private fun processValidFrame(red: Double, green: Double, blue: Double, hue: Double) {
        val brightness = (red + green + blue) / 3.0
        calculate(brightness)
        if (heartBeats > 0) {
            currentSecondHeartbeats.add(heartBeats)
        }
    }

    private fun calculate(brightness: Double) {
        if (lastBrightness == 0.0) {
            lastBrightness = brightness
            return
        }
        val currentVector = brightness - lastBrightness
        lastBrightness = brightness

        if (isNeedStatistic) {
            statisticHeartBeats(currentVector)
        }
    }

    private fun statisticHeartBeats(heartRateVector: Double) {
        if (heartRateVector == 0.0) return

        val isNegative = heartRateVector < 0.0
        val absVector = abs(heartRateVector)
        val isStrong = absVector in 0.010..0.1000

        if (!isNegative || !isStrong) return

        val currentTime = System.currentTimeMillis() / 1000.0
        if (lastHeartBeatsTimestamp == 0.0) {
            lastHeartBeatsTimestamp = currentTime
            return
        }
        val timeDiff = currentTime - lastHeartBeatsTimestamp
        if (timeDiff <= 0.1) return

        val heartRate = 60.0 / timeDiff
        if (heartRate >= 180.0) {
            lastHeartBeatsTimestamp = 0.0
            return
        }

        when {
            heartRate > trustMaxValue -> handleOutlier(heartRate, isMax = true)
            heartRate < trustMinValue -> handleOutlier(heartRate, isMax = false)
            else -> updateHeartRate(heartRate, timeDiff)
        }
    }

    private fun handleOutlier(heartRate: Double, isMax: Boolean) {
        if (isMax) {
            if (++showMaxOutsizeCount >= trustCount) {
                updateHeartRate(heartRate, timeDiff = 0.0)
            }
        } else {
            if (++showMinOutsizeCount >= trustCount) {
                updateHeartRate(heartRate, timeDiff = 0.0)
            }
        }
    }

    private fun updateHeartRate(heartRate: Double, timeDiff: Double) {
        lastHeartBeatsTimestamp = System.currentTimeMillis() / 1000.0
        heartBeats = heartRate
    }

    private fun calculateFinalAverage(): Double {
        return currentSecondHeartbeats.average().coerceAtLeast(0.0)
    }

    // ------------------------------ 计时任务 ------------------------------
    private val measureTimerRunnable = object : Runnable {
        override fun run() {
            if (!isMeasuring) return

            val currentAverage = if (isFingerOn) {
                currentSecondHeartbeats.average().coerceAtLeast(0.0)
            } else {
                0.0  // 等待状态下不更新心率
            }

            callback?.onHeartRateUpdated(currentAverage)

            currentSecondHeartbeats.clear()

            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= measurementDuration) {
                stopMeasurement()
            } else {
                handler.postDelayed(this, 1000)
            }
        }
    }

    // ------------------------------ 回调接口 ------------------------------
    interface HeartRateMonitorCallback {
        fun onFingerMoved()
        fun onHeartRateUpdated(averageBpm: Double)
        fun onMeasurementCompleted(finalAverageBpm: Double)
    }

    // ------------------------------ 核心参数 ------------------------------
    private val trustMaxValue = 90.0  // 心率最大置信值（次/分钟）
    private val trustMinValue = 50.0  // 心率最小置信值（次/分钟）
    private val trustCount = 3        // 允许超出置信区的次数
    private val isNeedStatistic = true  // 是否启用统计逻辑

    // ------------------------------ 颜色转换工具 ------------------------------
    private fun rgbToHue(r: Double, g: Double, b: Double): Double {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        return when (max) {
            r -> {
                val d = (g - b) / delta
                if (g >= b) d * 60 else d * 60 + 360
            }

            g -> {
                (b - r) / delta * 60 + 120
            }

            b -> {
                (r - g) / delta * 60 + 240
            }

            else -> 0.0
        }.coerceIn(0.0, 360.0)
    }
}
