package com.zero.health.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.zero.health.R
import com.zero.health.ui.activity.ProcessMonitor
import com.zero.health.ui.activity.ProcessUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileReader
import kotlin.math.roundToInt

class MemoryMonitorOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var memoryInfoText: TextView? = null
    private var processContainer: LinearLayout? = null
    private var updateRunnable: Runnable? = null
    private var closeBtn: TextView? = null
    private var startBtn: TextView? = null
    private var killAllBtn: TextView? = null
    private var swipeAwayBtn: TextView? = null
    private var clearBtn: TextView? = null
    private var pressureBtn: TextView? = null
    private var resetBtn: TextView? = null
    private var scrollView: ScrollView? = null
    private val updateHandler = android.os.Handler(android.os.Looper.getMainLooper())

    // 用于存储当前进程列表
    private var currentProcessList: List<ProcessUiModel> = emptyList()

    // 进程监控器 - 使用与AlarmRemindViewModel相同的包名列表
    private val monitor = ProcessMonitor(1000,
        setOf("com.example.appwidget", "com.a.oomtest", "a.page.launcher.test",
            "a.notification.listener.test", "a.no.page.launcher.text",
            "com.opencv.accessibilitykeepalive", "com.me.wm", "com.me.battery", "com.hq.recorder",
            "com.test.keekalivetest", "com.me.fs", "com.opencv.datasynckeeyalive",
            "com.opencv.accuratealarmclockdemo"))

    // 用于控制更新的协程
    private var updateJob: kotlinx.coroutines.Job? = null

    override fun onBind(intent: Intent?): IBinder? {
        return LocalBinder()
    }

    inner class LocalBinder : android.os.Binder() {
        fun getService(): MemoryMonitorOverlayService = this@MemoryMonitorOverlayService
    }

    override fun onCreate() {
        super.onCreate()
        if (Settings.canDrawOverlays(this)) {
            createOverlayView()
            startUpdatingMemoryInfo()
            updateProcessList() // 初始化进程列表显示
            // 自动开始加载进程
            startProcessMonitoring()
        }
    }

    private fun createOverlayView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_process_list, null)

        memoryInfoText = overlayView?.findViewById(R.id.tvProcessTitle)
        processContainer = overlayView?.findViewById(R.id.processContainer)
        closeBtn = overlayView?.findViewById(R.id.tvClose)
        startBtn = overlayView?.findViewById(R.id.tvStart)
        killAllBtn = overlayView?.findViewById(R.id.tvKillAll)
        swipeAwayBtn = overlayView?.findViewById(R.id.tvSwipeAway)
        clearBtn = overlayView?.findViewById(R.id.tvClear)
        pressureBtn = overlayView?.findViewById(R.id.tvPressure)
        resetBtn = overlayView?.findViewById(R.id.tvResetCounts)
        scrollView = overlayView?.findViewById(R.id.scrollView)

        // 设置关闭按钮点击事件
        closeBtn?.setOnClickListener {
            stopSelf()
        }

        // 设置其他按钮点击事件
        startBtn?.setOnClickListener {
            // 启动进程监控
            startProcessMonitoring()
        }

        killAllBtn?.setOnClickListener {
            // 强杀所有进程
            killAllProcesses()
        }

        swipeAwayBtn?.setOnClickListener {
            // 软杀进程
            swipeAwayProcesses()
        }

        clearBtn?.setOnClickListener {
            // 内存回收
            forceMemoryReclaim()
        }

        pressureBtn?.setOnClickListener {
            // 内存压力测试
            startMemoryStressTest()
        }

        resetBtn?.setOnClickListener {
            val ok = monitor.clearRestartCounts()
            android.widget.Toast.makeText(this, if (ok) "已清零重启次数" else "清零失败",
                android.widget.Toast.LENGTH_SHORT).show()
            val list = monitor.buildUiList()
            this.currentProcessList = list
            updateProcessList()
        }

        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT)
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT)
        }

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100
        // 设置触摸监听以支持拖动
        overlayView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: android.view.MotionEvent): Boolean {
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }

                    android.view.MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })
        windowManager?.addView(overlayView, params)
    }

    private fun startUpdatingMemoryInfo() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateMemoryInfo()
                updateHandler.postDelayed(this, 2000) // 每2秒更新一次
            }
        }
        updateHandler.post(updateRunnable!!)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // 确保服务被杀死后会重启
    }

    private fun startProcessMonitoring() {
        try {
            // 先检查ROOT权限
            if (!hasRootAccess()) {
                android.widget.Toast.makeText(this, "警告：没有ROOT权限，进程监控可能无法正常工作",
                    android.widget.Toast.LENGTH_LONG).show()
            }

            monitor.start()

            // 启动更新协程
            if (updateJob?.isActive != true) {
                updateJob = CoroutineScope(Dispatchers.IO).launch {
                    while (true) {
                        try {
                            val list = monitor.buildUiList()
                            this@MemoryMonitorOverlayService.currentProcessList = list

                            // 在主线程更新UI
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                updateProcessList()
                            }

                            delay(1000) // 每秒更新一次
                        } catch (e: Exception) {
                            e.printStackTrace()
                            delay(1000)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "启动进程监控失败: ${'$'}{e.message}",
                android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun hasRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su -c " + "id")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun killAllProcesses() {
        try {
            val results = monitor.killAllTargetProcesses()
            results.count { it.value }
            val totalCount = results.size

            android.widget.Toast.makeText(this,
                "已尝试杀死 $totalCount 个进程，并强制停止目标应用\n(结果可能因应用自启机制而有限)",
                android.widget.Toast.LENGTH_LONG).show()

            // 重新获取进程列表
            val list = monitor.buildUiList()
            this.currentProcessList = list
            updateProcessList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun swipeAwayProcesses() {
        try {
            val results = monitor.swipeAwayAll()
            android.widget.Toast.makeText(this, "滑动删除所有快照 $results",
                android.widget.Toast.LENGTH_SHORT).show()

            // 重新获取进程列表
            val list = monitor.buildUiList()
            this.currentProcessList = list
            updateProcessList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun forceMemoryReclaim() {
        try {
            val success = monitor.forceMemoryReclaim()

            if (success) {
                android.widget.Toast.makeText(this, "已触发系统内存回收，请观察进程变化",
                    android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(this, "内存回收执行失败，请检查ROOT权限",
                    android.widget.Toast.LENGTH_SHORT).show()
            }

            // 重新获取进程列表
            val list = monitor.buildUiList()
            this.currentProcessList = list
            updateProcessList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startMemoryStressTest() {
        try {
            // 发送开始压力测试的命令
            val intent = Intent(this, MemoryStressService::class.java).apply {
                action = MemoryStressService.ACTION_START_STRESS_TEST
                putExtra(MemoryStressService.EXTRA_AGGRESSIVE_MODE, false)
            }
            startService(intent)

            android.widget.Toast.makeText(this, "内存压力测试已启动",
                android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "内存压力测试启动失败: ${'$'}{e.message}",
                android.widget.Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun stopProcessMonitoring() {
        try {
            updateJob?.cancel()
            monitor.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProcessMonitoring()

        windowManager?.let { wm ->
            overlayView?.let { view ->
                wm.removeView(view)
            }
        }
        updateRunnable?.let {
            updateHandler.removeCallbacks(it)
        }
    }

    private fun updateMemoryInfo() {
        val (availableMemory, totalMemory, percentage) = getMemoryInfo()
        val availableMB = (availableMemory / (1024 * 1024)).toInt()
        val totalMB = (totalMemory / (1024 * 1024)).toInt()
        val memoryText = "内存: $availableMB MB / $totalMB MB ($percentage%)"
        memoryInfoText?.text = memoryText

        // 更新进程列表
        updateProcessList()
    }

    private fun updateProcessList() {
        val container = processContainer ?: return
        val newList = currentProcessList.take(10)

        if (newList.isEmpty()) {
            container.removeAllViews()
            val emptyTextView = TextView(this).apply {
                text = "无进程"
                setTextColor(android.graphics.Color.GRAY)
                textSize = 12f
                setPadding(8, 4, 8, 4)
            }
            emptyTextView.tag = "empty"
            container.addView(emptyTextView)
            return
        } else {
            // 移除占位视图
            for (i in container.childCount - 1 downTo 0) {
                val v = container.getChildAt(i)
                if (v.tag == "empty") {
                    container.removeViewAt(i)
                }
            }
        }

        val newPids = newList.map { it.pid }.toSet()

        // 删除旧的视图（不在新列表中的）
        for (i in container.childCount - 1 downTo 0) {
            val v = container.getChildAt(i)
            val pidTag = v.tag as? Int
            if (pidTag != null && !newPids.contains(pidTag)) {
                container.removeViewAt(i)
            }
        }

        // 逐项更新或插入，确保顺序与新列表一致
        for ((index, process) in newList.withIndex()) {
            val existingIndex = (0 until container.childCount).firstOrNull { idx ->
                (container.getChildAt(idx).tag as? Int) == process.pid
            }

            val view: LinearLayout = if (existingIndex == null) {
                // 创建新视图
                val processInfo = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(8, 4, 8, 4)
                }
                processInfo.tag = process.pid

                val processTitle = TextView(this)
                val processSubtitle = TextView(this)
                val processRestartCount = TextView(this)

                processTitle.setTextColor(android.graphics.Color.WHITE)
                processTitle.textSize = 11f

                processSubtitle.setTextColor(android.graphics.Color.LTGRAY)
                processSubtitle.textSize = 10f

                processRestartCount.setTextColor(android.graphics.Color.YELLOW)
                processRestartCount.textSize = 10f

                processInfo.addView(processTitle)
                processInfo.addView(processSubtitle)
                processInfo.addView(processRestartCount)

                // 插入到指定位置
                val safeIndex = index.coerceAtMost(container.childCount)
                container.addView(processInfo, safeIndex)
                processInfo
            } else {
                // 将视图移动到正确位置
                if (existingIndex != index) {
                    val v = container.getChildAt(existingIndex)
                    container.removeViewAt(existingIndex)
                    val safeIndex = index.coerceAtMost(container.childCount)
                    container.addView(v, safeIndex)
                }
                container.getChildAt(index) as LinearLayout
            }

            // 更新文本内容
            val title = view.getChildAt(0) as TextView
            val subtitle = view.getChildAt(1) as TextView
            val restart = view.getChildAt(2) as TextView

            title.text = "PID ${process.pid}  包名：" +
                process.packageName.substring(0, minOf(process.packageName.length, 30))
            val status = if (process.isRunning) "RUNNING" else "ENDED"
            val alive = formatTime(process.aliveSeconds.toInt())
            subtitle.text = "存活：$alive   状态：$status"
            restart.text = "重启次数：${process.restartCount}"
        }
    }

    private fun formatTime(seconds: Int): String {
        val s = if (seconds < 0) 0 else seconds
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val secs = s % 60

        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, secs)
        } else {
            "%02d:%02d".format(minutes, secs)
        }
    }

    private fun getMemoryInfo(): Triple<Long, Long, Int> {
        var totalMemory = 0L
        var availableMemory = 0L

        try {
            BufferedReader(FileReader("/proc/meminfo")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("MemTotal:")) {
                        totalMemory = line.split("\\s+".toRegex())[1].toLong() * 1024 // 转换为字节
                    } else if (line.startsWith("MemAvailable:")) {
                        availableMemory = line.split("\\s+".toRegex())[1].toLong() * 1024 // 转换为字节
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val percentage = if (totalMemory > 0) {
            ((availableMemory.toDouble() / totalMemory.toDouble()) * 100).roundToInt()
        } else {
            0
        }

        return Triple(availableMemory, totalMemory, percentage)
    }
}
