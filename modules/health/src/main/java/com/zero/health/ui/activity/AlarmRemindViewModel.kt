package com.zero.health.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zero.health.state.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * @date:2025/12/24 10:39
 * @path:com.zero.health.ui.activity.AlarmRemindViewModel
 */
class AlarmRemindViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<ProcessUiModel>>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()
    private val monitor = ProcessMonitor(1000,
        setOf("com.example.appwidget", "com.toto.jcyj.mvmix", "com.a.oomtest",
            "a.page.launcher.test", "a.notification.listener.test", "a.no.page.launcher.text",
            "com.opencv.accessibilitykeepalive", "com.me.wm", "com.me.battery","com.android.onesignal"))

    private val lastKillTime = AtomicLong(0)
    private val KILL_INTERVAL = 4 * 60 * 1000L // 10分钟间隔（毫秒）

    fun load() {
        monitor.start()
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1000) // 减少延迟时间，提高更新频率
                val list = monitor.buildUiList()
                _uiState.value = UiState.Success(list)
            }
        }

        // 启动自动清理进程的任务
        startAutoKillTask()
    }

    fun killAllTargetProcesses(): Map<Int, Boolean> {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastKill = currentTime - lastKillTime.get()

        if (timeSinceLastKill < KILL_INTERVAL) {
            // 如果距离上次执行不到10分钟，返回空结果
            return emptyMap()
        }

        // 更新最后执行时间
        lastKillTime.set(currentTime)

        return monitor.killAllTargetProcesses()
    }

    fun swipeAwayAll(): Boolean {
        return monitor.swipeAwayAll()
    }

    private fun startAutoKillTask() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(KILL_INTERVAL) // 每10分钟执行一次
                val currentTime = System.currentTimeMillis()
                lastKillTime.set(currentTime) // 更新最后执行时间
                monitor.killAllTargetProcesses() // 自动杀死目标进程
            }
        }
    }

    fun forceMemoryReclaim(): Boolean {
        return monitor.forceMemoryReclaim()
    }

    override fun onCleared() {
        super.onCleared()
        monitor.stop()
    }
}