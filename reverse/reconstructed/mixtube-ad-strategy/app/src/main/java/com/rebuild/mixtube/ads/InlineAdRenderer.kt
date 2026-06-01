package com.rebuild.mixtube.ads

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

class InlineAdRenderer(private val context: Context) {
    private var nativeAd: NativeAd? = null
    private var bannerView: AdView? = null

    fun load(scene: String, container: FrameLayout, style: Style) {
        destroy()
        container.removeAllViews()
        val units = ServiceLocator.adConfigRepository.unitsFor(scene)
        if (units.isEmpty()) {
            container.addView(infoText("No inline placement config for $scene"))
            return
        }
        val unit = ServiceLocator.weightedSelector.select(scene, units)
        val testPlacementId = RealGoogleMobileAdsAdapter.testPlacementIdForType(unit.adtype)
        Log.d(TAG, "load scene=$scene source=${unit.adsource} type=${unit.adtype} production=${unit.placementid} test=$testPlacementId style=$style")
        when (unit.adtype.lowercase()) {
            "banner" -> loadBanner(container, testPlacementId)
            else -> loadNative(container, testPlacementId, style)
        }
    }

    fun destroy() {
        nativeAd?.destroy()
        nativeAd = null
        bannerView?.destroy()
        bannerView = null
    }

    private fun loadBanner(container: FrameLayout, adUnitId: String) {
        val banner = AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "banner loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "banner failed: ${error.message}")
                    container.removeAllViews()
                    container.addView(infoText("Banner failed: ${error.message}"))
                }
            }
        }
        bannerView = banner
        container.addView(FrameLayout(context).apply {
            foregroundGravity = Gravity.CENTER
            addView(banner)
        })
        banner.loadAd(AdRequest.Builder().build())
    }

    private fun loadNative(container: FrameLayout, adUnitId: String, style: Style) {
        val slot = FrameLayout(context).apply {
            addView(infoText(context.getString(R.string.business_inline_loading)))
        }
        container.addView(slot)
        AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                slot.removeAllViews()
                slot.addView(renderNative(ad, style))
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "native failed: ${error.message}")
                    slot.removeAllViews()
                    slot.addView(infoText("Native failed: ${error.message}"))
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    private fun renderNative(ad: NativeAd, style: Style): NativeAdView {
        val nativeView = NativeAdView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(style.paddingDp), dp(style.paddingDp), dp(style.paddingDp), dp(style.paddingDp))
            background = roundedBackground(
                fillColor = if (style == Style.PLAYER_OVERLAY) "#E61A1F27" else "#26FFFFFF",
                strokeColor = if (style == Style.PLAYER_OVERLAY) "#66FFFFFF" else "#3AFFFFFF",
                radiusDp = style.radiusDp
            )
        }
        val badge = TextView(context).apply {
            text = "Ad"
            textSize = 11f
            setTextColor(Color.parseColor("#262626"))
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(6), dp(3), dp(6), dp(3))
            background = roundedBackground("#86C0FC", "#86C0FC", 4)
        }
        val mediaView = MediaView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(style.mediaHeightDp)
            )
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(context).apply {
            text = ad.headline
            textSize = if (style == Style.SEARCH_NATIVE) 15f else 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            maxLines = 1
        }
        val body = TextView(context).apply {
            text = ad.body ?: ad.advertiser ?: "Sponsored"
            textSize = 12f
            setTextColor(Color.parseColor("#CCFFFFFF"))
            maxLines = 2
        }
        val cta = Button(context).apply {
            text = ad.callToAction ?: "Install"
            textSize = 12f
            setTextColor(Color.WHITE)
            background = roundedBackground("#D73F36", "#D73F36", 6)
        }
        row.addView(TextView(context).apply {
            text = ad.advertiser ?: "Promoted"
            textSize = 12f
            setTextColor(Color.parseColor("#99FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(cta)

        root.addView(badge)
        root.addView(space(8))
        root.addView(mediaView)
        root.addView(space(8))
        root.addView(title)
        root.addView(space(4))
        root.addView(body)
        root.addView(space(10))
        root.addView(row)
        nativeView.addView(root)
        nativeView.mediaView = mediaView
        nativeView.headlineView = title
        nativeView.bodyView = body
        nativeView.callToActionView = cta
        nativeView.setNativeAd(ad)
        return nativeView
    }

    private fun roundedBackground(fillColor: String, strokeColor: String, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(Color.parseColor(fillColor))
            setStroke(dp(1), Color.parseColor(strokeColor))
        }
    }

    private fun infoText(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.parseColor("#CCFFFFFF"))
        }
    }

    private fun space(dpValue: Int): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(dpValue)
            )
        }
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    enum class Style(
        val mediaHeightDp: Int,
        val paddingDp: Int,
        val radiusDp: Int
    ) {
        LARGE_NATIVE(mediaHeightDp = 176, paddingDp = 12, radiusDp = 12),
        SEARCH_NATIVE(mediaHeightDp = 160, paddingDp = 12, radiusDp = 12),
        PLAYER_OVERLAY(mediaHeightDp = 150, paddingDp = 12, radiusDp = 12),
        QUEUE_PANEL(mediaHeightDp = 138, paddingDp = 12, radiusDp = 12),
        BOTTOM_BANNER(mediaHeightDp = 0, paddingDp = 0, radiusDp = 0)
    }

    private companion object {
        const val TAG = "InlineAdRenderer"
    }
}
