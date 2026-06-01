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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AdWinInterActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var remainingSeconds = 4
    private lateinit var countView: TextView
    private lateinit var closeView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.adManager.setAdWinShowing(true)
        val ad = ServiceLocator.adWinRepository.currentAd()
        ServiceLocator.adWinRepository.sendImpressions(ad)
        val scene = resolutionScene(ad)
        val source = "adwin"
        val type = "interstitial"
        val creative = SceneCreativeCatalog.resolve(scene, source, type)
        Log.d(TAG, "AdWinInterActivity onCreate: scene=$scene title=${ad.title}")
        setContentView(buildContent(ad, creative))
        if (remainingSeconds <= 0) {
            countView.visibility = View.GONE
            closeView.visibility = View.VISIBLE
        } else {
            tickCountdown()
        }
    }

    override fun onDestroy() {
        ServiceLocator.adManager.setAdWinShowing(false)
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "AdWinInterActivity onDestroy")
        super.onDestroy()
    }

    private fun resolutionScene(ad: AdWinNativeAd): String {
        val text = (ad.title + ad.description).lowercase()
        return when {
            text.contains("download") || text.contains("install") -> "mixIVDownload"
            text.contains("play") || text.contains("music") -> "mixIVplay"
            text.contains("switch") || text.contains("back") -> "mixIVswitchback"
            text.contains("clean") || text.contains("boost") -> "mixIVcleanBoost"
            else -> "mixIVother"
        }
    }

    private fun buildContent(ad: AdWinNativeAd, creative: SceneCreativeCatalog.Creative): View {
        return FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(creative.backgroundBase))
            addView(LinearLayout(this@AdWinInterActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(22), dp(54), dp(22), dp(28))
                addView(TextView(this@AdWinInterActivity).apply {
                    text = creative.sponsor
                    textSize = 13f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.parseColor("#BBFFFFFF"))
                    background = rounded("#22FFFFFF", "#22FFFFFF", 18)
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                })
                addView(space(18))
                addView(TextView(this@AdWinInterActivity).apply {
                    text = creative.headline
                    textSize = 26f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    maxLines = 2
                })
                addView(space(20))
                addView(LinearLayout(this@AdWinInterActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(18), dp(18), dp(18), dp(18))
                    background = rounded(creative.cardFill, "#22FFFFFF", 20)
                    addView(View(this@AdWinInterActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(210))
                        background = rounded(creative.accent, creative.accent, 14).apply { alpha = 35 }
                    })
                    addView(space(16))
                    addView(TextView(this@AdWinInterActivity).apply {
                        text = ad.title
                        textSize = 20f
                        setTypeface(typeface, Typeface.BOLD)
                        setTextColor(Color.WHITE)
                    })
                    addView(space(6))
                    addView(TextView(this@AdWinInterActivity).apply {
                        text = ad.description
                        textSize = 14f
                        setTextColor(Color.parseColor("#D1D5DB"))
                        maxLines = 3
                    })
                    addView(space(8))
                    addView(TextView(this@AdWinInterActivity).apply {
                        text = "AdWin Self Render  \u00b7  ${ad.downloadUrl.take(30)}"
                        textSize = 12f
                        setTextColor(Color.parseColor("#6B7280"))
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                })
                addView(space(18))
                addView(TextView(this@AdWinInterActivity).apply {
                    text = creative.body
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor("#D1D5DB"))
                    maxLines = 3
                })
                addView(space(22))
                addView(TextView(this@AdWinInterActivity).apply {
                    text = creative.cta
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.parseColor(creative.ctaText))
                    background = rounded(creative.ctaFill, creative.ctaFill, 26)
                    setPadding(dp(18), dp(15), dp(18), dp(15))
                    setOnClickListener {
                        ServiceLocator.adWinRepository.sendClick(ad)
                        ServiceLocator.markQueuedDownloads(true)
                        continueFlow()
                    }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(space(12))
                addView(TextView(this@AdWinInterActivity).apply {
                    text = "Close"
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor("#9CA3AF"))
                    background = rounded("#1F2937", "#1F2937", 24)
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                    visibility = View.GONE
                    closeView = this
                    setOnClickListener { continueFlow() }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            })
            addView(LinearLayout(this@AdWinInterActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(26), dp(18), 0)
                countView = TextView(this@AdWinInterActivity).apply {
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    minWidth = dp(36)
                    background = rounded("#33000000", "#33000000", 14)
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                }
                addView(countView)
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

    private fun rounded(fill: String, stroke: String, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(Color.parseColor(fill))
            setStroke(dp(1), Color.parseColor(stroke))
        }
    }

    private fun space(dpValue: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(dpValue))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "AdWinInterActivity"
    }
}
