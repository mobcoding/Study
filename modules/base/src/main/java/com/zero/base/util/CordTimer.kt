package com.zero.base.util

import java.util.Locale

/**
 * @date:2024/9/29 16:24
 * @path:com.zero.base.util.CordTimer
 */
class CordTimer {

    private var startTime: Long = 0
    private var endTime: Long = 0

    fun startTimer() {
        startTime = System.currentTimeMillis()
    }

    fun stopTimer() {
        endTime = System.currentTimeMillis()
    }

    fun getElapsedTimeInSeconds(): String {
        val elapsedTimeMillis = endTime - startTime
        val elapsedTimeSeconds = elapsedTimeMillis / 1000.0
        return String.format(Locale.US, "%.2fS", elapsedTimeSeconds)
    }
}
