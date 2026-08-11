package com.zero.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal object WidgetClockRenderer {
    private const val CLOCK_SIZE_DP = 88f
    private const val SECOND_RING_RADIUS = 32.5f
    private const val SECOND_HAND_TAIL_LENGTH = 10f
    private const val HOUR_HAND_WIDTH = 2.1f
    private const val HOUR_HAND_LENGTH = 21f
    private const val HOUR_HAND_LOOP_LENGTH = 14.7f
    private const val HOUR_HAND_OUTLINE = 0.42f
    private const val HOUR_HAND_TAIL_WIDTH = 0.63f
    private const val MINUTE_HAND_WIDTH = 2.1f
    private const val MINUTE_HAND_LENGTH = 29f
    private const val MINUTE_HAND_LOOP_END_OFFSET = 7.45f
    private const val MINUTE_HAND_TAIL_WIDTH = 0.63f

    fun render(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (CLOCK_SIZE_DP * density).roundToInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            this.density = context.resources.displayMetrics.densityDpi
        }
        val canvas = Canvas(bitmap)
        canvas.scale(size / CLOCK_SIZE_DP, size / CLOCK_SIZE_DP)

        val center = CLOCK_SIZE_DP / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.setShadowLayer(4f, 0f, 4f, 0x14000000)
        canvas.drawRoundRect(0f, 0f, CLOCK_SIZE_DP, CLOCK_SIZE_DP, 29.333f, 29.333f, paint)
        paint.clearShadowLayer()

        paint.strokeCap = Paint.Cap.ROUND
        for (index in 0 until 60) {
            val angle = Math.toRadians((index * 6f - 90f).toDouble())
            val startRadius = if (index % 5 == 0) 27.5f else 30.5f
            val endRadius = 32.5f
            paint.color = if (index % 5 == 0) 0x9934373A.toInt() else 0x5534373A
            paint.strokeWidth = if (index % 5 == 0) 0.7f else 0.35f
            canvas.drawLine(
                center + cos(angle).toFloat() * startRadius,
                center + sin(angle).toFloat() * startRadius,
                center + cos(angle).toFloat() * endRadius,
                center + sin(angle).toFloat() * endRadius,
                paint
            )
        }

        paint.color = 0xFF34373A.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 6.2f
        paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val labels = arrayOf("XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI")
        labels.forEachIndexed { index, label ->
            canvas.save()
            canvas.rotate(index * 30f, center, center)
            canvas.drawText(label, center, 11f, paint)
            canvas.restore()
        }

        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR)
        val minute = now.get(Calendar.MINUTE)
        val second = now.get(Calendar.SECOND)
        drawHourHand(canvas, center, hour * 30f + minute * 0.5f)
        drawMinuteHand(canvas, center, minute * 6f + second * 0.1f)
        drawSecondHand(canvas, paint, center, second * 6f)

        paint.color = 0xFFE65C5C.toInt()
        canvas.drawCircle(center, center, 2f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(center, center, 0.8f, paint)
        return bitmap
    }

    private fun drawHourHand(canvas: Canvas, center: Float, angle: Float) {
        val top = center - HOUR_HAND_LENGTH
        val loopBottom = top + HOUR_HAND_LOOP_LENGTH
        val outerRadius = HOUR_HAND_WIDTH / 2f
        val innerRadius = outerRadius - HOUR_HAND_OUTLINE
        val handPath = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRoundRect(
                center - outerRadius,
                top,
                center + outerRadius,
                loopBottom,
                outerRadius,
                outerRadius,
                Path.Direction.CW
            )
            addRoundRect(
                center - innerRadius,
                top + HOUR_HAND_OUTLINE,
                center + innerRadius,
                loopBottom - HOUR_HAND_OUTLINE,
                innerRadius,
                innerRadius,
                Path.Direction.CCW
            )
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }

        canvas.save()
        canvas.rotate(angle, center, center)
        canvas.drawPath(handPath, paint)
        canvas.drawRoundRect(
            center - HOUR_HAND_TAIL_WIDTH / 2f,
            loopBottom - HOUR_HAND_OUTLINE,
            center + HOUR_HAND_TAIL_WIDTH / 2f,
            center + HOUR_HAND_TAIL_WIDTH / 2f,
            HOUR_HAND_TAIL_WIDTH / 2f,
            HOUR_HAND_TAIL_WIDTH / 2f,
            paint
        )
        canvas.restore()
    }

    private fun drawMinuteHand(canvas: Canvas, center: Float, angle: Float) {
        val tip = center - MINUTE_HAND_LENGTH
        val loopBottom = center - MINUTE_HAND_LOOP_END_OFFSET
        val outerRadius = MINUTE_HAND_WIDTH / 2f
        val innerRadius = outerRadius - HOUR_HAND_OUTLINE
        val handPath = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRoundRect(
                center - outerRadius,
                tip,
                center + outerRadius,
                loopBottom,
                outerRadius,
                outerRadius,
                Path.Direction.CW
            )
            addRoundRect(
                center - innerRadius,
                tip + HOUR_HAND_OUTLINE,
                center + innerRadius,
                loopBottom - HOUR_HAND_OUTLINE,
                innerRadius,
                innerRadius,
                Path.Direction.CCW
            )
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }

        canvas.save()
        canvas.rotate(angle, center, center)
        canvas.drawPath(handPath, paint)
        canvas.drawRoundRect(
            center - MINUTE_HAND_TAIL_WIDTH / 2f,
            loopBottom - HOUR_HAND_OUTLINE,
            center + MINUTE_HAND_TAIL_WIDTH / 2f,
            center + MINUTE_HAND_TAIL_WIDTH / 2f,
            MINUTE_HAND_TAIL_WIDTH / 2f,
            MINUTE_HAND_TAIL_WIDTH / 2f,
            paint
        )
        canvas.restore()
    }

    private fun drawSecondHand(canvas: Canvas, paint: Paint, center: Float, angle: Float) {
        val radians = Math.toRadians((angle - 90f).toDouble())
        paint.color = 0xFFE65C5C.toInt()
        paint.strokeWidth = 0.8f
        canvas.drawLine(
            center - cos(radians).toFloat() * SECOND_HAND_TAIL_LENGTH,
            center - sin(radians).toFloat() * SECOND_HAND_TAIL_LENGTH,
            center + cos(radians).toFloat() * SECOND_RING_RADIUS,
            center + sin(radians).toFloat() * SECOND_RING_RADIUS,
            paint
        )
    }
}
