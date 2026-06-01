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

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#11161E"))
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        content.addView(header())
        content.addView(rewardRow())
        content.addView(settingRow("Privacy Policy", "Open policy details"))
        content.addView(settingRow("User Protocol", "View service terms"))
        content.addView(settingRow("Feedback", "Send your suggestion"))
        content.addView(settingRow("About Us", "App version and team"))
        content.addView(settingRow("Clear Cache", "128 MB"))
        content.addView(settingRow("YouTube Login", "Connected account"))
        return ScrollView(this).apply { addView(content) }
    }

    private fun header(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(headerButton("Back") { finish() })
            addView(TextView(this@SettingActivity).apply {
                text = "Setting"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(headerButton("", onClick = { }))
        }
    }

    private fun rewardRow(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = marginParams(20)
            background = roundedBackground("#26FFFFFF", "#33FFFFFF", 12)
            addView(LinearLayout(this@SettingActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@SettingActivity).apply {
                    text = "Reward Ad"
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                addView(TextView(this@SettingActivity).apply {
                    text = "Remaining time: 00:00:30"
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(6)
                    }
                })
            })
            addView(TextView(this@SettingActivity).apply {
                text = "Watch"
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#262626"))
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = roundedBackground("#5AEEEE", "#5AEEEE", 12)
                setOnClickListener { triggerSettingReward() }
            })
        }
    }

    private fun settingRow(title: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = marginParams(0)
            background = roundedBackground("#11161E", "#1D2430", 0)
            addView(TextView(this@SettingActivity).apply {
                text = title
                textSize = 15f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@SettingActivity).apply {
                text = value
                textSize = 12f
                setTextColor(Color.parseColor("#99FFFFFF"))
            })
        }
    }

    private fun triggerSettingReward() {
        val result = ServiceLocator.adManager.showScene(this, AdScene.Setting.key)
        Log.d(TAG, "setting reward scene -> ${result.message}")
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
            if (radiusDp > 0) {
                setStroke(dp(1), Color.parseColor(stroke))
            }
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

    private companion object {
        const val TAG = "SettingActivity"
    }
}
