package com.zero.base.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toRect
import com.zero.base.ext.dp
import com.zero.library_base.R

class EdgeLightView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val lightPaint: Paint = Paint()
    private val bitmapLightPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var totalHeight: Int = 0
    private var totalWidth: Int = 0
    private var lineSpaceSize: Float = 0f
    private var spaceSize: Float = 0f
    private val path = Path()
    private lateinit var sweepGradient: SweepGradient
    private val gradientMatrix = Matrix()
    private var mRotate = 0f
    private var rotateOffset = 15f
    private var topRound = 1f.dp
    private var bottomRound = 1f.dp
    private var isLine = true
    private var bitmap: Drawable? = null
    private var bitmapRectF: RectF = RectF(0f, 0f, 0f, 0f)
    private var countRatio = 0f
    private val pos = FloatArray(2)
    private var colors: IntArray = intArrayOf(Color.GREEN, Color.RED, Color.YELLOW)

    init {
        lightPaint.color = ResourcesCompat.getColor(resources, R.color.baseColorPrimary, null)
        lightPaint.isAntiAlias = true
        lightPaint.style = Paint.Style.STROKE
        lineSpaceSize = 1f.dp
        spaceSize = lineSpaceSize / 2
        lightPaint.strokeWidth = lineSpaceSize * 2
        bitmapLightPaint.color = ResourcesCompat.getColor(resources, R.color.baseColorPrimary, null)
        bitmapLightPaint.style = Paint.Style.FILL
        bitmapLightPaint.strokeWidth = 0f
        bitmap = ContextCompat.getDrawable(context, R.drawable.base_ic_circle)
        bitmapLightPaint.isAntiAlias = true
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        totalHeight = MeasureSpec.getSize(heightMeasureSpec)
        totalWidth = MeasureSpec.getSize(widthMeasureSpec)
        setColor(colors)
        setLayerType(LAYER_TYPE_HARDWARE, bitmapLightPaint)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        try {
            if (isLine) {
                path.moveTo(lineSpaceSize, totalHeight - bottomRound)
                path.lineTo(lineSpaceSize, topRound)
                if (topRound < lineSpaceSize) {
                    path.lineTo(lineSpaceSize, lineSpaceSize)
                } else {
                    path.quadTo(lineSpaceSize, lineSpaceSize, topRound, lineSpaceSize)
                }
                path.lineTo(totalWidth - topRound, lineSpaceSize)
                if (topRound < lineSpaceSize) {
                    path.lineTo(totalWidth - lineSpaceSize, lineSpaceSize)
                } else {
                    path.quadTo(totalWidth - lineSpaceSize, lineSpaceSize, totalWidth - lineSpaceSize, topRound)
                }
                path.lineTo(totalWidth - lineSpaceSize, totalHeight - bottomRound)
                if (bottomRound < lineSpaceSize) {
                    path.lineTo(totalWidth - lineSpaceSize, totalHeight - lineSpaceSize)
                } else {
                    path.quadTo(totalWidth - lineSpaceSize, totalHeight - lineSpaceSize, totalWidth - bottomRound, totalHeight - lineSpaceSize)
                }
                path.lineTo(bottomRound, totalHeight - lineSpaceSize)
                if (bottomRound < lineSpaceSize) {
                    path.lineTo(lineSpaceSize, totalHeight - lineSpaceSize)
                } else {
                    path.quadTo(lineSpaceSize, totalHeight - lineSpaceSize, lineSpaceSize, totalHeight - bottomRound)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    canvas.clipOutPath(path)
                } else {
                    canvas.clipPath(path, Region.Op.DIFFERENCE)
                }
                canvas.drawColor(Color.BLACK)
                path.reset()

                path.moveTo(lineSpaceSize, totalHeight - bottomRound)
                path.lineTo(lineSpaceSize, topRound)
                path.quadTo(lineSpaceSize, lineSpaceSize, topRound, lineSpaceSize)
                path.lineTo(totalWidth - topRound, lineSpaceSize)
                path.quadTo(totalWidth - lineSpaceSize, lineSpaceSize, totalWidth - lineSpaceSize, topRound)
                path.lineTo(totalWidth - lineSpaceSize, totalHeight - bottomRound)
                path.quadTo(totalWidth - lineSpaceSize, totalHeight - lineSpaceSize, totalWidth - bottomRound, totalHeight - lineSpaceSize)
                path.lineTo(bottomRound, totalHeight - lineSpaceSize)
                path.quadTo(lineSpaceSize, totalHeight - lineSpaceSize, lineSpaceSize, totalHeight - bottomRound)
                canvas.drawPath(path, lightPaint)
                path.reset()
            } else {
                path.moveTo(spaceSize, totalHeight - bottomRound)
                path.lineTo(spaceSize, topRound)
                if (topRound < spaceSize) {
                    path.lineTo(spaceSize, spaceSize)
                } else {
                    path.quadTo(spaceSize, spaceSize, topRound, spaceSize)
                }
                path.lineTo(totalWidth - topRound, spaceSize)
                if (topRound < spaceSize) {
                    path.lineTo(totalWidth - spaceSize, spaceSize)
                } else {
                    path.quadTo(totalWidth - spaceSize, spaceSize, totalWidth - spaceSize, topRound)
                }
                path.lineTo(totalWidth - spaceSize, totalHeight - bottomRound)
                if (bottomRound < spaceSize) {
                    path.lineTo(totalWidth - spaceSize, totalHeight - spaceSize)
                } else {
                    path.quadTo(totalWidth - spaceSize, totalHeight - spaceSize, totalWidth - bottomRound, totalHeight - spaceSize)
                }
                path.lineTo(bottomRound, totalHeight - spaceSize)
                if (bottomRound < spaceSize) {
                    path.lineTo(spaceSize, totalHeight - spaceSize)
                } else {
                    path.quadTo(spaceSize, totalHeight - spaceSize, spaceSize, totalHeight - bottomRound)
                }

                val pathMeasure = PathMeasure(path, false)
                val ratioOffset = spaceSize * 4 / pathMeasure.length
                if (countRatio > 1) {
                    countRatio = 0f
                }
                //            bitmapLightPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_ATOP)
                bitmapLightPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                while (countRatio <= 1) {
                    pathMeasure.getPosTan(pathMeasure.length * countRatio, pos, null)
                    bitmapRectF.set(pos[0] - spaceSize, pos[1] - spaceSize, pos[0] + spaceSize, pos[1] + spaceSize)
                    bitmap?.bounds = bitmapRectF.toRect()
                    bitmap?.draw(canvas)
                    bitmapRectF.set(bitmapRectF.left - 2, bitmapRectF.top - 2, bitmapRectF.right + 2, bitmapRectF.bottom + 2)
                    canvas.drawRect(bitmapRectF, bitmapLightPaint)
                    countRatio += ratioOffset
                }
                path.reset()
                bitmapLightPaint.xfermode = null
            }

            gradientMatrix.setRotate(mRotate, totalWidth / 2f, totalHeight / 2f)
            sweepGradient.setLocalMatrix(gradientMatrix)
            mRotate += rotateOffset
            if (mRotate >= 360f) {
                mRotate = 0f
            }
            invalidate()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun setColor(colors: IntArray) {
        this.colors = colors
        val oldSize = colors.size
        val colorArray = IntArray(oldSize + 2)
        val positionArray = FloatArray(oldSize + 2)
        val averageValue = 1 / oldSize.toFloat()
        for (i in colors.indices) {
            colorArray[i] = colors[i]
            positionArray[i] = i * averageValue
        }
        positionArray[oldSize] = 1f
        positionArray[oldSize + 1] = 1f
        colorArray[oldSize] = colors[0]
        colorArray[oldSize + 1] = colors[0]

        sweepGradient = SweepGradient(totalWidth / 2f, totalHeight / 2f, colorArray, positionArray)
        lightPaint.shader = sweepGradient
        bitmapLightPaint.shader = sweepGradient
    }

    fun setDrawMode(isLine: Boolean, resId: Int) {
        this.isLine = isLine
        bitmap = context.getDrawable(resId)!!
    }

    fun setSpeed(speed: Float) {
        this.rotateOffset = speed / 2f
    }

    fun setWidth(width: Float) {
        this.spaceSize = width / 4f
        this.lineSpaceSize = width / 2f
        lightPaint.strokeWidth = width
    }

    fun setTopRound(topRound: Float) {
        this.topRound = topRound
    }

    fun setBottomRound(topRound: Float) {
        this.bottomRound = topRound
    }

    fun reset() {
        this.mRotate = 0f
        this.rotateOffset = 15f
    }

}