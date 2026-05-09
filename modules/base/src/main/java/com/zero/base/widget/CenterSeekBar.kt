package com.zero.base.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.zero.library_base.R
import kotlin.math.max
import kotlin.math.min


class CenterSeekBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private var mThumb: Drawable?

    //默认的背景
    private var mBackgroundDrawable: Drawable?

    //滑动后的背景
    private var mProgressDrawable: Drawable?
    private var mSeekBarWidth: Int
    private var mSeekBarHeight: Int
    private val mThumbWidth: Int
    private val mThumbHeight: Int

    //thumb的中心位置
    private var mThumbCenterPosition = 0
    private var mSeekBarChangeListener: OnSeekBarChangeListener? = null
    private var mFlag = 0
    private var mMinWidth: Int = 0
    private var mMaxWidth: Int = 0
    private var mMinHeight: Int = 8
    private var mMaxHeight: Int = 0
    private var maxProgress: Int
    private var minProgress: Int = 0
    private var progress: Int
    private val mTextPaint: Paint

    // 文本与滑块的垂直间距
    private var mTextPadding = 15f

    // 文本透明度
    private var mTextAlpha = 0f

    // 属性动画
    private var mAlphaAnimator: ValueAnimator? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CenterSeekBar, defStyleAttr, defStyleAttr)
        mThumb = a.getDrawable(R.styleable.CenterSeekBar_thumb)
        if (mThumb == null) {
            mThumb = ContextCompat.getDrawable(context, R.drawable.progress_thumb)
        }
        mProgressDrawable = a.getDrawable(R.styleable.CenterSeekBar_progressDrawable)
        if (mProgressDrawable == null) {
            mProgressDrawable = ContextCompat.getDrawable(context, R.drawable.seekbar_progress)
        }
        mBackgroundDrawable = a.getDrawable(R.styleable.CenterSeekBar_backgroundDrawable)
        if (mBackgroundDrawable == null) {
            mBackgroundDrawable = ContextCompat.getDrawable(context, R.drawable.seekbar_background)
        }
        progress = a.getInt(R.styleable.CenterSeekBar_progress, 0)
        minProgress = a.getInt(R.styleable.CenterSeekBar_min, 0)
        maxProgress = a.getInt(R.styleable.CenterSeekBar_max, 0)
        mSeekBarHeight = mBackgroundDrawable?.intrinsicHeight!!
        mSeekBarWidth = mBackgroundDrawable?.intrinsicWidth!!
        mThumbHeight = mThumb?.intrinsicHeight!!
        mThumbWidth = mThumb?.intrinsicWidth!!
        val mTextSize = a.getDimension(R.styleable.CenterSeekBar_progressTextSize, 14f)
        val mTextColor = a.getColor(R.styleable.CenterSeekBar_progressTextColor, Color.WHITE)
        mTextPadding = a.getDimension(R.styleable.CenterSeekBar_progressTextPadding, mTextPadding)
        mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint.textSize = mTextSize
        mTextPaint.color = mTextColor
        mTextPaint.textAlign = Paint.Align.CENTER
        mAlphaAnimator = ValueAnimator.ofFloat(0f, 1f)
        mAlphaAnimator?.setDuration(ANIMATION_DURATION)
        mAlphaAnimator?.addUpdateListener { animation: ValueAnimator ->
            mTextAlpha = animation.animatedValue as Float
            invalidate()
        }
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = mProgressDrawable
        val thumbHeight = mThumb?.intrinsicHeight ?: 0
        var dw = 0
        var dh = 0
        if (d != null) {
            dw = max(mMinWidth.toDouble(), min(mMaxWidth.toDouble(), d.intrinsicWidth.toDouble())).toInt()
            dh = max(mMinHeight.toDouble(), min(mMaxHeight.toDouble(), d.intrinsicHeight.toDouble())).toInt()
            dh = max(thumbHeight.toDouble(), dh.toDouble()).toInt()
        }
        mSeekBarWidth = resolveSizeAndState(dw, widthMeasureSpec, 0)
        mSeekBarHeight = resolveSizeAndState(dh, heightMeasureSpec, 0)
        mThumbCenterPosition = mSeekBarWidth / 2
        setMeasuredDimension(mSeekBarWidth + mThumbWidth, mSeekBarHeight)
        // 在测量完成后重新设置进度，确保进度条位置正确
        setProgress(progress)
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制背景滑轨
        mBackgroundDrawable?.setBounds(mThumbWidth / 2, mSeekBarHeight / 2 - mMinHeight / 2, mSeekBarWidth - mThumbWidth / 2, mSeekBarHeight / 2 + mMinHeight / 2)
        mBackgroundDrawable?.draw(canvas)

        // 根据是否包含负值来决定滑轨的绘制方式
        if (minProgress >= 0) {
            // 纯正值范围，从左边开始绘制到当前位置
            mProgressDrawable?.setBounds(mThumbWidth / 2, mSeekBarHeight / 2 - mMinHeight / 2, mThumbCenterPosition, mSeekBarHeight / 2 + mMinHeight / 2)
        } else {
            // 包含负值的范围，从中心点开始绘制
            val centerX = mSeekBarWidth / 2
            if (mThumbCenterPosition > centerX) {
                // 正值部分
                mProgressDrawable?.setBounds(centerX, mSeekBarHeight / 2 - mMinHeight / 2, mThumbCenterPosition, mSeekBarHeight / 2 + mMinHeight / 2)
            } else {
                // 负值部分
                mProgressDrawable?.setBounds(mThumbCenterPosition, mSeekBarHeight / 2 - mMinHeight / 2, centerX, mSeekBarHeight / 2 + mMinHeight / 2)
            }
        }
        mProgressDrawable?.draw(canvas)

        // 绘制滑块
        val thumbTop = mSeekBarHeight / 2 - mThumbHeight / 2
        mThumb?.setBounds(mThumbCenterPosition - mThumbWidth / 2, thumbTop, mThumbCenterPosition + mThumbWidth / 2, thumbTop + mThumbHeight)
        mThumb?.draw(canvas)

        // 绘制进度文本
        if (mTextAlpha > 0) {
            mTextPaint.alpha = (mTextAlpha * 255).toInt()
            val progressText = progress.toString()
            val textY = thumbTop - mTextPadding
            canvas.drawText(progressText, mThumbCenterPosition.toFloat(), textY, mTextPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mFlag = CLICK_ON_PRESS
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                mTextAlpha = 1.0f
            }

            MotionEvent.ACTION_MOVE -> {
                if (mFlag == CLICK_ON_PRESS) {
                    mTextAlpha = 1.0f
                    val progressPosition = getProgressPosition(event)
                    mThumbCenterPosition = (progressPosition * (mSeekBarWidth - mThumbWidth) / (maxProgress - minProgress).toFloat() + mThumbWidth.toFloat() / 2).toInt()
                    val oldProgress = progress
                    this.progress = progressPosition + minProgress

                    // Make sure not to exceed bounds
                    this.progress = max(minProgress.toDouble(), min(progress.toDouble(), maxProgress.toDouble())).toInt()

                    if (mSeekBarChangeListener != null && oldProgress != progress) {
                        mSeekBarChangeListener?.onProgressChanged(this, progress)
                    }
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                hideProgressText()
                invalidate()
            }
        }
        return true
    }

    private fun getProgressPosition(event: MotionEvent): Int {
        val scale = if (event.x <= mThumbWidth.toFloat() / 2) {
            0f
        } else if (event.x >= (mSeekBarWidth - mThumbWidth.toFloat() / 2)) {
            1f
        } else {
            (event.x - mThumbWidth.toFloat() / 2) / (mSeekBarWidth - mThumbWidth).toFloat()
        }
        // correct progress calculation using center as reference point
        return ((maxProgress - minProgress) * scale).toInt()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        //设置的Thumb的状态
        val thumb = mThumb
        if (thumb != null && thumb.isStateful && thumb.setState(drawableState)) {
            invalidateDrawable(thumb)
        }
    }


    private fun showProgressText() {
        if (mAlphaAnimator != null) {
            mAlphaAnimator?.cancel()
            mAlphaAnimator?.setFloatValues(mTextAlpha, 1f)
            mAlphaAnimator?.start()
        }
    }

    private fun hideProgressText() {
        if (mAlphaAnimator != null) {
            mAlphaAnimator?.cancel()
            mAlphaAnimator?.setFloatValues(mTextAlpha, 0f)
            mAlphaAnimator?.start()
        }
    }

    fun setRange(minProgress: Int, maxProgress: Int) {
        this.maxProgress = maxProgress
        this.minProgress = minProgress
        // 重新计算当前进度，确保在新范围内
        setProgress(this.progress)
        // 根据范围类型重新设置初始位置
        mThumbCenterPosition = if (minProgress >= 0) {
            // 纯正值范围，从左边开始
            mThumbWidth / 2
        } else {
            // 包含负值的范围，从中心开始
            mSeekBarWidth / 2
        }
        invalidate()
    }

    fun getProgress(): Int {
        return progress
    }

    fun setProgress(progress: Int) {
        // 确保进度值在有效范围内
        this.progress = max(minProgress.toDouble(), min(progress.toDouble(), maxProgress.toDouble())).toInt()
        // 计算滑块位置
        if (minProgress >= 0) {
            // 纯正值范围，从左边开始计算
            val progressScale = (this.progress - minProgress).toFloat() / (maxProgress - minProgress)
            mThumbCenterPosition = (progressScale * (mSeekBarWidth - mThumbWidth) + mThumbWidth.toFloat() / 2).toInt()
        } else {
            // 包含负值的范围，从中心开始计算
            val totalRange = (maxProgress - minProgress).toFloat()
            val progressScale = this.progress.toFloat() / (totalRange / 2)
            mThumbCenterPosition = if (this.progress >= 0) {
                (mSeekBarWidth.toFloat() / 2 + progressScale * (mSeekBarWidth.toFloat() / 2 - mThumbWidth.toFloat() / 2)).toInt()
            } else {
                (mSeekBarWidth.toFloat() / 2 + progressScale * (mSeekBarWidth.toFloat() / 2 - mThumbWidth.toFloat() / 2)).toInt()
            }
        }
        // 显示进度文本
        showProgressText()
        // 延迟隐藏进度文本
        postDelayed({ this.hideProgressText() }, ANIMATION_DURATION * 3)
        invalidate()
    }

    fun setOnSeekBarChangeListener(listener: OnSeekBarChangeListener?) {
        mSeekBarChangeListener = listener
    }

    interface OnSeekBarChangeListener {
        fun onProgressChanged(seekBar: CenterSeekBar?, progress: Int)
    }

    companion object {
        private const val CLICK_ON_PRESS = 1

        // 动画时长
        private const val ANIMATION_DURATION = 300L
    }
}

