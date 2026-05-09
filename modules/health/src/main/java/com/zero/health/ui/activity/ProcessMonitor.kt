package com.zero.health.ui.activity

import android.content.Context
import androidx.core.content.edit
import com.zero.health.provider.HealthContextProvider
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 基于 su 的进程监控器
 * - 扫描 /proc
 * - 统计进程 start / end / duration
 * - 适合实时 RecyclerView 展示
 */
class ProcessMonitor(private val scanIntervalMs: Long = 2000L,
                     private val targetPackages: Set<String>? = null) {

    // =========================
    // 对外 UI 使用的数据模型
    // =========================
    // =========================
    // 内部时间轴模型
    // =========================

    private data class Timeline(val pid: Int, val packageName: String, val startUptimeSec: Double,
                                var endUptimeSec: Double? = null, var restartCount: Int = 0) {
        fun duration(now: Double): Double = (endUptimeSec ?: now) - startUptimeSec
    }

    // =========================
    // Raw /proc 扫描结果
    // =========================

    private data class RawProc(val pid: Int, val cmdline: String, val stat: String)

    // =========================
    // 状态容器
    // =========================

    private val active = HashMap<Int, Timeline>()
    private val finished = ArrayList<Timeline>()
    private val processHistory = HashMap<String, Int>() // 记录每个包名的重启次数
    private val activePackageNames = HashSet<String>() // 记录当前存活的包名（归一化后）
    private var isFirstTick = true

    private val running = AtomicBoolean(false)
    private var workerThread: Thread? = null
    private var hzCache: Int = 100
    private var minScanIntervalMs: Long = 800L
    private var maxScanIntervalMs: Long = 3000L
    private var adaptiveIntervalMs: Long = 800L
    private var stableTicks: Int = 0
    private val prefs by lazy {
        HealthContextProvider.appContext.getSharedPreferences("process_restart_counts", Context.MODE_PRIVATE)
    }
    private fun normalizePackageName(raw: String): String {
        val idx = raw.indexOf(':')
        return if (idx > 0) raw.substring(0, idx) else raw
    }

    // =========================
    // Public API
    // =========================

    fun start() {
        if (running.get()) return
        running.set(true)
        isFirstTick = true
        activePackageNames.clear()
        hzCache = readHzBySu()
        minScanIntervalMs = scanIntervalMs.coerceAtMost(1000L)
        maxScanIntervalMs = (scanIntervalMs * 3).coerceAtMost(5000L)
        adaptiveIntervalMs = minScanIntervalMs
        stableTicks = 0
        loadPersistedRestartCounts()

        workerThread = Thread {
            while (running.get()) {
                try {
                    tick()
                    Thread.sleep(adaptiveIntervalMs)
                } catch (_: InterruptedException) {
                } catch (_: Throwable) {
                    // 防止单次异常导致线程退出
                }
            }
        }.apply { start() }
    }

    fun stop() {
        running.set(false)
        workerThread?.interrupt()
        workerThread = null
    }

    /**
     * 构建给 RecyclerView 使用的数据
     */
    fun buildUiList(): List<ProcessUiModel> {
        val now = readUptimeNoSu()

        val runningList = active.values.map {
            ProcessUiModel(pid = it.pid, packageName = it.packageName,
                aliveSeconds = it.duration(now), isRunning = true, restartCount = it.restartCount)
        }

        // 只返回正在运行的进程
        return runningList.sortedByDescending { it.aliveSeconds }
    }

    // =========================
    // 核心扫描逻辑
    // =========================

    private fun tick() {
        val now = readUptimeNoSu()
        val scannedRaw = scanProcessesBySu()

        val currentScannedMap = HashMap<Int, RawProc>()
        val currentPackageSet = HashSet<String>()

        for (proc in scannedRaw) {
            if (!isTargetPackage(proc.cmdline)) continue
            currentScannedMap[proc.pid] = proc
            currentPackageSet.add(normalizePackageName(proc.cmdline))
        }

        val packagesChanged = !isFirstTick && (currentPackageSet != activePackageNames)

        for (pkg in currentPackageSet) {
            if (!activePackageNames.contains(pkg)) {
                if (!isFirstTick) {
                    val newCount = (processHistory[pkg] ?: 0) + 1
                    processHistory[pkg] = newCount
                    persistRestartCount(pkg, newCount)
                } else {
                    if (!processHistory.containsKey(pkg)) {
                        processHistory[pkg] = 0
                    }
                }
            }
        }

        activePackageNames.clear()
        activePackageNames.addAll(currentPackageSet)
        isFirstTick = false
        adjustInterval(packagesChanged)

        for (proc in currentScannedMap.values) {
            if (!active.containsKey(proc.pid)) {
                val startJiffies = parseStartJiffies(proc.stat)
                if (startJiffies <= 0) continue

                val startUptime = startJiffies.toDouble() / hzCache
                val pkg = normalizePackageName(proc.cmdline)
                val count = processHistory[pkg] ?: 0

                active[proc.pid] = Timeline(pid = proc.pid, packageName = proc.cmdline,
                    startUptimeSec = startUptime, restartCount = count)
            } else {
                val pkg = normalizePackageName(proc.cmdline)
                active[proc.pid]?.restartCount = processHistory[pkg] ?: 0
            }
        }

        val it = active.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (!currentScannedMap.containsKey(entry.key)) {
                entry.value.endUptimeSec = now
                finished.add(entry.value)
                it.remove()
            }
        }
    }

    private fun isTargetPackage(packageName: String): Boolean {
        // 如果没有指定目标包名，则不过滤（返回true表示不跳过）
        if (targetPackages == null) return true

        // 检查包名是否在目标列表中
        return targetPackages.any { packageName.contains(it) }
    }

    // =========================
    // su 扫描 /proc
    // =========================

    private fun scanProcessesBySu(): List<RawProc> {
        val script = """
            for p in /proc/[0-9]*; do
              pid=${'$'}{p##*/}
              stat=$(cat ${'$'}p/stat 2>/dev/null) || continue
              cmd=$(tr '\0' ' ' < ${'$'}p/cmdline 2>/dev/null)
              [ -z "${'$'}cmd" ] && continue
              echo "${'$'}pid|${'$'}cmd|${'$'}stat"
            done
        """.trimIndent()

        return runSu(script).mapNotNull { line ->
            val first = line.indexOf('|')
            val second = line.indexOf('|', first + 1)
            if (first <= 0 || second <= first) return@mapNotNull null

            val pid = line.substring(0, first).toIntOrNull() ?: return@mapNotNull null
            val cmd = line.substring(first + 1, second).trim()
            val stat = line.substring(second + 1)

            RawProc(pid, cmd, stat)
        }
    }

    // =========================
    // /proc 解析
    // =========================

    private fun parseStartJiffies(stat: String): Long {
        val end = stat.lastIndexOf(')')
        if (end < 0) return -1

        val after = stat.substring(end + 2)
        val fields = after.split(" ")
        return fields.getOrNull(19)?.toLongOrNull() ?: -1
    }

    // =========================
    // su 工具方法
    // =========================

    private fun readUptimeNoSu(): Double {
        return android.os.SystemClock.elapsedRealtime().toDouble() / 1000.0
    }

    private fun readHzBySu(): Int {
        return runSu("getconf CLK_TCK").firstOrNull()?.toIntOrNull() ?: 100
    }

    private fun adjustInterval(changed: Boolean) {
        if (isFirstTick) {
            adaptiveIntervalMs = minScanIntervalMs
            stableTicks = 0
            return
        }
        if (changed) {
            adaptiveIntervalMs = minScanIntervalMs
            stableTicks = 0
        } else {
            stableTicks++
            if (stableTicks % 3 == 0) {
                adaptiveIntervalMs = (adaptiveIntervalMs + 200L).coerceAtMost(maxScanIntervalMs)
            }
        }
    }

    private fun runSu(cmd: String): List<String> {
        val process = Runtime.getRuntime().exec("su")
        val result = ArrayList<String>()

        process.outputStream.bufferedWriter().use { w ->
            w.write(cmd)
            w.newLine()
            w.write("exit")
            w.newLine()
            w.flush()
        }

        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { result.add(it) }
        }

        process.waitFor()
        return result
    }

    /**
     * 杀死指定PID的进程
     */
    fun killProcess(pid: Int): Boolean {
        try {
            // 尝试使用强制杀死命令
            runSu("kill -9 $pid")
            // 为了更彻底地杀死进程，也可以尝试使用 am 命令强制停止应用
            // 获取进程对应的包名
            val timeline = active[pid]
            if (timeline != null) {
                runSu("am force-stop ${timeline.packageName}")
            }
            // 检查命令执行结果，如果命令执行没有抛出异常，认为尝试执行成功
            // 注意：这里只表示命令执行成功，不代表进程一定被杀死
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun swipeAwayAll(): Boolean {
        try {
            val myPid = android.os.Process.myPid()
            val currentActive = active.toMap()
            val pidsToKill = currentActive.keys.filter { it != myPid }.joinToString(separator = " ")
            if (pidsToKill.isBlank()) {
                return false
            }
            runSu("kill -9 $pidsToKill")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun loadPersistedRestartCounts() {
        try {
            val all = prefs.all
            for ((pkg, value) in all) {
                val count = when (value) {
                    is Int -> value
                    is Long -> value.toInt()
                    is String -> value.toIntOrNull() ?: 0
                    else -> 0
                }
                if (count > 0) {
                    processHistory[normalizePackageName(pkg)] = count
                }
            }
            // 确保所有 targetPackages 都有记录
            targetPackages?.forEach { pkg ->
                val key = normalizePackageName(pkg)
                if (!processHistory.containsKey(key)) {
                    processHistory[key] = 0
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun persistRestartCount(packageName: String, count: Int) {
        try {
            prefs.edit { putInt(packageName, count) }
        } catch (_: Throwable) {
        }
    }

    fun clearRestartCounts(): Boolean {
        return try {
            prefs.edit { clear() }
            processHistory.clear()
            
            // 重置当前活跃包名的计数为 0
            for (pkg in activePackageNames) {
                processHistory[pkg] = 0
                persistRestartCount(pkg, 0)
            }
            
            // 更新 UI 模型
            for (timeline in active.values) {
                timeline.restartCount = 0
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 杀死所有目标包名的进程
     */
    fun killAllTargetProcesses(): Map<Int, Boolean> {
        val results = mutableMapOf<Int, Boolean>()
        val currentActive = active.toMap() // 创建快照防止并发问题

        // 先尝试杀死所有可见的PID
        for ((pid, _) in currentActive) {
            results[pid] = killProcess(pid)
        }

        // 然后尝试强制停止所有目标包名的应用
        if (targetPackages != null) {
            for (packageName in targetPackages) {
                runSu("am force-stop $packageName")
            }
        }

        return results
    }

    /**
     * 强制回收系统内存
     */
    fun forceMemoryReclaim(): Boolean {
        try {
            // 触发系统内存回收
            runSu("echo 3 > /proc/sys/vm/drop_caches") // 清理页面缓存、目录项和inode缓存
            runSu("sync") // 同步文件系统
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}

data class ProcessUiModel(val pid: Int, val packageName: String, val aliveSeconds: Double,
                          val isRunning: Boolean, val restartCount: Int = 0)

