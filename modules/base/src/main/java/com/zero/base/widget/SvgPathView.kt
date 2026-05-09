package com.zero.base.widget

/**
 * @date:2025/1/6 19:05
 * @path:com.editor.plus.PathView
 */
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.PathParser

class SvgPathView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint: Paint = Paint().apply {
        color = Color.RED // 设置颜色
        style = Paint.Style.FILL_AND_STROKE // 设置为描边
        strokeWidth = 2f // 设置笔触宽度
    }
    private var svgPath: Path? = null
    private var shapeWidth: Float = 0f
    private var shapeHeight: Float = 0f
    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f

    init {
        // 获取设备屏幕宽高
        val displayMetrics = context.resources.displayMetrics
        screenWidth = displayMetrics.widthPixels.toFloat()
        screenHeight = displayMetrics.heightPixels.toFloat()
    }

    fun updatePath(pathData: String, shapeWidth: Float, shapeHeight: Float) {
        this.svgPath = PathParser.createPathFromPathData(pathData)
        this.shapeWidth = shapeWidth
        this.shapeHeight = shapeHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        svgPath?.let {
            canvas.drawPath(transformPath(it, shapeWidth, shapeHeight), paint)
        }
    }

    private fun transformPath(svgPath: Path, width: Float, height: Float): Path {
        val newPath = Path()
        svgPath.let {
            val matrix = Matrix()
            val scaleX = width / screenWidth
            val scaleY = height / screenHeight
            val scale = if (scaleX < scaleY) scaleX else scaleY
            matrix.postScale(scale, scale)
            matrix.postTranslate(200F, 200F)
            svgPath.transform(matrix, newPath)
        }
        return newPath
    }

}