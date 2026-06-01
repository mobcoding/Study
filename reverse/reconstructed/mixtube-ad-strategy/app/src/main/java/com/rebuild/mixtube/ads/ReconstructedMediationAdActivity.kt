package com.rebuild.mixtube.ads

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ReconstructedMediationAdActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var remainingSeconds = 3
    private lateinit var countView: TextView
    private lateinit var closeView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scene = intent.getStringExtra(EXTRA_SCENE).orEmpty()
        val source = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
        val type = intent.getStringExtra(EXTRA_TYPE).orEmpty()
        val placement = intent.getStringExtra(EXTRA_PLACEMENT).orEmpty()
        remainingSeconds = intent.getIntExtra(EXTRA_CLOSE_DELAY, 3).coerceAtLeast(0)
        Log.d(TAG, "open scene=$scene source=$source type=$type placement=$placement")
        setContentView(buildContent(scene, source, type, placement))
        if (remainingSeconds == 0) {
            countView.visibility = View.GONE
            closeView.visibility = View.VISIBLE
        } else {
            tickCountdown()
        }
    }

    private fun buildContent(scene: String, source: String, type: String, placement: String): View {
        val creative = SceneCreativeCatalog.resolve(scene, source, type)
        return FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(creative.backgroundBase))
            addView(LinearLayout(this@ReconstructedMediationAdActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(24), dp(66), dp(28), dp(28))
                addView(TextView(this@ReconstructedMediationAdActivity).apply {
                    text = creative.sponsor
                    textSize = 13f
                    setTextColor(Color.parseColor("#CCFFFFFF"))
                    setTypeface(typeface, Typeface.BOLD)
                    background = rounded("#22FFFFFF", "#22FFFFFF", 18)
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                })
                addView(space(18))
                addView(TextView(this@ReconstructedMediationAdActivity).apply {
                    text = creative.headline
                    textSize = 28f
                    gravity = Gravity.CENTER
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    maxLines = 3
                })
                addView(space(20))
                addView(LinearLayout(this@ReconstructedMediationAdActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(22), dp(22), dp(22), dp(22))
                    background = rounded(creative.cardFill, "#22FFFFFF", 22)
                    addView(View(this@ReconstructedMediationAdActivity).apply {
                        background = rounded(creative.accent, creative.accent, 18).apply { alpha = 30 }
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200))
                    })
                    addView(space(14))
                    addView(TextView(this@ReconstructedMediationAdActivity).apply {
                        text = "${source.uppercase()} | ${creative.category}"
                        textSize = 13f
                        setTextColor(Color.parseColor("#9CA3AF"))
                        gravity = Gravity.CENTER
                    })
                })
                addView(space(18))
                addView(TextView(this@ReconstructedMediationAdActivity).apply {
                    text = creative.body
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor("#D1D5DB"))
                    maxLines = 4
                })
                addView(space(26))
                addView(Button(this@ReconstructedMediationAdActivity).apply {
                    text = creative.cta
                    textSize = 16f
                    setTextColor(Color.parseColor(creative.ctaText))
                    background = rounded(creative.ctaFill, creative.ctaFill, 26)
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                    setOnClickListener {
                        if (scene.contains("Download", ignoreCase = true)) {
                            ServiceLocator.markQueuedDownloads(true)
                        }
                        Log.d(TAG, "cta click scene=$scene source=$source type=$type")
                        continueFlow()
                    }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

            addView(LinearLayout(this@ReconstructedMediationAdActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(26), dp(18), 0)
                countView = TextView(this@ReconstructedMediationAdActivity).apply {
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    minWidth = dp(36)
                    background = rounded("#33000000", "#33000000", 14)
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                }
                closeView = TextView(this@ReconstructedMediationAdActivity).apply {
                    text = "\u2715"
                    textSize = 18f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    visibility = View.GONE
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    background = rounded("#33000000", "#33000000", 14)
                    setOnClickListener { continueFlow() }
                }
                addView(countView)
                addView(closeView)
            }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun tickCountdown() {
        countView.text = remainingSeconds.toString()
        if (remainingSeconds <= 0) {
            countView.visibility = View.GONE
            closeView.visibility = View.VISIBLE
            return
        }
        handler.postDelayed({
            remainingSeconds -= 1
            if (!isFinishing) tickCountdown()
        }, 1000L)
    }

    private fun continueFlow() {
        PostAdNavigationManager.consumeAndStart(this)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun rounded(fill: String, stroke: String, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(Color.parseColor(fill))
            setStroke(dp(1), Color.parseColor(stroke))
        }
    }

    private fun space(dpValue: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(dpValue))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_SCENE = "scene"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_TYPE = "type"
        const val EXTRA_PLACEMENT = "placement"
        const val EXTRA_CLOSE_DELAY = "close_delay"
        private const val TAG = "ReconstructedAd"
    }
}
