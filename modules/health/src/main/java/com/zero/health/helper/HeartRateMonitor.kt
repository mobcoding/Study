package com.zero.health.helper

import android.os.Handler
import android.os.Looper
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// 数据类：封装颜色和亮度信息
data class ColorInfo(
    val brightness: Double,
    val red: Double,
    val green: Double,
    val blue: Double,
    val hue: Double
)

// 心率监测回调接口
interface HeartRateMonitorListener {
    /** 每秒回调当前心率平均值 */
    fun onHeartRateUpdated(avgHeartRate: Double)

    /** 手指移开摄像头时回调 */
    fun onFingerRemoved()

    /** 10秒测量完成后回调 */
    fun onMeasurementCompleted(finalAvgHeartRate: Double)
}

class HeartRateMonitor private constructor() {
    // 单例实例
    companion object {
        @Volatile
        private var instance: HeartRateMonitor? = null

        fun getInstance(): HeartRateMonitor {
            return instance ?: synchronized(this) {
                instance ?: HeartRateMonitor().also { instance = it }
            }
        }
    }

    // 回调监听器
    var listener: HeartRateMonitorListener? = null

    // 测量状态
    private var isMeasuring = false
    private var measurementStartTime: Long = 0
    private val totalMeasurementDuration: Long = 10 * 1000 // 总测量时间10秒(毫秒)
    private val heartRateReadings = mutableListOf<Double>()

    // 心率计算相关属性
    private var lastBrightness = 0.0
    private val trustMaxValue = 90.0  // 最大可信心率
    private val trustMinValue = 50.0   // 最小可信心率
    private val trustCount = 3         // 允许超出置信区间的次数
    private var showMaxOutsizeCount = 0
    private var showMinOutsizeCount = 0
    private var isNeedStatistic = true
    private var lastHeartBeatsTimestamp = 0.0
    private var currentHeartRate = 0.0

