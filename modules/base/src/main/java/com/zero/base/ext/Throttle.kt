package com.zero.base.ext

import android.os.SystemClock
import java.util.concurrent.TimeUnit

class Throttle(skipDuration: Long, timeUnit: TimeUnit) {
    private val delayMilliseconds: Long = if (skipDuration < 0) {
        0
    } else {
        timeUnit.toMillis(skipDuration)
    }
    private var oldTime = 0L

    fun needSkip(): Boolean {
        val nowTime = SystemClock.elapsedRealtime()
        val intervalTime = nowTime - oldTime
        if (oldTime == 0L || intervalTime >= delayMilliseconds) {
            oldTime = nowTime
            return false
        }

        return true
    }
}