package com.rebuild.mixtube.ads

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CleanBoostActivity : AppCompatActivity() {
    private lateinit var mode: Mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getStringExtra(EXTRA_MODE)?.let(Mode::valueOf) ?: Mode.CLEAN
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(if (mode == Mode.CLEAN) "#16241E" else "#081B28"))
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        content.addView(header())
        if (mode == Mode.CLEAN) {
            content.addView(cleanBody())
        } else {
            content.addView(boostBody())
        }
        return ScrollView(this).apply { addView(content) }
    }

    private fun header(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(headerButton("Back") { finish() })
            addView(TextView(this@CleanBoostActivity).apply {
                text = if (mode == Mode.CLEAN) "Clean" else "Boost"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(headerButton("", onClick = { }))
        }
    }

    private fun cleanBody(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = marginParams(24)
            addView(progressCircle("128", "MB"))
            addView(titleText("Cleaning up..."))
            addView(subText("Scanning cache leftovers and stale audio pages."))
            addView(primaryButton("Cleanup completed") {
                Log.d(TAG, "clean complete tapped")
            })
        }
    }

    private fun boostBody(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = marginParams(24)
            addView(progressCircle("100", "%"))
            addView(titleText("Boost success"))
            addView(subText("The speed has reached the fastest"))
            addView(primaryButton("Back") { finish() })
        }
    }

    private fun progressCircle(top: String, bottom: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(220), dp(220))
            background = roundedBackground(
                if (mode == Mode.CLEAN) "#20372D" else "#123346",
                if (mode == Mode.CLEAN) "#54F563" else "#00FFFF",
                110
            )
            addView(TextView(this@CleanBoostActivity).apply {
                text = top
                textSize = 36f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            addView(TextView(this@CleanBoostActivity).apply {
                text = bottom
                textSize = 16f
                setTextColor(Color.parseColor("#CCFFFFFF"))
            })
        }
    }

    private fun titleText(text: String): View {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = marginParams(20)
        }
    }

    private fun subText(text: String): View {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.parseColor("#CCFFFFFF"))
            gravity = Gravity.CENTER
            layoutParams = marginParams(10)
        }
    }

    private fun primaryButton(text: String, onClick: () -> Unit): View {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor(if (mode == Mode.CLEAN) "#2A3939" else "#2A3939"))
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(14), dp(24), dp(14))
            background = roundedBackground(
                if (mode == Mode.CLEAN) "#54F563" else "#00FFFF",
                if (mode == Mode.CLEAN) "#54F563" else "#00FFFF",
                12
            )
            layoutParams = LinearLayout.LayoutParams(dp(180), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(28)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun headerButton(text: String, onClick: () -> Unit): View {
        return TextView(this).apply {
            this.text = text
            minWidth = dp(40)
            textSize = 13f
            setTextColor(Color.WHITE)
            setOnClickListener { onClick() }
        }
    }

    private fun roundedBackground(fill: String, stroke: String, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(radiusDp).toFloat()
            setColor(Color.parseColor(fill))
            setStroke(dp(2), Color.parseColor(stroke))
        }
    }

    private fun marginParams(top: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(top)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    enum class Mode {
        CLEAN,
        BOOST
    }

    companion object {
        const val EXTRA_MODE = "mode"
        private const val TAG = "CleanBoostActivity"
    }
}
