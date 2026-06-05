package com.zero.study.ui.activity

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.toolkit.admob_libray.BuildConfig
import com.zero.base.activity.BaseActivity
import com.zero.study.databinding.ActivityFullNativeAdBinding

class FullNativeAdActivity : BaseActivity<ActivityFullNativeAdBinding>(ActivityFullNativeAdBinding::inflate) {
    override val useRootWindowInsetsPadding: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private var nativeAd: NativeAd? = null
    private var scene: String = SCENE_NATIVE_HOME
    private var closeDelaySeconds = 3

    override fun initData() {
        scene = intent.getStringExtra(EXTRA_SCENE) ?: SCENE_NATIVE_HOME
    }

    override fun initView() {
        applySystemBarPadding(binding.tvCountTime, top = true, right = true)
        applySystemBarPadding(binding.ivClose, top = true, right = true)
        applySystemBarPadding(binding.ivCloseAdditional, top = true, left = true)
        applySystemBarPadding(binding.nativeAdView, bottom = true)
        setupCloseGate()
        loadFullScreenNativeAd()
    }

    override fun addListener() {
        binding.ivClose.setOnClickListener { finish() }
        binding.ivCloseAdditional.setOnClickListener { finish() }
    }

    private fun setupCloseGate() {
        binding.ivClose.visibility = View.GONE
        binding.ivCloseAdditional.visibility = View.GONE
        binding.tvCountTime.visibility = View.VISIBLE
        tickCloseCountdown(closeDelaySeconds)
    }

    private fun tickCloseCountdown(remaining: Int) {
        binding.tvCountTime.text = remaining.toString()
        if (remaining <= 0) {
            binding.tvCountTime.visibility = View.GONE
            val showDirectClose = (0 until 100).random() < CLOSE_DIRECT_EXPOSE_PERCENT
            if (showDirectClose) {
                binding.ivClose.visibility = View.VISIBLE
            } else {
                binding.ivCloseAdditional.visibility = View.VISIBLE
            }
            return
        }
        handler.postDelayed({ tickCloseCountdown(remaining - 1) }, 1_000)
    }

    private fun loadFullScreenNativeAd() {
        Log.d(TAG, "FullNativeAd load start: scene=$scene, unitId=${BuildConfig.NATIVE_BANNER_HOME}")
        AdLoader.Builder(this, BuildConfig.NATIVE_BANNER_HOME)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                bindNativeAd(ad)
                Log.d(TAG, "FullNativeAd loaded: scene=$scene")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "FullNativeAd failed: scene=$scene, error=${error.message}")
                    finish()
                }

                override fun onAdClicked() {
                    Log.d(TAG, "FullNativeAd clicked: scene=$scene")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "FullNativeAd impression: scene=$scene")
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    private fun bindNativeAd(ad: NativeAd) {
        val nativeView: NativeAdView = binding.nativeAdView
        val mediaView: MediaView = binding.adMedia
        val headline: TextView = binding.adHeadline
        val meta: TextView = binding.adMeta
        val icon: ImageView = binding.adAppIcon
        val cta: TextView = binding.adCallToAction

        nativeView.headlineView = headline
        nativeView.mediaView = mediaView
        nativeView.iconView = icon
        nativeView.callToActionView = cta

        headline.text = ad.headline.orEmpty()
        meta.text = listOfNotNull(ad.advertiser, ad.store, ad.price, ad.body)
            .filter { it.isNotBlank() }
            .joinToString("  |  ")
            .ifBlank { "Sponsored" }

        val action = ad.callToAction
        cta.visibility = if (action.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
        cta.text = action.orEmpty()

        ad.icon?.let {
            icon.visibility = View.VISIBLE
            icon.setImageDrawable(it.drawable)
        } ?: run {
            icon.visibility = View.INVISIBLE
        }
        ad.mediaContent?.let(mediaView::setMediaContent)
        nativeView.setNativeAd(ad)
    }

    override fun onDestroy() {
        nativeAd?.destroy()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SCENE = "scene"
        const val SCENE_NATIVE_HOME = "mixnative"
        private const val CLOSE_DIRECT_EXPOSE_PERCENT = 65
        private const val TAG = "FullNativeAd"
    }
}
