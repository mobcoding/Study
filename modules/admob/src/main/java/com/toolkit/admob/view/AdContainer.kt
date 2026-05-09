package com.toolkit.admob.view

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.toolkit.admob_libray.R

/**
 * Base class for a template view. *
 */
class AdContainer : FrameLayout {
    private var lottieAnimationView: LottieAnimationView? = null

    private var nativeAd: NativeAd? = null
    private var nativeAdView: NativeAdView? = null

    private var headlineView: TextView? = null
    private var bodyView: TextView? = null
    private var iconView: ImageView? = null
    private var callToActionView: TextView? = null

    constructor(context: Context?) : super(context!!)

    constructor(context: Context, layoutRes: Int) : super(context) {
        initView(context, layoutRes)
    }

    fun setNativeAd(nativeAd: NativeAd) {
        this.nativeAd = nativeAd

        val headline = nativeAd.headline
        val body = nativeAd.body
        val icon = nativeAd.icon
        val cta = nativeAd.callToAction


        headlineView!!.text = headline
        nativeAdView!!.headlineView = headlineView

        callToActionView!!.text = cta
        nativeAdView!!.callToActionView = callToActionView

        if (icon != null) {
            iconView!!.visibility = VISIBLE
            iconView!!.setImageDrawable(icon.drawable)
            nativeAdView!!.iconView = iconView
        } else {
            iconView!!.visibility = GONE
        }

        if (bodyView != null) {
            bodyView!!.text = body
            nativeAdView!!.bodyView = bodyView
        }

        nativeAdView!!.setNativeAd(nativeAd)

        nativeAdView!!.visibility = VISIBLE
        lottieAnimationView!!.visibility = GONE
    }


    private fun initView(context: Context, layoutRes: Int) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(layoutRes, this)
        lottieAnimationView = findViewById(R.id.skeletonLoading)

        nativeAdView = findViewById(R.id.native_ad_view)
        headlineView = findViewById(R.id.ad_headline)
        bodyView = findViewById(R.id.ad_body)

        callToActionView = findViewById(R.id.ad_call_to_action)
        iconView = findViewById(R.id.ad_app_icon)
    }
}
