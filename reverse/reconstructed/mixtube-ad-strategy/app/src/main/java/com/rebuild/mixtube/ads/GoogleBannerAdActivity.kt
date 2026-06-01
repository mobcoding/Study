package com.rebuild.mixtube.ads

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

class GoogleBannerAdActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val adUnit = intent.getStringExtra(RealGoogleMobileAdsAdapter.EXTRA_AD_UNIT).orEmpty()
        val adView = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = adUnit
            loadAd(AdRequest.Builder().build())
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(TextView(this@GoogleBannerAdActivity).apply {
                text = "Real AdMob Banner\n$adUnit"
                textSize = 18f
                gravity = Gravity.CENTER
            })
            addView(adView)
        })
    }
}
