package com.zero.base.ext

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.annotation.LayoutRes
import java.util.concurrent.TimeUnit

/**
 * @date:2024/12/11 17:01
 * @path:com.zero.base.ext.ViewExt
 */

inline fun <reified T : Activity> Context.startActivity() {
    startActivity(Intent(this, T::class.java))
}

inline fun <reified T : Activity> Context.startActivity(noinline block: Intent.() -> Unit = {}) {
    val intent = Intent(this, T::class.java).apply(block)
    startActivity(intent)
}

fun ViewGroup.inflate(@LayoutRes layoutRes: Int): View =
    LayoutInflater.from(context).inflate(layoutRes, this, false)

fun View.onClick(skipDuration: Long = 500, block: (View) -> Unit) {
    val throttle = Throttle(skipDuration, TimeUnit.MILLISECONDS)
    setOnClickListener {
        if (throttle.needSkip()) return@setOnClickListener
        block(it)
    }
}

fun View.animateHeart(minScale: Float = 0.8f, maxScale: Float = 1.0f, duration: Long = 1000) {
    ValueAnimator.ofFloat(minScale, maxScale, minScale).apply {
        this.duration = duration
        interpolator = LinearInterpolator()
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE

        addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            scaleX = animatedValue
            scaleY = animatedValue
            alpha = 1.0f - (maxScale - animatedValue) * 0.2f
        }
        start()
    }
}

fun View.moveAnimal(duration: Long = 1000) {
    ObjectAnimator.ofFloat(this, "translationX", -this.width.toFloat() + 15,
        this.width.toFloat() - 15.dp).apply {
        this.duration = duration
        interpolator = LinearInterpolator()
        repeatCount = ObjectAnimator.INFINITE
        repeatMode = ObjectAnimator.RESTART
        start()
    }
}
