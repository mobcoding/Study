package com.rebuild.mixtube.ads

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

class FullNativeAdActivity : AppCompatActivity() {
    private var nativeAd: NativeAd? = null
    private val handler = Handler(Looper.getMainLooper())
    private var closeDelaySeconds = 3
    private var closeExposePercent = 65
    private var scene: String = AdScene.Download.key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scene = intent.getStringExtra(EXTRA_SCENE) ?: AdScene.Download.key
        val adType = intent.getStringExtra(EXTRA_AD_TYPE) ?: "full_native"
        closeDelaySeconds = ServiceLocator.remoteConfig.getLong("full_native_close_delay", 3).toInt().coerceAtLeast(0)
        closeExposePercent = ServiceLocator.remoteConfig.getLong("full_native_close_percent", 65).toInt().coerceIn(0, 100)
        Log.d(TAG, "adType=$adType, scene=$scene, delay=$closeDelaySeconds, percent=$closeExposePercent")
        if (!adType.equals("full_native", ignoreCase = true)) {
            setContentView(closeButton("Unsupported adType=$adType"))
            return
        }
        setContentView(R.layout.activity_native_ad)
        setupCloseGate()
        val cached = FullNativeAdCache.consume(scene)
        if (cached != null) {
            nativeAd = cached.nativeAd
            bindNativeAd(cached.nativeAd)
            Log.d(
                TAG,
                "full native used cached ad: scene=$scene source=${cached.unit.adsource} production=${cached.unit.placementid}"
            )
        } else {
            loadFullScreenNative(scene)
        }
    }

    private fun loadFullScreenNative(scene: String) {
        val referenceUnit = ServiceLocator.adManager.selectedUnitForScene(scene)
        val adUnitId = RealGoogleMobileAdsAdapter.TEST_NATIVE
        Log.d(
            TAG,
            "full native fallback load: scene=$scene source=${referenceUnit?.adsource} production=${referenceUnit?.placementid} test=$adUnitId"
        )
        AdLoader.Builder(this, adUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                bindNativeAd(ad)
                Log.d(TAG, "full native loaded: scene=$scene testUnit=$adUnitId")
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "full native failed: ${error.message}")
                    setContentView(closeButton("Native failed: ${error.message}"))
                }

                override fun onAdClicked() {
                    Log.d(TAG, "full native clicked")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "full native impression")
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    private fun bindNativeAd(ad: NativeAd) {
        val nativeView = findViewById<NativeAdView>(R.id.native_ad_view)
        val mediaView = findViewById<MediaView>(R.id.ad_media)
        val headline = findViewById<TextView>(R.id.ad_headline)
        val meta = findViewById<TextView>(R.id.ad_meta)
        val icon = findViewById<ImageView>(R.id.ad_app_icon)
        val cta = findViewById<AppCompatTextView>(R.id.ad_call_to_action)
        nativeView.headlineView = headline
        nativeView.callToActionView = cta
        nativeView.iconView = icon
        nativeView.mediaView = mediaView
        headline.text = ad.headline.orEmpty()
        meta.text = listOfNotNull(ad.advertiser, ad.store, ad.price, ad.body)
            .filter { it.isNotBlank() }
            .joinToString("  •  ")
            .ifBlank { "Sponsored" }
        val action = ad.callToAction
        cta.visibility = if (action.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
        cta.text = action.orEmpty()
        ad.icon?.let {
            icon.visibility = View.VISIBLE
            icon.setImageDrawable(it.drawable)
        } ?: run { icon.visibility = View.INVISIBLE }
        ad.mediaContent?.let(mediaView::setMediaContent)
        nativeView.setNativeAd(ad)
    }

    private fun setupCloseGate() {
        val count = findViewById<AppCompatTextView>(R.id.tv_count_time)
        val directClose = findViewById<AppCompatTextView>(R.id.iv_close)
        val additionalClose = findViewById<AppCompatTextView>(R.id.iv_close_addtional)
        val selectedClose = if ((0 until 100).random() < closeExposePercent) directClose else additionalClose
        directClose.visibility = View.GONE
        additionalClose.visibility = View.GONE
        count.visibility = if (closeDelaySeconds > 0) View.VISIBLE else View.GONE
        directClose.setOnClickListener { closeAndContinue() }
        additionalClose.setOnClickListener { closeAndContinue() }
        if (closeDelaySeconds <= 0) {
            selectedClose.visibility = View.VISIBLE
            return
        }
        tickCloseCountdown(closeDelaySeconds, count, selectedClose)
    }

    private fun tickCloseCountdown(remaining: Int, count: AppCompatTextView, close: AppCompatTextView) {
        count.text = remaining.toString()
        if (remaining <= 0) {
            count.visibility = View.GONE
            close.visibility = View.VISIBLE
            return
        }
        handler.postDelayed({ tickCloseCountdown(remaining - 1, count, close) }, 1000)
    }

    private fun closeButton(text: String): Button = Button(this).apply {
        this.text = "$text\nClose"
        setOnClickListener { closeAndContinue() }
    }

    private fun closeAndContinue() {
        PostAdNavigationManager.consumeAndStart(this)
        finish()
    }

    override fun onDestroy() {
        nativeAd?.destroy()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SCENE = "scene"
        const val EXTRA_AD_TYPE = "adType"
        private const val TAG = "FullNativeAd"
    }
}
