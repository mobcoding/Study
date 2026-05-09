package com.zero.base.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.toColorInt

class SegmentedSlider @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    // --- 动态配置属性 ---
    private var mLabels = arrayOf("0.5", "1.0", "2.0", "3.0", "4.0", "5.0")

    var showProgress = true
        set(value) {
            field = value
            invalidate()
        }

    var showSplitLines = true
        set(value) {
            field = value
            invalidate()
        }

    // 间隔线颜色
    var colorLineSelected = "#D9D9D9".toColorInt()
        set(value) {
            field = value
            invalidate()
        }
    var colorLineNormal = "#F4F5F7".toColorInt()
        set(value) {
            field = value
            invalidate()
        }

    // 需求：支持动态更改选中与未选中字体颜色
    var colorTextSelected = Color.BLACK
        set(value) {
            field = value
            invalidate()
        }
    var colorTextNormal = Color.GRAY
        set(value) {
            field = value
            invalidate()
        }

    // --- 内部状态 ---
    private var selectedIndex = 1
    private var indicatorX = 0f
    private var itemWidth = 0f
    private var animator: ValueAnimator? = null

    // 尺寸常量
    private val spacing = dpToPx(2f)
    private val indicatorPadding = dpToPx(2f)

    // 画笔
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#D9D9D9".toColorInt() }
    private val progressPaint = Paint(
        Paint.ANTI_ALIAS_FLAG).apply { color = "#F4F5F7".toColorInt() }
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        setShadowLayer(dpToPx(3f), 0f, dpToPx(1.5f), "#20000000".toColorInt())
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = dpToPx(1f) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = spToPx(13f)
        textAlign = Paint.Align.CENTER
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setLabels(labels: Array<String>, defaultIndex: Int = 0) {
        this.mLabels = labels
        this.selectedIndex = defaultIndex.coerceIn(0, labels.size - 1)
        post {
            if (width > 0) {
                itemWidth = width.toFloat() / mLabels.size
                indicatorX = selectedIndex * itemWidth + itemWidth / 2f
                invalidate()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mLabels.isNotEmpty()) {
            itemWidth = w.toFloat() / mLabels.size
            indicatorX = selectedIndex * itemWidth + itemWidth / 2f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mLabels.isEmpty()) return

        val viewHeight = height.toFloat()
        val viewWidth = width.toFloat()
        val bgRadius = viewHeight / 2f

        // 1. 绘制总背景
        canvas.drawRoundRect(0f, 0f, viewWidth, viewHeight, bgRadius, bgRadius, bgPaint)

        // 2. 绘制间隔线
        if (showSplitLines) {
            for (i in 1 until mLabels.size) {
                val lineX = i * itemWidth
                linePaint.color = if (showProgress && lineX < indicatorX) colorLineSelected else colorLineNormal
                canvas.drawLine(lineX, viewHeight * 0.35f, lineX, viewHeight * 0.65f, linePaint)
            }
        }

        // 3. 绘制进度条
        if (showProgress) {
            val indicatorRadius = (viewHeight / 2f) - spacing - indicatorPadding
            val isAtLastSlot = selectedIndex == mLabels.size - 1
            val progressRight = if (isAtLastSlot && !isAnimating()) {
                viewWidth - spacing
            } else {
                (indicatorX + indicatorRadius + indicatorPadding).coerceAtMost(viewWidth - spacing)
            }
            val progressRect = RectF(spacing, spacing, progressRight, viewHeight - spacing)
            canvas.drawRoundRect(progressRect, progressRect.height() / 2f,
                progressRect.height() / 2f, progressPaint)
        }

        // 4. 绘制文字和指示器
        drawLabelsAndIndicator(canvas, viewHeight)
    }

    private fun drawLabelsAndIndicator(canvas: Canvas, viewHeight: Float) {
        val fontMetrics = textPaint.fontMetrics
        val baseline = viewHeight / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
        val indicatorRadius = (viewHeight / 2f) - spacing - indicatorPadding

        // 绘制未选中文字 (应用动态颜色 colorTextNormal)
        for (i in mLabels.indices) {
            val centerX = i * itemWidth + itemWidth / 2f
            if (i != selectedIndex) {
                textPaint.color = colorTextNormal
                textPaint.typeface = Typeface.DEFAULT
                canvas.drawText(mLabels[i], centerX, baseline, textPaint)
            }
        }

        // 绘制白色圆圈
        canvas.drawCircle(indicatorX, viewHeight / 2f, indicatorRadius, indicatorPaint)

        // 绘制选中文字 (应用动态颜色 colorTextSelected)
        textPaint.color = colorTextSelected
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(mLabels[selectedIndex], indicatorX, baseline, textPaint)
    }

    // ... (onTouchEvent, updatePosition, animateTo 逻辑保持不变)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animator?.cancel()
                updatePosition(event.x)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                updatePosition(event.x)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val finalIndex = (event.x / itemWidth).toInt().coerceIn(0, mLabels.size - 1)
                animateTo(finalIndex)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updatePosition(x: Float) {
        val minX = itemWidth / 2f
        val maxX = width - itemWidth / 2f
        indicatorX = x.coerceIn(minX, maxX)
        selectedIndex = (indicatorX / itemWidth).toInt().coerceIn(0, mLabels.size - 1)
        invalidate()
    }

    private fun animateTo(index: Int) {
        val targetX = index * itemWidth + itemWidth / 2f
        if (indicatorX == targetX) return
        animator = ValueAnimator.ofFloat(indicatorX, targetX).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                indicatorX = it.animatedValue as Float
                selectedIndex = (indicatorX / itemWidth).toInt().coerceIn(0, mLabels.size - 1)
                invalidate()
            }
            start()
        }
    }

    private fun isAnimating(): Boolean = animator?.isRunning ?: false
    private fun dpToPx(dp: Float) = dp * context.resources.displayMetrics.density
    private fun spToPx(sp: Float) = sp * context.resources.displayMetrics.scaledDensity
}