package com.zero.base.widget

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import com.bumptech.glide.Glide

/**
 * ImageView对比
 */
class CompareImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    // 图片控件
    private val mImageViewA by lazy {
        ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }
    private val mImageViewB by lazy {
        ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    // 样式相关
    private var lineColor: Int = Color.WHITE
    private var lineWidth: Float = 2f
    private var sliderIconSize = 45

    // 动画和交互
    private var animator: ValueAnimator? = null
    private var linePer: Float = 0.5f
    private var startX = 0f

    private var isUserInteracting = false // 标记是否正在拖动

    // 滑动方向
    private var comparisonOrientation = HORIZONTAL

    companion object {
        const val HORIZONTAL = 0
        const val VERTICAL = 1
    }

    init {
        isFocusable = true
        isClickable = true
        contentDescription = "des"
    }

    // 设置图片
    fun setImage(imgBefore: Any?, imgAfter: Any?) {
        loadImage(mImageViewB, imgBefore)
        loadImage(mImageViewA, imgAfter)
    }

    private fun loadImage(imageView: ImageView, source: Any?) {
        when (source) {
            is Bitmap -> imageView.setImageBitmap(source)
            is Int -> imageView.setImageResource(source)
            is Uri -> imageView.setImageURI(source)
            is String -> Glide.with(context).load(source).into(imageView)
            else -> throw IllegalArgumentException("Unsupported image source type")
        }
    }

    // 设置分隔线样式
    fun setLineStyle(color: Int, width: Float) {
        lineColor = color
        lineWidth = width
        invalidate()
    }

    private val mLinePaint by lazy {
        // 设置渐变效果
        val gradient = LinearGradient(0f, 0f, width.toFloat(), 0f, Color.parseColor("#FF78FA"), Color.parseColor("#4A66E0"), Shader.TileMode.CLAMP)
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
            color = lineColor
            strokeWidth = lineWidth
        }
    }


    // 设置比较方向
    fun setComparisonOrientation(orientation: Int) {
        if (comparisonOrientation != orientation) {
            comparisonOrientation = orientation
            invalidate()
        }
    }

    // 动画绘制
    fun autoPlayDrawImage(start: Float, end: Float, duration: Long = 1000, interpolator: TimeInterpolator = LinearInterpolator()) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(start, end).apply {
            this.interpolator = interpolator
            addUpdateListener {
                linePer = it.animatedValue as Float
                postInvalidate()
            }
            this.duration = duration
            start()
        }
    }

    fun startAnimation(start: Float, end: Float, duration: Long = 1000, repeat: Boolean = true) {
        animator?.cancel() // 取消当前动画
        animator = ValueAnimator.ofFloat(start, end).apply {
            interpolator = LinearInterpolator() // 默认线性动画
            addUpdateListener {
                linePer = it.animatedValue as Float
                postInvalidate()
            }
            this.duration = duration
            if (repeat) {
                repeatCount = ValueAnimator.INFINITE // 无限循环
                repeatMode = ValueAnimator.REVERSE // 循环模式，往返
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (comparisonOrientation) {
            HORIZONTAL -> {
                mImageViewB.draw(canvas)
                canvas.save()
                canvas.clipRect(getLineValue(), 0, width, height)
            }

            VERTICAL -> {
                mImageViewB.draw(canvas)
                canvas.save()
                canvas.clipRect(0, getLineValue(), width, height)
            }
        }
        mImageViewA.draw(canvas)
        canvas.restore()

        // 绘制分隔线
        canvas.save()
        if (comparisonOrientation == HORIZONTAL) {
            canvas.drawLine(getLineValue().toFloat(), 0f, getLineValue().toFloat(), height.toFloat(), mLinePaint)
        } else {
            canvas.drawLine(0f, getLineValue().toFloat(), width.toFloat(), getLineValue().toFloat(), mLinePaint)
        }
        canvas.restore()

        // 绘制滑块图标
        canvas.save()/*canvas.translate(
            (getLineValue() - sliderIconSize / 2).toFloat(),
            height - sliderIconSize.toFloat() * 2
        )
        mDrawableBar?.setBounds(0, 0, sliderIconSize, sliderIconSize / 2)
        mDrawableBar?.draw(canvas)
        canvas.restore()*/
    }

    private fun getLineValue(): Int = if (comparisonOrientation == HORIZONTAL) {
        (width * linePer).toInt()
    } else {
        (height * linePer).toInt()
    }

    /**
     * 停止播放动画
     */
    fun stopAnimation() {
        animator?.cancel()
    }

    /**
     * 根据当前位置恢复动画
     */
    private fun resumeAnimation() {
        animator?.start()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                stopAnimation()
                isUserInteracting = true
                startX = if (comparisonOrientation == HORIZONTAL) event.rawX else event.rawY
                val pos = getLineValue()
                return if (comparisonOrientation == HORIZONTAL) {
                    startX > (pos - sliderIconSize) && startX < (pos + sliderIconSize)
                } else {
                    startX > (pos - sliderIconSize) && startX < (pos + sliderIconSize)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val movePos = if (comparisonOrientation == HORIZONTAL) event.rawX else event.rawY
                val delta = movePos - startX
                val pos = getLineValue()
                linePer = if (comparisonOrientation == HORIZONTAL) {
                    (pos + delta) / width
                } else {
                    (pos + delta) / height
                }
                linePer = linePer.coerceIn(0f, 1f)
                startX = movePos
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (isUserInteracting) {
                    isUserInteracting = false
                    resumeAnimation() // 用户交互结束后恢复动画
                }
                return false
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mImageViewA.measure(widthMeasureSpec, heightMeasureSpec)
        mImageViewB.measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mImageViewA.layout(left, top, right, bottom)
        mImageViewB.layout(left, top, right, bottom)
    }
}



