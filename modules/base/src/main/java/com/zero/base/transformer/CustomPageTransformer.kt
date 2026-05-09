package com.zero.base.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class CustomPageTransformer : ViewPager2.PageTransformer {
    private val minScale = 0.85f
    private val minAlpha = 0.5f

    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width
        val pageHeight = page.height

        page.apply {
            val scaleFactor = minScale.coerceAtLeast(1 - kotlin.math.abs(position))
            val verticalMargin = pageHeight * (1 - scaleFactor) / 2
            val horizontalMargin = pageWidth * (1 - scaleFactor) / 2

            translationX = if (position < 0) {
                horizontalMargin - verticalMargin / 2
            } else {
                -horizontalMargin + verticalMargin / 2
            }

            scaleX = scaleFactor
            scaleY = scaleFactor

            alpha = minAlpha + (scaleFactor - minScale) / (1 - minScale) * (1 - minAlpha)
        }
    }
}