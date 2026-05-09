package com.zero.health.helper

import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import androidx.core.util.Pair
import com.zero.health.entity.HeartRateEntity
import java.sql.Date
import java.util.Arrays
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class HeartRateHandler(private val listener: com.zero.health.listener.BpmListener?) {
    private val maxValue = 36

    private val processing = AtomicBoolean(false)
    private var averageIndex = 0
    private val averageArray = IntArray(4)

    enum class TYPE {
        GREEN, RED
    }

    //设置默认类型
    private var current: TYPE = TYPE.GREEN

    // 上次处理完成的时间
    private var lastHandleTime: Long = 0

    private var error = 0

    //开始时间
    private var firstBeatTime: Long = 0

    // 心跳波峰时间
    private val beatStamps = ArrayList<Long>()

    // 心跳时间间隔
    private val intervals = ArrayList<Long>()

    private val collectionList = ArrayList<Int>()

    private var roundStartTime: Long = 0

    private var roundBeats = 0

    private var startTime: Long = 0

    private var previousHsv1 = 0.0f // 上一帧的饱和度
    private var previousHsv2 = 0.0f // 上一帧的亮度

    private var lastDynamicSignalTime: Long = 0 // 上次检测到动态信号的时间戳

    // 新增：连续无效帧计数器
    private var consecutiveInvalidFrames = 0
    private val MAX_CONSECUTIVE_INVALID_FRAMES = 10 // 连续10帧无效就认为手指离开
    
    // 新增：超时检测
    private var lastValidSignalTime: Long = 0
    private val FINGER_OUT_TIMEOUT_MS: Long = 3000 // 3秒没有有效信号就认为手指离开

    fun handleCamera(data: ByteArray?, width: Int, height: Int) {
        if (data == null) throw NullPointerException()
        if (!processing.compareAndSet(false, true)) return
        val currentTime = SystemClock.elapsedRealtime()

        // 新增：超时检测
        if (lastValidSignalTime > 0 && currentTime - lastValidSignalTime > FINGER_OUT_TIMEOUT_MS) {
            onFingerOut(3) // 3表示超时
            processing.set(false)
            return
        }

        if (currentTime - lastHandleTime < 30) { // 从50ms减少到30ms
            processing.set(false)
            return
        }
        lastHandleTime = currentTime

        //图像处理
        val rgb = decodeRgb(data.clone(), width, height)
        if (rgb[0] < 120) {
            consecutiveInvalidFrames++
            if (consecutiveInvalidFrames >= MAX_CONSECUTIVE_INVALID_FRAMES) {
                onFingerOut(1)
            }
            processing.set(false)
            return
        }

        // hsv[0] red 红色分量值
        val hsv = floatArrayOf(0.0f, 0.0f, 0.0f)
        Color.RGBToHSV(rgb[0], rgb[1], rgb[2], hsv)


        // 判断红色的 HSV 范围
        var isRedInRange = (hsv[0] in 0.0f..30.0f) || (hsv[0] in 330.0f..360.0f)

        // 判断亮度和饱和度是否满足阈值
        val isBrightnessAndSaturationValid = (hsv[1] + hsv[2] > 1.0f && rgb[0] > 5.0f)

        // 新增：检测动态波动，过滤静态物体
        val brightnessChange = abs((hsv[2] - previousHsv2).toDouble()).toFloat()
        val saturationChange = abs((hsv[1] - previousHsv1).toDouble()).toFloat()

        // 更新上一帧的值
        previousHsv1 = hsv[1]
        previousHsv2 = hsv[2]


        // 如果亮度和饱和度的波动太小，判断为静止
        if (brightnessChange < MIN_BRIGHTNESS_CHANGE && saturationChange < MIN_SATURATION_CHANGE) {
            // 检查是否超过容忍时间窗口
            if (currentTime - lastDynamicSignalTime > MAX_STATIC_TOLERANCE_MS) {
                isRedInRange = false // 过滤静态信号
            }
        } else {
            // 如果有明显动态信号，更新最后动态信号时间
            lastDynamicSignalTime = currentTime
        }

        // 只有同时满足 isRedInRange 和 isBrightnessAndSaturationValid，才认为是有效信号
        if (!isRedInRange || !isBrightnessAndSaturationValid) {
            consecutiveInvalidFrames++
            if (consecutiveInvalidFrames >= MAX_CONSECUTIVE_INVALID_FRAMES) {
                onFingerOut(1)
            }
            processing.set(false)
            return
        }

        // 重置连续无效帧计数器
        consecutiveInvalidFrames = 0

        // 更新最后有效信号时间
        lastValidSignalTime = currentTime


        //计算平均值
        var averageArrayAvg = 0
        var averageArrayCnt = 0
        for (j in averageArray) {
            if (j > 0) {
                averageArrayAvg += j
                averageArrayCnt++
            }
        }

        //计算平均值
        val rollingAverage = if ((averageArrayCnt > 0)) (averageArrayAvg / averageArrayCnt) else 0

        var newType = current
        if (rgb[0] < rollingAverage) {
            newType = TYPE.RED
            if (newType != current) {
                if (firstBeatTime <= 0) {
                    firstBeatTime = currentTime
                    beatStamps.clear()
                } else {
                    if (beatStamps.isEmpty()) {
                        intervals.add((currentTime - firstBeatTime))
                        beatStamps.add(currentTime)
                    } else {
                        intervals.add((currentTime - beatStamps[beatStamps.size - 1]))
                        beatStamps.add(currentTime)
                    }
                    onProgress()
                    roundBeats++
                }
            }
        } else {
            if (rgb[0] > rollingAverage) {
                newType = TYPE.GREEN
            }
        }

        if (averageIndex == averageArray.size) {
            averageIndex = 0
        }
        averageArray[averageIndex] = rgb[0]
        averageIndex++

        if (newType != current) {
            current = newType
        }

        if (roundStartTime == 0L) {
            roundStartTime = SystemClock.elapsedRealtime()
        }

        val roundSeconds = (currentTime - roundStartTime) / 1000.0
        if (roundSeconds >= 2.0) {
            val bps = roundBeats / roundSeconds
            val bpm = (bps * 60.0).toInt()
            if (bpm < 30 || bpm > 180) {
                Log.e("zzz", "bpm is error ---> $bpm")
                if (error < 10) {
                    error++
                    for (i in 0 until roundBeats) {
                        if (beatStamps.isNotEmpty()) {
                            beatStamps.removeAt(beatStamps.size - 1)
                        }
                    }
                    roundBeats = 0
                    roundStartTime = SystemClock.elapsedRealtime()
                } else {
                    onFingerOut(2)
                }
                processing.set(false)
                return
            }
            error = 0
            roundBeats = 0
            roundStartTime = SystemClock.elapsedRealtime()
            onRound()
        }

        processing.set(false)
    }

    private fun onRound() {
        val values = FloatArray(intervals.size)
        for (i in intervals.indices) {
            values[i] = intervals[i].toFloat()
        }
        val averBpmAndStress = getAverageBpm(values)
        listener!!.onBpm(averBpmAndStress[0].toInt(), averBpmAndStress[1].toInt())
        collectionList.add(averBpmAndStress[0].toInt())
        onProgress()
    }

    private fun onRestart() {
        val progress = beatStamps.size * 100.0f / maxValue
        if (progress >= 90) {
            if (listener != null) {
                listener.onProgress(100)
                listener.onFinish(intervals)
            }
            return
        }

        Arrays.fill(averageArray, 0)
        averageIndex = 0
        collectionList.clear()
        startTime = SystemClock.elapsedRealtime()
        firstBeatTime = 0
        roundBeats = 0
        roundStartTime = SystemClock.elapsedRealtime()
        //beats心跳总数
        error = 0
        beatStamps.clear()
        intervals.clear()

        // 重置连续无效帧计数器
        consecutiveInvalidFrames = 0

        // 重置超时检测
        lastValidSignalTime = 0

        if (listener != null) {
            listener.onBpm(0, 0)
            listener.onProgress(0)
        }
    }


    private fun onFingerOut(scene: Int) {
        listener?.onFingerOut(scene)
        onRestart()
    }

    private fun onProgress() {
        if (listener != null) {
            val progress = (beatStamps.size * 100.0f / maxValue).toInt()
            listener.onProgress(min(progress.toDouble(), 100.0).toInt())
            if (progress >= 100.0f) {
                listener.onFinish(intervals)
            }
        }
    }

    fun startHandle() {
        startTime = SystemClock.elapsedRealtime()
    }

    fun stopHandle() {
        this.startTime = SystemClock.elapsedRealtime()
    }

    fun getAverageBpm(rrStream: FloatArray): FloatArray {
        if (rrStream.size <= 2) {
            return floatArrayOf(75f, 0f)
        }
        try {
            val height = getUserHeight(180.0f, 0)
            val weight = getUserWeight(84.0f, 0)
            val refinedStream = refineStream(rrStream)
            val differences = getArrayDifferences(refinedStream)
            val amo = generateAmo(refinedStream)
            val pnn50 = generatePnn50(differences)
            val medsd = generateMedsd(differences)
            val nBpm = generateNBpm(height, weight)
            val bpm = generateBpm(refinedStream)
            val sdnn = generateSdnn(refinedStream)
            val refinedSdnn = u(sdnn, bpm, nBpm)
            val stress = generateStress(amo, pnn50, medsd, nBpm, bpm)
            val energy = generateEnergy(refinedSdnn, medsd)
            Log.e("Blood", "Bpm =$bpm,,stress =$stress,energy = $energy, hrv = $sdnn")
            return floatArrayOf(bpm, stress)
        } catch (t: Throwable) {
            return floatArrayOf(75f, 0f)
        }
    }

    private fun generateMeanrr(rrFiltered: FloatArray): Float {
        var f6 = 0.0f
        for (f7 in rrFiltered) {
            f6 += f7
        }
        return f6 / rrFiltered.size
    }

    private fun generateEnergy(sdnnHrCorrected: Float, medsd: Float): Float {
        var f6 = 100.0f
        var f7 = 1.0f
        if (sdnnHrCorrected > 1.0f) {
            f7 = if (sdnnHrCorrected > 140.0f) {
                50.0f
            } else if (sdnnHrCorrected <= 19.0f) {
                20.0f - ((19.0f - sdnnHrCorrected) * 1.0555556f)
            } else if (sdnnHrCorrected < 69.9f) {
                (sdnnHrCorrected - 19.0f) * 1.3752456f + 20.0f
            } else if (sdnnHrCorrected < 78.9f) {
                90.0f + ((sdnnHrCorrected - 69.9f) * 1.1111112f)
            } else if (sdnnHrCorrected < 99.4f) {
                100.0f - ((sdnnHrCorrected - 78.9f) * 0.9756098f)
            } else if (sdnnHrCorrected <= 140.0f) {
                80.0f - ((sdnnHrCorrected - 99.4f) * 0.7389163f)
            } else {
                0.0f
            }
        }
        if (medsd < 12.0f) {
            f6 = 20.0f - (((12.0f - medsd) * 19.0f) / 11.0f)
        } else if (medsd <= 42.0f) {
            f6 = (((medsd - 12.0f) * 80.0f) / 30.0f) + 20.0f
        } else if (medsd >= 76.0f) {
            f6 = if (medsd < 86.0f) {
                100.0f - (((medsd - 76.0f) * 50.0f) / 10.0f)
            } else {
                50.0f
            }
        }
        var abs = abs((sdnnHrCorrected - 48.0f).toDouble()).toFloat()
        var abs2 = (abs((medsd - 26.0f).toDouble()) * 1.5f).toFloat()
        if (abs < 7.0f && abs2 < 7.0f) {
            abs = 0.5f
            abs2 = 0.5f
        }
        val f8 = abs + abs2
        if (abs / f8 > 0.8f) {
            abs = 0.8f
            abs2 = 0.2f
        } else if (abs2 / f8 > 0.8f) {
            abs = 0.2f
            abs2 = 0.8f
        }
        return ((f7 * abs2) + (f6 * abs)) / (abs + abs2)
    }

    private fun u(sdnn: Float, bpm: Float, nbpm: Float): Float {
        if (sdnn == 0.0f || bpm == 0.0f || nbpm == 0.0f) {
            return 0.0f
        }
        val f6 = nbpm - 10.0f
        if (bpm < f6) {
            return (sdnn / exp(0.17006802558898926)).toFloat()
        }
        if (f6 <= bpm && bpm <= 10.0f + nbpm) {
            return (sdnn / exp(((-(bpm - nbpm)) / 58.8f).toDouble())).toFloat()
        }
        return (sdnn / exp(-0.17006802558898926)).toFloat()
    }

    private fun generateRmssd(rrDiff: FloatArray?): Float {
        var f6 = 0.0f
        if (rrDiff == null || rrDiff.isEmpty()) {
            return 0.0f
        }
        for (f7 in rrDiff) {
            f6 += f7.pow(2.0f)
        }
        return sqrt((f6 / rrDiff.size).toDouble()).toFloat()
    }

    private fun generateSdnn(floats: FloatArray?): Float {
        var f6 = 0.0f
        if (floats == null || floats.size < 2) {
            return 0.0f
        }
        var f7 = 0.0f
        for (f8 in floats) {
            f7 += f8
        }
        val length = f7 / floats.size
        for (f9 in floats) {
            f6 = (f6 + (f9 - length).pow(2.0f))
        }
        return sqrt((f6 / (floats.size - 1)).toDouble()).toFloat()
    }

    private fun getUserHeight(userHeight: Float, userGender: Int): Float {
        if (userHeight in 100.0f..300.0f) {
            return userHeight
        }
        if (userGender == 0) {
            return 180.0f
        }
        return 165.0f
    }

    private fun getUserWeight(userWeight: Float, userGender: Int): Float {
        if (userWeight <= 250.0f && userWeight >= 30.0f) {
            return userWeight
        }
        if (userGender == 0) {
            return 84.0f
        }
        return 68.0f
    }

    private fun refineStream(rrStream: FloatArray): FloatArray {
        val refined = removeOutiers(rrStream, 200.0f, 2000.0f)
        val round1Size = refined.size
        Log.e("Blood", "skip some values1: " + (rrStream.size - round1Size))
        return refined
    }

    private fun removeOutiers(arr: FloatArray, lower: Float, upper: Float): FloatArray {
        val fArr = FloatArray(arr.size)
        var index = 0
        for (value in arr) {
            if (value >= lower && value <= upper) {
                fArr[index] = value
                index++
            }
        }
        return fArr.copyOf(index)
    }

    private fun h(rrStream: FloatArray): Pair<Float, Float> {
        try {
            val w5 = getUserHeight(180.0f, 0)
            val x5 = getUserWeight(84.0f, 0)
            val r5 = refineStream(rrStream)
            //float[] r5 = rrStream;
            val q6 = getArrayDifferences(r5)
            val a6 = generateAmo(r5)
            val n6 = generatePnn50(q6)
            val l6 = generateMedsd(q6)
            val m6 = generateNBpm(w5, x5)
            val f6 = generateBpm(r5)
            return Pair(f6, generateStress(a6, n6, l6, m6, f6))
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(75.0f, 25.0f)
        }
    }

    private fun generateMedsd(rrDiff: FloatArray): Float {
        return Math.round(getMiddleValue(absArrayValues(rrDiff))).toFloat()
    }

    private fun generateStress(amo: Float, pnn50: Float, medsd: Float, nbpm: Float, bpm: Float): Float {
        var f6: Float
        var f7 = 0f
        var f8 = 0f
        val f9: Float
        var f10 = 0f
        var f11 = 90.0f
        var f12 = 1.0f
        if (medsd < 9.0f) {
            f6 = 100.0f
        } else {
            if (medsd <= 12.0f) {
                f7 = (medsd - 9.0f) * 3.0f
                f8 = 99.0f
            } else if (medsd <= 20.0f) {
                f6 = 90.0f - ((medsd - 12.0f) * 3.125f)
            } else if (medsd <= 27.0f) {
                f6 = 65.0f - ((medsd - 20.0f) * 2.857143f)
            } else if (medsd <= 37.0f) {
                f7 = (medsd - 27.0f) * 2.0f
                f8 = 45.0f
            } else if (medsd <= 46.0f) {
                f6 = 25.0f - ((medsd - 37.0f) * 1.1111112f)
            } else if (medsd <= 54.0f) {
                f6 = 15.0f - ((medsd - 46.0f) * 1.75f)
            } else {
                f6 = 1.0f
            }
            f6 = f8 - f7
        }
        if (pnn50 <= 1.0f) {
            f9 = 96.0f
        } else if (pnn50 <= 2.0f) {
            f9 = 93.0f
        } else if (pnn50 <= 5.0f) {
            f9 = 90.0f - ((pnn50 - 2.0f) * 8.333333f)
        } else {
            val d6 = pnn50.toDouble()
            f9 = if (d6 <= 22.2) {
                65.0f - ((pnn50 - 5.0f) * 1.1046511f)
            } else if (d6 <= 34.4) {
                40.0f - ((pnn50 - 22.2f) * 1.147541f)
            } else if (pnn50 <= 45.5f) {
                20.0f - ((pnn50 - 34.4f) * 1.1711712f)
            } else {
                1.0f
            }
        }
        if (amo >= 18.0f) {
            if (amo < 24.0f) {
                f12 = ((amo - 18.0f) * 2.0f) + 3.0f
            } else if (amo < 36.0f) {
                f12 = ((amo - 24.0f) * 2.0833333f) + 15.0f
            } else if (amo <= 57.0f) {
                f12 = ((amo - 36.0f) * 1.4285715f) + 40.0f
            } else {
                if (amo <= 60.0f) {
                    f10 = (amo - 57.0f) * 3.3333333f
                    f11 = 70.0f
                } else if (amo <= 65.0f) {
                    f10 = (amo - 60.0f) * 2.0f
                    f11 = 80.0f
                } else if (amo <= 68.0f) {
                    f10 = (amo - 65.0f) * 3.3333333f
                } else {
                    f12 = 100.0f
                }
                f12 = f10 + f11
            }
        }
        val f13 = ((f6 + f9) + f12) / 3.0f
        if (f13 > 20.0f) {
            return f13
        }
        if (bpm >= nbpm) {
            return min(((bpm - nbpm) + 21.0f).toDouble(), 100.0).toFloat()
        }
        val f14 = nbpm - 2.0f
        if (bpm >= f14) {
            return 20.3f
        }
        if (bpm >= f14) {
            return 25.0f
        }
        return f13
    }

    private fun generateBpm(rrFiltered: FloatArray): Float {
        var count = 0.0f
        for (interval in rrFiltered) {
            count += 60000.0f / interval
        }
        return count / rrFiltered.size
    }

    private fun generateNBpm(userHeightCorrected: Float, userWeightCorrected: Float): Float {
        return (userHeightCorrected / userWeightCorrected).pow(0.33f) * 48.0f
    }

    private fun getMiddleValue(arr: FloatArray): Float {
        val copyOf = arr.copyOf(arr.size)
        Arrays.sort(copyOf)
        val length = copyOf.size / 2
        if (copyOf.size % 2 == 0) {
            return (copyOf[length - 1] + copyOf[length]) / 2.0f
        }
        return copyOf[length]
    }

    private fun getArrayDifferences(rrFiltered: FloatArray): FloatArray {
        val length = rrFiltered.size - 1
        val fArr = FloatArray(length)
        var index = 0
        while (index < length) {
            val next = index + 1
            fArr[index] = rrFiltered[next] - rrFiltered[index]
            index = next
        }
        return fArr
    }

    private fun generateAmo(rrFiltered: FloatArray): Float {
        val hashMap = HashMap<Int, Int>()
        for (value in rrFiltered) {
            val floor = floor((value / 50.0f)).toInt()
            val num = hashMap[floor]
            if (num != null) {
                hashMap[floor] = num + 1
            } else {
                hashMap[floor] = 1
            }
        }
        return Math.round((Collections.max(hashMap.values) * 100.0f) / rrFiltered.size).toFloat()
    }

    private fun generatePnn50(rrDiff: FloatArray): Float {
        var i6 = 0
        for (f6 in absArrayValues(rrDiff)) {
            if (f6 > 50.0f) {
                i6++
            }
        }
        return (i6 * 100.0f) / rrDiff.size
    }

    private fun absArrayValues(arr: FloatArray): FloatArray {
        val fArr = FloatArray(arr.size)
        for (i6 in arr.indices) {
            fArr[i6] = abs(arr[i6].toDouble()).toFloat()
        }
        return fArr
    }

    private fun decodeRgb(yuv420sp: ByteArray?, width: Int, height: Int): IntArray {
        if (yuv420sp == null) {
            return intArrayOf(0, 0, 0)
        }
        val totalSize = width * height
        var redSum = 0
        var greenSum = 0
        var blueSum = 0
        // 遍历每一行
        for (row in 0 until height) {
            // 计算当前行在 Y 数据中的起始位置
            val yRowStart = row * width

            // 计算对应的 UV 行索引（UV 数据是 Y 高度的一半）
            val uvRow = row / 2
            val uvRowStart = totalSize + uvRow * width

            // 遍历当前行的每一列
            for (column in 0 until width) {
                // 获取 Y 值 (亮度)
                val yIndex = yRowStart + column
                if (yIndex >= yuv420sp.size) continue // 防止越界

                var y = (yuv420sp[yIndex].toInt() and 255) - 16
                if (y < 0) y = 0

                // 获取对应的 UV 值 (色度)
                // 每两个 Y 像素共享一组 UV 值，所以 UV 索引需要除以 2
                val uvColumn = column / 2

                // 计算 UV 索引
                val uvIndex = uvRowStart + uvColumn * 2

                // 确保 UV 索引不越界
                if (uvIndex + 1 >= yuv420sp.size) continue

                val v = (yuv420sp[uvIndex].toInt() and 255) - 128
                val u = (yuv420sp[uvIndex + 1].toInt() and 255) - 128

                // YUV 转 RGB 计算
                var r = y * 1192 + v * 1634
                var g = y * 1192 - v * 833 - u * 400
                var b = y * 1192 + u * 2066

                // 限制在 0-255 范围内
                r = if (r < 0) 0 else if (r > 262143) 262143 else r
                g = if (g < 0) 0 else if (g > 262143) 262143 else g
                b = if (b < 0) 0 else if (b > 262143) 262143 else b

                // 转换为 8 位值
                val red = (r shr 10) and 255
                val green = (g shr 10) and 255
                val blue = (b shr 10) and 255

                redSum += red
                greenSum += green
                blueSum += blue
            }
        }

        // 计算平均值
        return intArrayOf(redSum / totalSize, greenSum / totalSize, blueSum / totalSize)
    }


    fun getMeasureResult(rrStream: FloatArray): HeartRateEntity? {
        if (rrStream.size <= 2) {
            return null
        }
        val height = getUserHeight(180.0f, 0)
        val weight = getUserWeight(84.0f, 0)
        val refinedStream = refineStream(rrStream)
        val differences = getArrayDifferences(refinedStream)
        val amo = generateAmo(refinedStream)
        val pnn50 = generatePnn50(differences)
        val medsd = generateMedsd(differences)
        val nBpm = generateNBpm(height, weight)
        val bpm = generateBpm(refinedStream)
        val sdnn = generateSdnn(refinedStream)
        val refinedSdnn = u(sdnn, bpm, nBpm)
        val stress = generateStress(amo, pnn50, medsd, nBpm, bpm)
        val energy = generateEnergy(refinedSdnn, medsd)
        val rmssd = generateRmssd(differences)
        val meanrr = generateMeanrr(refinedStream)

        val entity = HeartRateEntity()

        entity.bpm = Math.round(bpm)
        entity.stress = stress
        entity.energy = energy
        entity.sdnn = sdnn
        entity.rmssd = rmssd
        entity.amo = amo
        entity.pnn50 = pnn50
        entity.medsd = medsd
        entity.meanrr = meanrr
        val now = Date(System.currentTimeMillis())
        entity.time = now
        entity.updateTime = now
        entity.createTime = now
        Log.e("Blood", "Bpm =$bpm,,stress =$stress,energy = $energy, hrv = $sdnn")
        return entity
    }

    companion object {
        private const val MIN_BRIGHTNESS_CHANGE = 0.003f // 最小亮度波动
        private const val MIN_SATURATION_CHANGE = 0.003f // 最小饱和度波动

        private const val MAX_STATIC_TOLERANCE_MS: Long = 2000 // 最大静止容忍时间（2秒）
    }
}
