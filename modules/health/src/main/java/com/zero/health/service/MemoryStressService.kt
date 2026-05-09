package com.zero.health.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zero.health.R
import com.zero.health.ui.activity.AlarmRemindActivity
import com.zero.jni.NativeMemoryAllocator
import java.util.concurrent.Executors

class MemoryStressService : Service() {
    companion object {
        const val CHANNEL_ID = "OOMTestChannel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_STRESS_TEST = "start_stress_test"
        const val ACTION_STOP_STRESS_TEST = "stop_stress_test"
        const val EXTRA_AGGRESSIVE_MODE = "aggressive_mode"
        const val EXTRA_BLOCK_SIZE = "block_size"
        const val EXTRA_BLOCK_COUNT = "block_count"
        const val EXTRA_MODE = "mode"
    }

    private val nativeAllocator = NativeMemoryAllocator()
    private val executor = Executors.newFixedThreadPool(3)
    private var isStressTesting = false  // 这个变量实际上在当前实现中没有被使用
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MemoryStressService = this@MemoryStressService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundNotification() // 服务创建时立即启动前台通知
    }

    private val SAFE_STANDARD_MODE_MIN_BYTES = 50L * 1024L * 1024L // 标准模式下保留最少50MB
    private val SAFE_AGGRESSIVE_MODE_MIN_BYTES = 500L * 1024L * 1024L // 激进模式最低500MB可用内存门槛

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_STRESS_TEST -> {
                // 在启动新测试前先停止任何现有的测试
                stopStressTest()

                val aggressiveMode = intent.getBooleanExtra(EXTRA_AGGRESSIVE_MODE, false)
                val blockSize = intent.getIntExtra(EXTRA_BLOCK_SIZE, if (aggressiveMode) 50 else 20)
                val blockCount = intent.getLongExtra(EXTRA_BLOCK_COUNT,
                    if (intent.hasExtra(EXTRA_BLOCK_COUNT)) intent.getLongExtra(EXTRA_BLOCK_COUNT,
                        1) else 0)
                val mode = intent.getIntExtra(EXTRA_MODE, if (aggressiveMode) 1 else 0)

                // 在启动前检查系统可用内存，避免把设备推到极限
                val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memInfo)
                val avail = memInfo.availMem

                if (aggressiveMode) {
                    if (avail < SAFE_AGGRESSIVE_MODE_MIN_BYTES) {
                        Log.w("OOMTest",
                            "拒绝启动激进模式：系统可用内存过低(${avail / 1024 / 1024} MB)")
                        return START_STICKY
                    }
                } else {
                    if (avail < SAFE_STANDARD_MODE_MIN_BYTES) {
                        Log.w("OOMTest",
                            "拒绝启动标准模式：系统可用内存过低(${avail / 1024 / 1024} MB)")
                        return START_STICKY
                    }
                }

                startStressTest(blockSize, blockCount, mode, aggressiveMode)
            }

            ACTION_STOP_STRESS_TEST -> {
                stopStressTest()
            }
        }

        return START_STICKY // 服务被杀死后会尝试重启
    }

    // 系统内存紧张时会被回调，立即停止并释放内存
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.i("OOMTest", "onTrimMemory level=$level")
        // 当系统提示内存不足时，立即停止测试以保护自身和系统
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            Log.w("OOMTest", "系统内存紧张，停止压力测试以保护稳定性")
            stopStressTest()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "OOM Test Channel",
                NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val notificationIntent = Intent(this, AlarmRemindActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(
            "OOM压力测试运行中").setContentText("正在消耗系统内存以观察其他应用行为").setSmallIcon(
            R.drawable.ic_heart_rate).setContentIntent(pendingIntent).setOngoing(true).setPriority(
            NotificationCompat.PRIORITY_HIGH).build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startStressTest(blockSizeMB: Int, maxBlocks: Long, mode: Int, aggressive: Boolean) {
        // 在后台线程中启动native内存分配
        executor.execute {
            try {
                if (aggressive) {
                    Log.d("OOMTest", "开始激进模式内存压力测试")
                    nativeAllocator.startStressTest(blockSizeMB, maxBlocks, 1)
                } else {
                    Log.d("OOMTest",
                        "开始标准模式内存压力测试，块大小: $blockSizeMB MB, 块数量: $maxBlocks, 模式: $mode")
                    nativeAllocator.startStressTest(blockSizeMB, maxBlocks, mode)
                }
            } catch (e: OutOfMemoryError) {
                Log.e("OOMTest", "Native内存分配遇到OutOfMemoryError", e)
            } catch (e: Exception) {
                Log.e("OOMTest", "Native内存分配异常", e)
            }
        }

        // 添加额外的内存消耗线程（激进模式）
        if (aggressive) {
            for (i in 0 until 2) {
                executor.execute {
                    try {
                        nativeAllocator.startStressTest(30, 0, 1) // 额外的30MB块分配，激进模式
                    } catch (e: OutOfMemoryError) {
                        Log.e("OOMTest", "额外Native内存分配遇到OutOfMemoryError", e)
                    } catch (e: Exception) {
                        Log.e("OOMTest", "额外Native内存分配异常", e)
                    }
                }
            }
        }
    }

    fun stopStressTest() {
        Log.d("OOMTest", "收到停止压力测试请求")

        // 停止native内存分配
        executor.execute {
            nativeAllocator.stopStressTest()
        }

        // 设置标志位（虽然现在主要依赖native层的标志）
        isStressTesting = false

        Log.d("OOMTest", "压力测试已停止请求已发送")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStressTest()
        executor.shutdown()
    }
}