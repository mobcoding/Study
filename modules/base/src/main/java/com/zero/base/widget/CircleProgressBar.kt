package com.zero.base.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.annotation.IntDef
import com.zero.base.ext.dp
import com.zero.base.ext.px
import com.zero.library_base.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.core.content.withStyledAttributes

class CircleProgressBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val mProgressRectF = RectF()
    private val mBoundsRectF = RectF()
    private val mProgressTextRect = Rect()

    private val mProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mProgressBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mProgressTextPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private var mRadius = 0f
    private var mCenterX = 0f
    private var mCenterY = 0f

    // 核心修改：进度值改为 Float 类型
    private var mProgress = 0.1f
    private var mHeardRate = 0
    private var mMax = DEFAULT_MAX

    //Only work well in the Line Style, represents the line count of the rings included
    private var mLineCount = 0

    //Only work well in the Line Style, Height of the line of the progress bar
    private var mLineWidth = 0f

    //Stroke width of the progress of the progress bar
    private var mProgressStrokeWidth = 0f

    //Text size of the progress of the progress bar
    private var mProgressTextSize = 0f

    //Start color of the progress of the progress bar
    private var mProgressStartColor = 0

    //End color of the progress of the progress bar
    private var mProgressEndColor = 0

    //Color of the progress value of the progress bar
    private var mProgressTextColor = 0

    //Background color of the progress of the progress bar
    private var mProgressBackgroundColor = 0

    //the rotate degree of the canvas, default is -90.
    private var mStartDegree = 0

    // whether draw the background only outside the progress area or not
    private var mDrawBackgroundOutsideProgress = false

    // Format the current progress value to the specified format
    private var mProgressFormatter: ProgressFormatter? = DefaultProgressFormatter()

    // The style of the progress color
    @Style
    private var mStyle = 0

    // The Shader of mProgressPaint
    @ShaderMode
    private var mShader = 0

    // The Stroke Cap of mProgressPaint and mProgressBackgroundPaint
    private var mCap: Paint.Cap? = null

    // The blur radius of mProgressPaint
    private var mBlurRadius = 0

    // The blur style of mProgressPaint
    private var mBlurStyle: BlurMaskFilter.Blur? = null

    private val bpmText = "BPM"
    private val bpmTextRect = Rect()
    private var bpmBaseY: Float = 0f
    private var heartBitmap: Bitmap? = null
    private var heartBitmapDrawLeft: Float = 0f
    private var heartBitmapDrawTop: Float = 0f
    private var heartBitmapHeight: Int = 30.dp
    private var heartBitmapWidth: Int = 30.dp
    private val margin = 10.dp

    // 心跳动画相关
    private var heartBeatAnimator: ValueAnimator? = null
    private var heartScale = 1.0f
    private var isHeartBeating = false
    private var heartBeatDuration = 800L // 心跳周期，根据心率动态调整

    init {
        initFromAttributes(context, attrs)
        initPaint()
        startHeartBeatAnimation()
    }

    /**
     * 开始心跳动画
     */
    private fun startHeartBeatAnimation() {
        stopHeartBeatAnimation()
        
        heartBeatAnimator = ValueAnimator.ofFloat(1.0f, 1.3f, 1.0f).apply {
            duration = heartBeatDuration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            
            addUpdateListener { animator ->
                heartScale = animator.animatedValue as Float
                invalidate()
            }
        }
        heartBeatAnimator?.start()
        isHeartBeating = true
    }

    /**
     * 停止心跳动画
     */
    private fun stopHeartBeatAnimation() {
        heartBeatAnimator?.let {
            it.cancel()
            it.removeAllUpdateListeners()
        }
        heartBeatAnimator = null
        isHeartBeating = false
        heartScale = 1.0f
    }

    /**
     * 根据心率更新心跳动画速度
     */
    fun updateHeartBeatSpeed(heartRate: Int) {
        if (heartRate > 0) {
            // 根据心率计算心跳周期，心率越高周期越短
            heartBeatDuration = (60000L / heartRate).coerceIn(400L, 1200L)
            
            // 如果动画正在运行，重新启动以应用新的速度
            if (isHeartBeating) {
                startHeartBeatAnimation()
            }
        }
    }

    /**
     * 开始心跳动画
     */
    fun startHeartBeat() {
        if (!isHeartBeating) {
            startHeartBeatAnimation()
        }
    }

    /**
     * 停止心跳动画
     */
    fun stopHeartBeat() {
        stopHeartBeatAnimation()
        invalidate()
    }

    /**
     * Basic data initialization
     */
    private fun initFromAttributes(context: Context, attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.CircleProgressBar) {
            val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.heart)
            val scale = heartBitmapHeight.toFloat() / originalBitmap.height
            heartBitmapWidth = (originalBitmap.width * scale).toInt()
            heartBitmap = Bitmap.createScaledBitmap(originalBitmap, heartBitmapWidth, heartBitmapHeight, true)
            mLineCount = getInt(R.styleable.CircleProgressBar_line_count, DEFAULT_LINE_COUNT)
            mStyle = getInt(R.styleable.CircleProgressBar_progress_style, LINE)
            mShader = getInt(R.styleable.CircleProgressBar_progress_shader, LINEAR)
            mCap = if (hasValue(R.styleable.CircleProgressBar_progress_stroke_cap)) {
                Paint.Cap.entries.toTypedArray()[getInt(R.styleable.CircleProgressBar_progress_stroke_cap, 0)]
            } else {
                Paint.Cap.BUTT
            }

            mLineWidth = getDimensionPixelSize(R.styleable.CircleProgressBar_line_width, DEFAULT_LINE_WIDTH.px).toFloat()
            mProgressTextSize = getDimensionPixelSize(R.styleable.CircleProgressBar_progress_text_size, DEFAULT_PROGRESS_TEXT_SIZE.px).toFloat()
            mProgressStrokeWidth = getDimensionPixelSize(R.styleable.CircleProgressBar_progress_stroke_width, DEFAULT_PROGRESS_STROKE_WIDTH.px).toFloat()

            mProgressStartColor = getColor(R.styleable.CircleProgressBar_progress_start_color, Color.parseColor(COLOR_FFF2A670))
            mProgressEndColor = getColor(R.styleable.CircleProgressBar_progress_end_color, Color.parseColor(COLOR_FFF2A670))
            mProgressTextColor = getColor(R.styleable.CircleProgressBar_progress_text_color, Color.parseColor(COLOR_FFF2A670))
            mProgressBackgroundColor = getColor(R.styleable.CircleProgressBar_progress_background_color, Color.parseColor(COLOR_FFD3D3D5))

            mStartDegree = getInt(R.styleable.CircleProgressBar_progress_start_degree, DEFAULT_START_DEGREE)
            mDrawBackgroundOutsideProgress = getBoolean(R.styleable.CircleProgressBar_drawBackgroundOutsideProgress, false)

            mBlurRadius = getDimensionPixelSize(R.styleable.CircleProgressBar_progress_blur_radius, 0)
            val blurStyle = getInt(R.styleable.CircleProgressBar_progress_blur_style, 0)
            mBlurStyle = when (blurStyle) {
                1 -> BlurMaskFilter.Blur.SOLID
                2 -> BlurMaskFilter.Blur.OUTER
                3 -> BlurMaskFilter.Blur.INNER
                else -> BlurMaskFilter.Blur.NORMAL
            }
        }
    }

    /**
     * Paint initialization
     */
    private fun initPaint() {
        mProgressTextPaint.textAlign = Paint.Align.CENTER
        mProgressTextPaint.textSize = mProgressTextSize
        mProgressTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        mProgressPaint.style = if (mStyle == SOLID) Paint.Style.FILL else Paint.Style.STROKE
        mProgressPaint.strokeWidth = mProgressStrokeWidth
        mProgressPaint.color = mProgressStartColor
        mProgressPaint.strokeCap = mCap
        updateMaskBlurFilter()

        mProgressBackgroundPaint.style = if (mStyle == SOLID) Paint.Style.FILL else Paint.Style.STROKE
        mProgressBackgroundPaint.strokeWidth = mProgressStrokeWidth
        mProgressBackgroundPaint.color = mProgressBackgroundColor
        mProgressBackgroundPaint.strokeCap = mCap
    }


    private fun updateMaskBlurFilter() {
        if (mBlurStyle != null && mBlurRadius > 0) {
            setLayerType(LAYER_TYPE_SOFTWARE, mProgressPaint)
            mProgressPaint.maskFilter = BlurMaskFilter(mBlurRadius.toFloat(), mBlurStyle)
        } else {
            mProgressPaint.maskFilter = null
        }
    }

    /**
     * The progress bar color gradient,
     * need to be invoked in the [.onSizeChanged]
     */
    private fun updateProgressShader() {
        if (mProgressStartColor != mProgressEndColor) {
            var shader: Shader? = null
            when (mShader) {
                LINEAR -> {
                    shader = LinearGradient(
                        mProgressRectF.left, mProgressRectF.top,
                        mProgressRectF.left, mProgressRectF.bottom,
                        mProgressStartColor, mProgressEndColor,
                        Shader.TileMode.CLAMP
                    )
                    val matrix = Matrix()
                    matrix.setRotate(LINEAR_START_DEGREE, mCenterX, mCenterY)
                    shader.setLocalMatrix(matrix)
                }

                RADIAL -> {
                    shader = RadialGradient(
                        mCenterX, mCenterY, mRadius,
                        mProgressStartColor, mProgressEndColor,
                        Shader.TileMode.CLAMP
                    )
                }

                SWEEP -> {
                    // 核心修改：使用浮点计算弧度
                    val radian = (mProgressStrokeWidth / Math.PI * 2.0f / mRadius).toFloat()
                    val rotateDegrees = (-(if (mCap == Paint.Cap.BUTT && mStyle == SOLID_LINE) 0.0 else Math.toDegrees(radian.toDouble()))).toFloat()

                    shader = SweepGradient(
                        mCenterX, mCenterY,
                        intArrayOf(mProgressStartColor, mProgressEndColor),
                        floatArrayOf(0.0f, 1.0f)
                    )
                    val matrix = Matrix()
                    matrix.setRotate(rotateDegrees, mCenterX, mCenterY)
                    shader.setLocalMatrix(matrix)
                }

                else -> {}
            }
            mProgressPaint.shader = shader
        } else {
            mProgressPaint.shader = null
            mProgressPaint.color = mProgressStartColor
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.rotate(mStartDegree.toFloat(), mCenterX, mCenterY)
        drawProgress(canvas)
        canvas.restore()
        drawProgressText(canvas)
    }

    private fun drawProgressText(canvas: Canvas) {
        if (mProgressFormatter == null) {
            return
        }
        val progressText = mHeardRate.toString()
        if (TextUtils.isEmpty(progressText)) {
            return
        }
        mProgressTextPaint.textSize = mProgressTextSize
        mProgressTextPaint.color = mProgressTextColor
        mProgressTextPaint.getTextBounds(progressText, 0, progressText.length, mProgressTextRect)
        val textBaseY = mCenterY + mProgressTextRect.height() / 2

        // 绘制带心跳动画的爱心图片
        heartBitmap?.let { bitmap ->
            canvas.save()
            // 计算缩放后的位置，保持居中
            val scaledWidth = heartBitmapWidth * heartScale
            val scaledHeight = heartBitmapHeight * heartScale
            val offsetX = (scaledWidth - heartBitmapWidth) / 2
            val offsetY = (scaledHeight - heartBitmapHeight) / 2
            
            // 应用缩放变换
            canvas.translate(heartBitmapDrawLeft - offsetX, heartBitmapDrawTop - offsetY)
            canvas.scale(heartScale, heartScale)
            
            // 绘制爱心图片
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            
            canvas.restore()
        }

        // 再绘制进度文本
        canvas.drawText(progressText, 0, progressText.length, mCenterX, textBaseY, mProgressTextPaint)

        // 绘制 BPM
        val oldTextSize = mProgressTextPaint.textSize
        val oldTextColor = mProgressTextPaint.color
        mProgressTextPaint.textSize = mProgressTextSize * 0.6f
        mProgressTextPaint.color = Color.parseColor("#8A8A91")
        canvas.drawText(bpmText, 0, bpmText.length, mCenterX, bpmBaseY, mProgressTextPaint)
        mProgressTextPaint.textSize = oldTextSize
        mProgressTextPaint.color = oldTextColor
    }


    private fun drawProgress(canvas: Canvas) {
        when (mStyle) {
            SOLID -> drawSolidProgress(canvas)
            SOLID_LINE -> drawSolidLineProgress(canvas)
            LINE -> drawLineProgress(canvas)
            else -> drawLineProgress(canvas)
        }
    }

    /**
     * 绘制线条式进度（核心修改：支持浮点进度计算线条数量）
     */
    private fun drawLineProgress(canvas: Canvas) {
        val unitDegrees = (2.0f * Math.PI / mLineCount).toFloat()
        val outerCircleRadius = mRadius
        val interCircleRadius = mRadius - mLineWidth

        // 核心修改：用浮点进度计算线条数量（保留小数精度）
        val progressRatio = mProgress / mMax
        val progressLineCount = (progressRatio * mLineCount).toInt()

        for (i in 0 until mLineCount) {
            val rotateDegrees = i * -unitDegrees

            val startX = mCenterX + cos(rotateDegrees.toDouble()).toFloat() * interCircleRadius
            val startY = mCenterY - sin(rotateDegrees.toDouble()).toFloat() * interCircleRadius

            val stopX = mCenterX + cos(rotateDegrees.toDouble()).toFloat() * outerCircleRadius
            val stopY = mCenterY - sin(rotateDegrees.toDouble()).toFloat() * outerCircleRadius

            if (mDrawBackgroundOutsideProgress) {
                if (i >= progressLineCount) {
                    canvas.drawLine(startX, startY, stopX, stopY, mProgressBackgroundPaint)
                }
            } else {
                canvas.drawLine(startX, startY, stopX, stopY, mProgressBackgroundPaint)
            }

            if (i < progressLineCount) {
                canvas.drawLine(startX, startY, stopX, stopY, mProgressPaint)
            }
        }
    }

    /**
     * 绘制实心进度（核心修改：浮点进度计算角度）
     */
    private fun drawSolidProgress(canvas: Canvas) {
        // 核心修改：用浮点进度计算角度
        val progressRatio = mProgress / mMax
        val progressAngle = MAX_DEGREE * progressRatio

        if (mDrawBackgroundOutsideProgress) {
            val startAngle = progressAngle
            val sweepAngle = MAX_DEGREE - startAngle
            canvas.drawArc(mProgressRectF, startAngle, sweepAngle, true, mProgressBackgroundPaint)
        } else {
            canvas.drawArc(mProgressRectF, 0.0f, MAX_DEGREE, true, mProgressBackgroundPaint)
        }
        canvas.drawArc(mProgressRectF, 0.0f, progressAngle, true, mProgressPaint)
    }

    /**
     * 绘制线条式圆弧进度（核心修改：浮点进度计算角度）
     */
    private fun drawSolidLineProgress(canvas: Canvas) {
        // 核心修改：用浮点进度计算角度
        val progressRatio = mProgress / mMax
        val progressAngle = MAX_DEGREE * progressRatio

        if (mDrawBackgroundOutsideProgress) {
            val startAngle = progressAngle
            val sweepAngle = MAX_DEGREE - startAngle
            canvas.drawArc(mProgressRectF, startAngle, sweepAngle, false, mProgressBackgroundPaint)
        } else {
            canvas.drawArc(mProgressRectF, 0.0f, MAX_DEGREE, false, mProgressBackgroundPaint)
        }
        canvas.drawArc(mProgressRectF, 0.0f, progressAngle, false, mProgressPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBoundsRectF.left = paddingLeft.toFloat()
        mBoundsRectF.top = paddingTop.toFloat()
        mBoundsRectF.right = (w - paddingRight).toFloat()
        mBoundsRectF.bottom = (h - paddingBottom).toFloat()

        mCenterX = mBoundsRectF.centerX()
        mCenterY = mBoundsRectF.centerY()

        mRadius = (min(mBoundsRectF.width().toDouble(), mBoundsRectF.height().toDouble()) / 2).toFloat()

        mProgressRectF.set(mBoundsRectF)

        updateProgressShader()

        mProgressRectF.inset(mProgressStrokeWidth / 2, mProgressStrokeWidth / 2)

        // 计算 BPM 位置
        mProgressTextPaint.textSize = mProgressTextSize
        mProgressTextPaint.getTextBounds(mHeardRate.toString(), 0, mHeardRate.toString().length, mProgressTextRect)
        val textBaseY = mCenterY + mProgressTextRect.height() / 2
        mProgressTextPaint.textSize = mProgressTextSize * 0.6f
        mProgressTextPaint.getTextBounds(bpmText, 0, bpmText.length, bpmTextRect)
        bpmBaseY = textBaseY + margin + bpmTextRect.height()

        val progressTextTop = textBaseY - mProgressTextRect.height()
        heartBitmapDrawLeft = mCenterX - heartBitmapWidth / 2f
        heartBitmapDrawTop = progressTextTop - margin - heartBitmapHeight
    }


    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.progress = mProgress // 保存浮点进度
        return savedState
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        progress = ss.progress // 恢复浮点进度
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 当View附加到窗口时，如果心率大于0则开始心跳动画
        if (mHeardRate > 0) {
            startHeartBeat()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 当View从窗口分离时，停止心跳动画
        stopHeartBeat()
    }


    fun setProgressFormatter(progressFormatter: ProgressFormatter?) {
        mProgressFormatter = progressFormatter
        invalidate()
    }

    fun setProgressStrokeWidth(progressStrokeWidth: Float) {
        mProgressStrokeWidth = progressStrokeWidth
        mProgressRectF.set(mBoundsRectF)
        updateProgressShader()
        mProgressRectF.inset(mProgressStrokeWidth / 2, mProgressStrokeWidth / 2)
        invalidate()
    }

    fun setProgressTextSize(progressTextSize: Float) {
        mProgressTextSize = progressTextSize
        invalidate()
    }

    fun setProgressStartColor(progressStartColor: Int) {
        mProgressStartColor = progressStartColor
        updateProgressShader()
        invalidate()
    }

    fun setProgressEndColor(progressEndColor: Int) {
        mProgressEndColor = progressEndColor
        updateProgressShader()
        invalidate()
    }

    fun setProgressTextColor(progressTextColor: Int) {
        mProgressTextColor = progressTextColor
        invalidate()
    }

    fun setProgressBackgroundColor(progressBackgroundColor: Int) {
        mProgressBackgroundColor = progressBackgroundColor
        mProgressBackgroundPaint.color = mProgressBackgroundColor
        invalidate()
    }

    fun setLineCount(lineCount: Int) {
        mLineCount = lineCount
        invalidate()
    }

    fun setLineWidth(lineWidth: Float) {
        mLineWidth = lineWidth
        invalidate()
    }

    fun setStyle(@Style style: Int) {
        mStyle = style
        mProgressPaint.style = if (mStyle == SOLID) Paint.Style.FILL else Paint.Style.STROKE
        mProgressBackgroundPaint.style = if (mStyle == SOLID) Paint.Style.FILL else Paint.Style.STROKE
        invalidate()
    }

    fun setBlurRadius(blurRadius: Int) {
        mBlurRadius = blurRadius
        updateMaskBlurFilter()
        invalidate()
    }

    fun setBlurStyle(blurStyle: BlurMaskFilter.Blur?) {
        mBlurStyle = blurStyle
        updateMaskBlurFilter()
        invalidate()
    }

    fun setShader(@ShaderMode shader: Int) {
        mShader = shader
        updateProgressShader()
        invalidate()
    }

    fun setCap(cap: Paint.Cap?) {
        mCap = cap
        mProgressPaint.strokeCap = cap
        mProgressBackgroundPaint.strokeCap = cap
        invalidate()
    }

    fun setStartDegree(startDegree: Int) {
        mStartDegree = startDegree
        invalidate()
    }

    fun setDrawBackgroundOutsideProgress(drawBackgroundOutsideProgress: Boolean) {
        mDrawBackgroundOutsideProgress = drawBackgroundOutsideProgress
        invalidate()
    }

    // 核心修改：支持 Float 类型的进度值
    var progress: Float
        get() = mProgress
        set(progress) {
            // 限制进度在 0~max 范围内
            mProgress = progress.coerceIn(0f, mMax)
            invalidate()
        }

    // 重载：支持传入 Int 类型（兼容旧调用）
    fun setProgress(progress: Int) {
        this.progress = progress.toFloat()
    }

    fun reset() {
        this.progress = 0.1f
    }

    var heartRate: Int
        get() = mHeardRate
        set(heartRate) {
            mHeardRate = heartRate
            updateHeartBeatSpeed(heartRate)
            invalidate()
        }

    var max: Float
        get() = mMax
        set(max) {
            mMax = max
            invalidate()
        }

    // 重载：支持传入 Int 类型的 max（兼容旧调用）
    fun setMax(max: Int) {
        this.max = max.toFloat()
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(LINE, SOLID, SOLID_LINE)
    private annotation class Style

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(LINEAR, RADIAL, SWEEP)
    private annotation class ShaderMode

    // 核心修改：进度格式化器支持 Float 进度
    interface ProgressFormatter {
        fun format(progress: Float, max: Float): CharSequence
    }

    // 核心修改：默认格式化器适配 Float 进度
    private class DefaultProgressFormatter : ProgressFormatter {
        @SuppressLint("DefaultLocale")
        override fun format(progress: Float, max: Float): CharSequence {
            return String.format(DEFAULT_PATTERN, (progress / max * 100).toInt())
        }

        companion object {
            private const val DEFAULT_PATTERN = "%d%%"
        }
    }

    private class SavedState : BaseSavedState {
        var progress: Float = 0f // 保存浮点进度

        constructor(superState: Parcelable?) : super(superState)

        private constructor(parcel: Parcel) : super(parcel) {
            progress = parcel.readFloat() // 读取浮点进度
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(progress) // 写入浮点进度
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel): SavedState {
                    return SavedState(parcel)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        const val LINE: Int = 0
        const val SOLID: Int = 1
        const val SOLID_LINE: Int = 2

        const val LINEAR: Int = 0
        const val RADIAL: Int = 1
        const val SWEEP: Int = 2

        private const val DEFAULT_MAX = 100f // 改为 Float
        private const val MAX_DEGREE = 360.0f
        private const val LINEAR_START_DEGREE = 90.0f

        private const val DEFAULT_START_DEGREE = -90

        private const val DEFAULT_LINE_COUNT = 45

        private const val DEFAULT_LINE_WIDTH = 4.0f
        private const val DEFAULT_PROGRESS_TEXT_SIZE = 11.0f
        private const val DEFAULT_PROGRESS_STROKE_WIDTH = 1.0f

        private const val COLOR_FFF2A670 = "#fff2a670"
        private const val COLOR_FFD3D3D5 = "#ffe3e3e5"
    }
}