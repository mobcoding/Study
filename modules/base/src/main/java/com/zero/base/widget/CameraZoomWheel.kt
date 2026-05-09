package com.zero.base.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.widget.OverScroller
import androidx.core.graphics.toColorInt
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

class CameraZoomWheel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onZoomChangeListener: ((Float) -> Unit)? = null

    // --- 自动隐藏逻辑 ---
    private val mHideHandler = Handler(Looper.getMainLooper())
    private val mHideRunnable = Runnable { this.visibility = GONE }
    private var mIsTouching = false

    // --- 视觉参数配置 ---
    private val mRadius = 750f
    private val mLineSpacing = 55f
    private val mBottomPadding = 300f
    private val mIndicatorOffset = 20f

    // --- 背景与间距配置 ---
    private val mBgColor = "#000000".toColorInt() // 约60%透明度的黑色
    private val mTextToBgGap = 40f  // 文字顶部距离背景圆弧边缘的间距

    // --- 动态范围 ---
    private var mMinZoom = -2.0f
    private var mMaxZoom = 10.0f
    private var mBaseZoom = 1.0f
    private var mStep = 0.1f

    private var mLeftTicks = 0
    private var mTotalTicks = 0

    // --- 绘图工具 ---
    private val mBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mBgColor
        style = Paint.Style.FILL
    }
    private val mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }
    private val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val mIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFD700".toColorInt()
        style = Paint.Style.FILL
    }

    private var mOffset = 0f
    private val mScroller = OverScroller(context)
    private var mVelocityTracker: VelocityTracker? = null
    private var mLastX = 0f
    private var mLastVibrateIndex = -1

    fun setRange(min: Float, max: Float, base: Float, step: Float = 0.1f) {
        this.mMinZoom = min
        this.mMaxZoom = max
        this.mBaseZoom = base
        this.mStep = step
        mLeftTicks = round((base - min) / step).toInt()
        mTotalTicks = mLeftTicks + round((max - base) / step).toInt()
        mOffset = mLeftTicks * mLineSpacing
        invalidate()
    }

    fun show() {
        this.visibility = VISIBLE
        resetHideTimer()
    }

    fun setZoomValue(value: Float, animated: Boolean = true) {
        val clampedValue = value.coerceIn(mMinZoom, mMaxZoom)

        // 1. 先计算该数值对应的原始索引（可能是小数）
        val rawIndex = (clampedValue - mMinZoom) / mStep

        // 2. 对索引进行四舍五入取整，确保它对齐到最近的刻度线上
        val snappedIndex = round(rawIndex)

        // 3. 计算最终对齐后的物理偏移量
        val targetOffset = snappedIndex * mLineSpacing

        if (animated) {
            stopHideTimer()
            if (!mScroller.isFinished) mScroller.abortAnimation()
            // 使用 Scroller 平滑滚动到对齐的位置
            mScroller.startScroll(mOffset.toInt(), 0, (targetOffset - mOffset).toInt(), 0, 600)
        } else {
            mOffset = targetOffset
            if (visibility == VISIBLE) resetHideTimer()
        }
        postInvalidateOnAnimation()
    }

    fun setZoomValue2(value: Float, animated: Boolean = true) {

        val clampedValue = value.coerceIn(mMinZoom, mMaxZoom)

        val targetOffset = ((clampedValue - mMinZoom) / mStep) * mLineSpacing

        if (animated) {

            stopHideTimer()

            if (!mScroller.isFinished) mScroller.abortAnimation()

            mScroller.startScroll(mOffset.toInt(), 0, (targetOffset - mOffset).toInt(), 0, 600)

        } else {

            mOffset = targetOffset

            if (visibility == VISIBLE) resetHideTimer()

        }

        postInvalidateOnAnimation()

    }

    private fun resetHideTimer() {
        mHideHandler.removeCallbacks(mHideRunnable)
//        mHideHandler.postDelayed(mHideRunnable, 3000)
    }

    private fun stopHideTimer() = mHideHandler.removeCallbacks(mHideRunnable)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0 || visibility != VISIBLE) return

        val centerX = width / 2f
        val centerY = height + mRadius - mBottomPadding

        // --- 1. 绘制圆弧背景 ---
        // 我们需要背景的圆弧顶边缘高于文字。
        // 文字最高点约在 centerY - mRadius - 长刻度线高度 - 文字高度 - 额外偏移
        // 这里我们让背景半径 = 刻度圆半径 + 刻度线长度(45) + 文字高度及间距(60) + 您要求的Gap(mTextToBgGap)
        val bgRadius = mRadius + 45f + 60f + mTextToBgGap
        canvas.drawCircle(centerX, centerY, bgRadius, mBgPaint)

        // 当前对准的刻度索引
        val currentIndex = round(mOffset / mLineSpacing).toInt()

        // --- 2. 绘制刻度和文字 ---
        for (i in 0..mTotalTicks) {
            val itemOffset = i * mLineSpacing
            val relativeOffset = itemOffset - mOffset
            val angle = relativeOffset / mRadius

            // 限制绘制区域，避免计算不必要的点
            if (abs(angle) > Math.toRadians(60.0)) continue

            val x = centerX + mRadius * sin(angle)
            val y = centerY - mRadius * cos(angle)

            val isMin = i == 0
            val isMax = i == mTotalTicks
            val isBase = i == mLeftTicks
            val isMajor = (i - mLeftTicks) % 5 == 0

            val shouldShowLongLine = isMin || isMax || isBase || isMajor
            val h = if (shouldShowLongLine) 45f else 22f

            canvas.drawLine(x, y, x, y - h, mLinePaint)

            if (shouldShowLongLine) {
                // 边界避让逻辑
                val isMajorTooClose = isMajor && !isMin && !isMax && (i < 3 || mTotalTicks - i < 3)
                if (!isMajorTooClose) {
                    val valAtTick = mMinZoom + i * mStep
                    if (i == currentIndex) {
                        mTextPaint.color = mIndicatorPaint.color
                        mTextPaint.textSize = 38f
                    } else {
                        mTextPaint.color = Color.WHITE
                        mTextPaint.textSize = 34f
                    }
                    canvas.drawText(formatValue(valAtTick), x, y - h - 30f, mTextPaint)
                }
            }

            // 反馈
            if (abs(relativeOffset) < mLineSpacing / 2 && mLastVibrateIndex != i) {
                mLastVibrateIndex = i
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onZoomChangeListener?.invoke(mMinZoom + i * mStep)
            }
        }

        // --- 3. 绘制中央黄色指示器 ---
        drawIndicator(canvas, centerX, centerY - mRadius - mIndicatorOffset)
    }

    private fun formatValue(value: Float): String {
        if (abs(value) < 0.01f) return "0.0"
        val sign = if (value > 0 && mMinZoom < 0) "+" else ""
        return "$sign${String.format("%.1f", value)}"
    }

    private fun drawIndicator(canvas: Canvas, cx: Float, cy: Float) {
        val path = Path()
        path.moveTo(cx, cy)
        path.lineTo(cx - 16f, cy - 28f)
        path.lineTo(cx + 16f, cy - 28f)
        path.close()
        canvas.drawPath(path, mIndicatorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain()
        mVelocityTracker?.addMovement(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mIsTouching = true
                stopHideTimer()
                if (!mScroller.isFinished) mScroller.abortAnimation()
                mLastX = event.x
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - mLastX
                mOffset = (mOffset - dx).coerceIn(0f, mTotalTicks * mLineSpacing)
                mLastX = event.x
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsTouching = false
                mVelocityTracker?.computeCurrentVelocity(1000)
                val xVel = mVelocityTracker?.xVelocity ?: 0f
                mScroller.fling(mOffset.toInt(), 0, -xVel.toInt(), 0, 0,
                    (mTotalTicks * mLineSpacing).toInt(), 0, 0)
                snapToTick()
                if (mScroller.isFinished) resetHideTimer()
                invalidate()
                mVelocityTracker?.recycle()
                mVelocityTracker = null
            }
        }
        return true
    }

    private fun snapToTick() {
        val nearest = (round(mOffset / mLineSpacing) * mLineSpacing).coerceIn(0f,
            mTotalTicks * mLineSpacing)
        mScroller.startScroll(mOffset.toInt(), 0, (nearest - mOffset).toInt(), 0, 400)
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mOffset = mScroller.currX.toFloat()
            postInvalidateOnAnimation()
        } else if (!mIsTouching && visibility == VISIBLE) {
            resetHideTimer()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mHideHandler.removeCallbacksAndMessages(null)
    }
}