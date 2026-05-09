package com.zero.health.helper

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import kotlin.math.roundToInt

/**
 * 进度条动画管理类
 * 负责进度条在指定时间内从0平滑滚动到100，并提供停止功能
 */
class ProgressHelper(private val startProgress: Float = 0f, private val targetProgress: Float = 100f, private val duration: Long = 10000, private val updateListener: (progress: Float) -> Unit = {}) {
    private var animator: ValueAnimator? = null

    fun start() {
        stop() // 停止已有动画
        val step = 0.1f
        val start = (startProgress / step).roundToInt()
        val target = (targetProgress / step).roundToInt()
        val values = FloatArray(if (target >= start) target - start + 1 else 0) { i -> (start + i) * step }
        animator = ValueAnimator.ofFloat(*values).apply {
            duration = this@ProgressHelper.duration
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val roundedInt = Math.round(progress * 10)
                val formattedProgress = roundedInt / 10.0f
                updateListener.invoke(formattedProgress)
            }
            start()
        }
    }

    /**
     * 停止进度条动画
     * @return 停止时的进度值
     */
    fun stop() {
        animator?.let {
            if (it.isRunning || it.isPaused) {
                it.cancel()
            }
        }
        animator = null
    }

    /**
     * 暂停动画（可恢复）
     */
    fun pause() {
        if (animator?.isRunning == true) {
            animator?.pause()
        }
    }

    /**
     * 恢复暂停的动画
     */
    fun resume() {
        if (animator?.isPaused == true) {
            animator?.resume()
        }
    }

    /**
     * 释放资源（在Activity/Fragment销毁时调用）
     */
    fun release() {
        stop()
    }
}
