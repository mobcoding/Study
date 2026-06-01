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
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ToponSplashAdShowActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var remainingSeconds = 5
    private lateinit var closeView: TextView
    private lateinit var countdownView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val placementId = intent.getStringExtra(EXTRA_PLACEMENT_ID).orEmpty()
        val scene = intent.getStringExtra(EXTRA_SCENE).orEmpty()
        val source = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "splash"
        remainingSeconds = intent.getIntExtra(EXTRA_CLOSE_DELAY, 5).coerceAtLeast(3)
        val creative = SceneCreativeCatalog.resolve(scene, source, type)
        setContentView(buildOriginalStyleLayout(creative, source))
        if (remainingSeconds <= 0) {
            countdownView.visibility = View.GONE
            closeView.visibility = View.VISIBLE
        } else {
            tickSkip()
        }
    }

    private fun buildOriginalStyleLayout(creative: SceneCreativeCatalog.Creative, source: String): RelativeLayout {
        return RelativeLayout(this).apply {
            setBackgroundColor(Color.parseColor(creative.backgroundBase))
            addView(buildAppInfoBar())
            addView(buildForegroundCard(creative, source))
            addView(buildTopController())
            addView(buildAdCircleBadge())
            addView(buildCtaButton(creative))
        }
    }

    private fun buildTopController(): RelativeLayout {
        val container = RelativeLayout(this).apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(9), dp(9), dp(9), 0) }
        }
        container.addView(TextView(this).apply {
            id = View.generateViewId()
            text = "\u2139"
            textSize = 16f
            setTextColor(Color.parseColor("#FFFFFFFF"))
            gravity = Gravity.CENTER
            layoutParams = RelativeLayout.LayoutParams(dp(35), dp(35)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        })
        closeView = TextView(this).apply {
            text = "Close ->"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.thinkup_splash_close_bg)
            setPadding(dp(10), 0, dp(10), 0)
            visibility = View.GONE
            setOnClickListener { closeAndContinue() }
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(28)
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        }
        container.addView(closeView)
        countdownView = TextView(this).apply {
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.thinkup_myoffer_splash_bg_skip)
            setPadding(dp(14), 0, dp(11), 0)
            textSize = 12f
            setTextColor(Color.WHITE)
            setOnClickListener { if (remainingSeconds <= 0) closeAndContinue() }
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(24)
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        }
        container.addView(countdownView)
        return container
    }

    private fun buildForegroundCard(creative: SceneCreativeCatalog.Creative, source: String): RelativeLayout {
        val card = RelativeLayout(this).apply {
            id = View.generateViewId()
            setBackgroundResource(R.drawable.thinkup_shape_splash_corners_14)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(10), dp(130), dp(10), 0) }
        }
        val iconView = View(this).apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(dp(40), dp(40))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor(creative.accent))
            }
        }
        card.addView(iconView)
        card.addView(TextView(this).apply {
            text = creative.headline
            textSize = 16f
            setTextColor(Color.BLACK)
            setSingleLine(true)
            ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, 0, 0)
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)
            ).apply {
                addRule(RelativeLayout.END_OF, iconView.id)
                addRule(RelativeLayout.ALIGN_TOP, iconView.id)
            }
        })
        val imageContainer = RelativeLayout(this).apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(185)
            ).apply {
                addRule(RelativeLayout.BELOW, iconView.id)
                topMargin = dp(12)
            }
        }
        imageContainer.addView(View(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor(creative.accent))
                alpha = 40
            }
        })
        imageContainer.addView(TextView(this).apply {
            text = source.uppercase() + " Splash"
            textSize = 14f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { addRule(RelativeLayout.CENTER_IN_PARENT) }
        })
        imageContainer.addView(TextView(this).apply {
            text = "AD"
            textSize = 10f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.thinkup_shape_splash_rightbottom_corners_10)
            setPadding(dp(3), 0, dp(3), 0)
            layoutParams = RelativeLayout.LayoutParams(dp(34), dp(14)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.ALIGN_PARENT_END)
            }
        })
        card.addView(imageContainer)
        return card
    }

    private fun buildCtaButton(creative: SceneCreativeCatalog.Creative): Button {
        return Button(this).apply {
            text = creative.cta
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setOnClickListener {
                Log.d(TAG, "CTA clicked, continuing flow")
                closeAndContinue()
            }
            layoutParams = RelativeLayout.LayoutParams(
                dp(280), ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                bottomMargin = dp(60)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setColor(Color.parseColor(creative.ctaFill))
            }
        }
    }

    private fun buildAppInfoBar(): RelativeLayout {
        val bar = RelativeLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                setMargins(dp(10), 0, dp(10), dp(10))
            }
        }
        bar.addView(TextView(this).apply {
            id = View.generateViewId()
            text = "Mixtube . App info"
            textSize = 11f
            setTextColor(Color.WHITE)
            setLineSpacing(1.0f, 1.2f)
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        })
        val featuresBtn = makeGrayButton("Features")
        val permissionBtn = makeGrayButton("Permission")
        val privacyBtn = makeGrayButton("Privacy")
        bar.addView(privacyBtn.apply {
            (layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_END)
        })
        bar.addView(permissionBtn.apply {
            (layoutParams as RelativeLayout.LayoutParams).apply {
                addRule(RelativeLayout.START_OF, privacyBtn.id)
                marginEnd = dp(6)
            }
        })
        bar.addView(featuresBtn.apply {
            (layoutParams as RelativeLayout.LayoutParams).apply {
                addRule(RelativeLayout.START_OF, permissionBtn.id)
                marginEnd = dp(6)
            }
        })
        return bar
    }

    private fun makeGrayButton(text: String): TextView {
        return TextView(this).apply {
            id = View.generateViewId()
            this.text = text
            textSize = 8f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.thinkup_splash_button_bg_gray)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { addRule(RelativeLayout.CENTER_VERTICAL) }
        }
    }

    private fun buildAdCircleBadge(): TextView {
        return TextView(this).apply {
            text = "AD"
            textSize = 10f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.thinkup_shape_splash_circle_14)
            setPadding(dp(3), 0, dp(3), 0)
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(14)
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.ALIGN_PARENT_END)
                setMargins(0, 0, dp(5), dp(5))
            }
        }
    }

    private fun tickSkip() {
        countdownView.text = if (remainingSeconds > 0) "Skip " + remainingSeconds + "s" else "Skip ->"
        if (remainingSeconds <= 0) {
            countdownView.visibility = View.GONE
            closeView.visibility = View.VISIBLE
            return
        }
        handler.postDelayed({
            remainingSeconds -= 1
            if (!isFinishing) tickSkip()
        }, 1000L)
    }

    private fun closeAndContinue() {
        PostAdNavigationManager.consumeAndStart(this)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_PLACEMENT_ID = "placementId"
        const val EXTRA_SCENE = "scene"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_TYPE = "type"
        const val EXTRA_CLOSE_DELAY = "close_delay"
        private const val TAG = "ToponSplashAd"
    }
}
