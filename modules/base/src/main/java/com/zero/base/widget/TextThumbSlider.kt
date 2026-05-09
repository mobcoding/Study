package com.zero.base.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.SeekBar
import androidx.core.graphics.toColorInt
import kotlin.math.roundToInt

/**
 * 自定义带文字滑块的进度条
 * 支持动态设置区间 [minProgress, maxProgress]
 */
class TextThumbSlider @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    /* ---------------- 颜色配置 ---------------- */

    var isDarkNight = false
    private val colorBg = if (isDarkNight) "#232425".toColorInt() else "#D9D9D9".toColorInt()
    private val colorProgress = if (isDarkNight) "#2D2D2D".toColorInt() else "#F4F5F7".toColorInt()
    private val colorTextLabel = if (isDarkNight) "#D5D5D5".toColorInt() else "#333333".toColorInt()

    /* ---------------- 尺寸配置 ---------------- */

    private val thumbSize = 40.dp()
    private val trackHeight = 50.dp()
    private val gap = 3.dp()

    /* ---------------- 画笔 ---------------- */

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorBg
        style = Paint.Style.FILL
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorProgress
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /* ---------------- 状态属性 ---------------- */

    private var minProgress = 0
    private var maxProgress = 100
    private var _progress = 6f // 当前进度值

    /**
     * 当前进度数值
     */
    var progress: Int
        get() = _progress.roundToInt()
        set(value) {
            val newValue = value.coerceIn(minProgress, maxProgress).toFloat()
            if (_progress != newValue) {
                _progress = newValue
                onProgressChangeListener?.onProgressChanged(this, _progress.roundToInt(), false)
                invalidate()
            }
        }


    /* ---------------- 核心方法 ---------------- */

    /**
     * 设置进度区间及当前值
     */
    fun setRange(min: Int, max: Int, current: Int = min) {
        if (min >= max) {
            Log.e("TextThumbSlider", "min ($min) 必须小于 max ($max)")
            return
        }
        this.minProgress = min
        this.maxProgress = max
        this.progress = current
        invalidate()
    }

    /**
     * 是否展示小数点
     */
    var showDecimal = true
        set(value) {
            field = value
            invalidate()
        }

    /* ---------------- 回调 ---------------- */

    interface OnProgressChangeListener {
        fun onProgressChanged(slider: TextThumbSlider, progress: Int, fromUser: Boolean)
    }

    var onProgressChangeListener: OnProgressChangeListener? = null

    /* ---------------- 绘制逻辑 ---------------- */

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2f

        // 计算滑块可移动的起始和结束 X 坐标
        val startX = gap + thumbSize / 2f + gap
        val endX = w - gap - thumbSize / 2f - gap
        val usableWidth = endX - startX

        // 根据当前进度计算比例 (0.0 ~ 1.0)
        val range = (maxProgress - minProgress).coerceAtLeast(1).toFloat()
        val ratio = (_progress - minProgress) / range
        val thumbX = startX + ratio * usableWidth
        val thumbRightEdge = thumbX + thumbSize / 2f

        /* 1. 绘制轨道背景 */
        trackPaint.color = colorBg
        val trackRect = RectF(0f, centerY - trackHeight / 2f, w, centerY + trackHeight / 2f)
        canvas.drawRoundRect(trackRect, trackHeight / 2f, trackHeight / 2f, trackPaint)

        /* 2. 绘制进度条（带滑块位置镂空效果） */
        val progressRect = RectF(gap, centerY - trackHeight / 2f + gap, thumbRightEdge + gap,
            centerY + trackHeight / 2f - gap)

        val progressPath = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            val radius = (trackHeight - gap * 2) / 2f
            addRoundRect(progressRect, radius, radius, Path.Direction.CW)
            // 镂空滑块圆圈
            addCircle(thumbX, centerY, thumbSize / 2f, Path.Direction.CW)
        }
        canvas.drawPath(progressPath, progressPaint)

        /* 3. 绘制两侧 A- / A+ 标签 */
        textPaint.color = colorTextLabel
        textPaint.textSize = 14.sp()
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("A-", 14.dp(), centerY + baseline(textPaint), textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("A+", w - 14.dp(), centerY + baseline(textPaint), textPaint)

        /* 4. 绘制实心滑块 */
        trackPaint.color = colorBg // 滑块使用白色
        canvas.drawCircle(thumbX, centerY, thumbSize / 2f, trackPaint)

        /* 5. 绘制滑块内部数字 */
        textPaint.color = colorTextLabel
        textPaint.textAlign = Paint.Align.CENTER

        // 根据 showDecimal 决定显示整数还是保留一位小数的浮点数
        // 逻辑：如果是最小值、最大值，或者未开启 showDecimal，则显示整数
        // 只有在区间中间且开启 showDecimal 时才显示一位小数
        val isAtBoundary = _progress <= minProgress || _progress >= maxProgress
        val textToShow = if (showDecimal && !isAtBoundary) {
            "%.1f".format(_progress)
        } else {
            _progress.roundToInt().toString()
        }
        canvas.drawText(textToShow, thumbX, centerY + baseline(textPaint), textPaint)
    }

    /* ---------------- 触摸事件 ---------------- */

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 这里的边界计算需与 onDraw 保持一致以获得精准手感
        val startX = gap + thumbSize / 2f + gap
        val endX = width - gap - thumbSize / 2f - gap
        val usableWidth = endX - startX

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                handleTouchUpdate(event.x, startX, endX, usableWidth)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                handleTouchUpdate(event.x, startX, endX, usableWidth)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                performClick()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouchUpdate(x: Float, startX: Float, endX: Float, usableWidth: Float) {
        val clampedX = x.coerceIn(startX, endX)
        val ratio = (clampedX - startX) / usableWidth

        val range = (maxProgress - minProgress).toFloat()

        // 1. 记录旧的整数值，用于判断回调触发
        val oldInt = _progress.roundToInt()

        // 2. 关键优化：直接保存浮点数，不取整。
        // 这样 onDraw 中的 thumbX 会随像素平滑移动，解决“一节一节”的问题
        _progress = minProgress + (ratio * range)

        val newInt = _progress.roundToInt()

        // 3. 只有当四舍五入后的“整数部分”发生变化时，才触发外部监听
        if (oldInt != newInt) {
            onProgressChangeListener?.onProgressChanged(this, newInt, true)
        }

        // 每一帧都重绘，保证滑块移动平滑
        invalidate()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /* ---------------- 状态持久化 ---------------- */

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable("super_state", super.onSaveInstanceState())
            putFloat("current_progress", _progress)
            putInt("min_val", minProgress)
            putInt("max_val", maxProgress)
            putBoolean("show_decimal", showDecimal) // 保存状态
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            minProgress = state.getInt("min_val")
            maxProgress = state.getInt("max_val")
            _progress = state.getFloat("current_progress")
            showDecimal = state.getBoolean("show_decimal") // 恢复状态
            super.onRestoreInstanceState(state.getParcelable("super_state"))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /* ---------------- 辅助工具 ---------------- */

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = SeekBar::class.java.name
        info.contentDescription = "进度调节: ${progress}"
    }

    private fun baseline(paint: Paint): Float {
        val fm = paint.fontMetrics
        return (fm.descent - fm.ascent) / 2f - fm.descent
    }

    private fun Int.dp(): Float = this * resources.displayMetrics.density
    private fun Int.sp(): Float = this * resources.displayMetrics.scaledDensity
}