    // 主线程Handler，用于定时器回调
    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isMeasuring) return

            // 检查是否已完成测量
            val elapsedTime = System.currentTimeMillis() - measurementStartTime
            if (elapsedTime >= totalMeasurementDuration) {
                finishMeasurement()
                return
            }

            // 计算并回调当前平均心率
            val average = calculateAverageHeartRate()
            listener?.onHeartRateUpdated(average)

            // 继续定时
            mainHandler.postDelayed(this, 1000)
        }
    }

    /** 开始心率测量 */
    fun startMeasurement() {
        resetMeasurementState()
        isMeasuring = true
        measurementStartTime = System.currentTimeMillis()

        // 开始每秒回调
        mainHandler.post(updateRunnable)
    }

    /** 停止心率测量 */
    fun stopMeasurement() {
        isMeasuring = false
        mainHandler.removeCallbacks(updateRunnable)
        resetMeasurementState()
    }

    /**
     * 处理相机预览的字节数组
     * @param byteArray 相机捕获的字节数组(BGRA格式)
     * @param width 图像宽度
     * @param height 图像高度
     */
    fun processCameraFrame(byteArray: ByteArray, width: Int, height: Int) {
        if (!isMeasuring) return

        // 从字节数组提取颜色信息
        val colorInfo = extractColorInfo(
            byteArray = byteArray,
            width = width,
            height = height
        )

        // 检查手指是否在摄像头前
        val isFingerPresent = checkFingerPresence(
            red = colorInfo.red,
            green = colorInfo.green,
            blue = colorInfo.blue,
            hue = colorInfo.hue
        )
        if (!isFingerPresent) {
            listener?.onFingerRemoved()
            return
        }

        // 计算心率
        calculateHeartRate(brightness = colorInfo.brightness)

        // 记录有效的心率值
        if (currentHeartRate > 0) {
            heartRateReadings.add(currentHeartRate)
        }
    }

    /** 从字节数组提取颜色和亮度信息 */
    private fun extractColorInfo(
        byteArray: ByteArray,
        width: Int,
        height: Int
    ): ColorInfo {
        // 计算中心区域，只处理中心部分提高效率
        val centerX = width / 2
        val centerY = height / 2
        val regionSize = 40 // 取中心40x40的区域
        val startX = max(0, centerX - regionSize / 2)
        val startY = max(0, centerY - regionSize / 2)
        val endX = min(width, startX + regionSize)
        val endY = min(height, startY + regionSize)

        // 计算区域内的平均颜色值
        var totalRed = 0.0
        var totalGreen = 0.0
        var totalBlue = 0.0
        var totalBrightness = 0.0
        var pixelCount = 0

        // 假设字节数组是BGRA格式，每个像素占4个字节
        for (y in startY until endY) {
            for (x in startX until endX) {
                val index = (y * width + x) * 4
                if (index + 3 >= byteArray.size) break

                val blue = byteArray[index].toDouble() / 255.0
                val green = byteArray[index + 1].toDouble() / 255.0
                val red = byteArray[index + 2].toDouble() / 255.0

                // 计算亮度（灰度值）
                val brightness = 0.299 * red + 0.587 * green + 0.114 * blue

                totalRed += red
                totalGreen += green
                totalBlue += blue
                totalBrightness += brightness
                pixelCount++
            }
        }

        // 计算平均值
        val avgRed = if (pixelCount > 0) totalRed / pixelCount else 0.0
        val avgGreen = if (pixelCount > 0) totalGreen / pixelCount else 0.0
        val avgBlue = if (pixelCount > 0) totalBlue / pixelCount else 0.0
        val avgBrightness = if (pixelCount > 0) totalBrightness / pixelCount else 0.0

        // 计算色调值
        val hue = calculateHue(avgRed, avgGreen, avgBlue)

        return ColorInfo(avgBrightness, avgRed, avgGreen, avgBlue, hue)
    }

    /** 计算色调值 */
    private fun calculateHue(red: Double, green: Double, blue: Double): Double {
        val maxVal = maxOf(red, green, blue)
        val minVal = minOf(red, green, blue)
        val delta = maxVal - minVal

        if (delta <= 0) return 0.0 // 灰色，无色调

        var hue = 0.0

        when (maxVal) {
            red -> hue = (green - blue) / delta % 6
            green -> hue = (blue - red) / delta + 2
            blue -> hue = (red - green) / delta + 4
        }

        hue *= 60
        return if (hue < 0) hue + 360 else hue
    }

    /** 检查手指是否在摄像头前 */
    private fun checkFingerPresence(red: Double, green: Double, blue: Double, hue: Double): Boolean {
        // 颜色范围判断
        var isPassOfColor = true
        if (red >= 0.95 || red <= 0.7) {
            isPassOfColor = false
        }
        if (green >= 0.08500 || green <= 0.000200) {
            isPassOfColor = false
        }
        if (blue >= 0.15000 || blue <= 0.000100) {
            isPassOfColor = false
        }

        // 色调范围判断
        val isAllowOfMax = hue >= 350 && hue <= 360
        val isAllowOfMin = hue > 0.0 && hue < 10.0

        return (isAllowOfMax || isAllowOfMin) && isPassOfColor
    }

    /** 计算心率 */
    private fun calculateHeartRate(brightness: Double) {
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

    /** 统计心跳 */
    private fun statisticHeartBeats(heartRateVector: Double) {
        if (heartRateVector == 0.0) {
            return
        }

        // 判断是否为有效的心跳信号
        val isNegativeNumber = heartRateVector < 0.0
        val heartRateAbsolute = abs(heartRateVector)
        val isStrong = heartRateAbsolute > 0.010 && heartRateAbsolute < 0.1000

        if (!isNegativeNumber || !isStrong) {
            return
        }

        if (lastHeartBeatsTimestamp == 0.0) {
            lastHeartBeatsTimestamp = System.currentTimeMillis().toDouble() / 1000.0
            return
        }

        val currentTimestamp = System.currentTimeMillis().toDouble() / 1000.0
        val timeDiff = currentTimestamp - lastHeartBeatsTimestamp

        // 过滤时间间隔太短的情况
        if (timeDiff <= 0.1) {
            return
        }

        // 计算心率 (每分钟心跳次数)
        val heartBeatsRate = 60 * (1 / timeDiff)

        // 过滤异常值
        if (heartBeatsRate >= 180) {
            lastHeartBeatsTimestamp = 0.0
            return
        }

        // 处理超出置信区间的情况
        if (heartBeatsRate > trustMaxValue && showMaxOutsizeCount < trustCount) {
            lastHeartBeatsTimestamp = 0.0
            showMaxOutsizeCount++
            return
        }

        if (heartBeatsRate < trustMinValue && showMinOutsizeCount < trustCount) {
            lastHeartBeatsTimestamp = 0.0
            showMinOutsizeCount++
            return
        }

        // 更新心率值
        if (heartBeatsRate >= 0 && heartBeatsRate <= 10000) {
            lastHeartBeatsTimestamp = currentTimestamp
            currentHeartRate = heartBeatsRate
        }
    }

    /** 计算平均心率 */
    private fun calculateAverageHeartRate(): Double {
        if (heartRateReadings.isEmpty()) return 0.0

        val sum = heartRateReadings.sum()
        return sum / heartRateReadings.size
    }

    /** 完成测量 */
    private fun finishMeasurement() {
        isMeasuring = false
        mainHandler.removeCallbacks(updateRunnable)

        val finalAverage = calculateAverageHeartRate()
        listener?.onMeasurementCompleted(finalAverage)
    }

    /** 重置测量状态 */
    private fun resetMeasurementState() {
        heartRateReadings.clear()
        lastBrightness = 0.0
        lastHeartBeatsTimestamp = 0.0
        currentHeartRate = 0.0
        showMaxOutsizeCount = 0
        showMinOutsizeCount = 0
        isNeedStatistic = true
    }
}

