package com.rebuild.mixtube.ads

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

class GoogleNativeAdActivity : AppCompatActivity() {
    private var nativeAd: NativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val adUnit = intent.getStringExtra(RealGoogleMobileAdsAdapter.EXTRA_AD_UNIT).orEmpty()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            addView(TextView(this@GoogleNativeAdActivity).apply {
                text = "Loading real AdMob Native\n$adUnit"
                textSize = 18f
                gravity = Gravity.CENTER
            })
        }
        setContentView(container)

        val loader = AdLoader.Builder(this, adUnit)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                container.removeAllViews()
                container.addView(renderNativeAd(ad))
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "native failed: ${error.message}")
                    container.addView(TextView(this@GoogleNativeAdActivity).apply { text = "Native failed: ${error.message}" })
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }

    private fun renderNativeAd(ad: NativeAd): NativeAdView {
        val view = NativeAdView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val headline = TextView(this).apply { textSize = 22f; text = ad.headline }
        val body = TextView(this).apply { text = ad.body ?: "" }
        val icon = ImageView(this).apply {
            setImageDrawable(ad.icon?.drawable)
            visibility = if (ad.icon == null) View.GONE else View.VISIBLE
        }
        val cta = Button(this).apply { text = ad.callToAction ?: "Open" }
        val media = FrameLayout(this)
        layout.addView(headline)
        layout.addView(icon, LinearLayout.LayoutParams(128, 128))
        layout.addView(body)
        layout.addView(media, LinearLayout.LayoutParams(-1, 360))
        layout.addView(cta)
        view.addView(layout)
        view.headlineView = headline
        view.bodyView = body
        view.iconView = icon
        view.callToActionView = cta
        view.mediaView = com.google.android.gms.ads.nativead.MediaView(this).also { media.addView(it, FrameLayout.LayoutParams(-1, -1)) }
        view.setNativeAd(ad)
        return view
    }

    override fun onDestroy() {
        nativeAd?.destroy()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "RealNative"
    }
}
