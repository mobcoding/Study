package com.toolkit.admob.manager

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.interstitial.InterstitialAdPreloader
import com.google.android.gms.ads.preload.PreloadCallbackV2
import com.google.android.gms.ads.preload.PreloadConfiguration
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Admin
 */
object InterstitialPreloadAdMobManager {
    private var lastDismissStamp: Long = 0
    private const val COOLING_INTERVAL = (20 * 1000).toLong()
    val isAdShowing = AtomicBoolean(false)


    fun preLoadInterstitialAd(unitId: String) {
        val configuration = PreloadConfiguration.Builder(unitId).setBufferSize(1).build()

        val callback = object : PreloadCallbackV2() {
            override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo?) {
                // Called when preloaded ads are available.
                AdMobManager.logMsg("InterstitialAd onAdLoaded()")
            }

            override fun onAdsExhausted(preloadId: String) {
                AdMobManager.logMsg("InterstitialAd onAdsExhausted(): $preloadId")
                // Called when no preloaded ads are available.
            }

            override fun onAdFailedToPreload(preloadId: String, adError: AdError) {
                AdMobManager.logMsg(
                    "InterstitialAd onAdFailedToPreload(): $preloadId->${adError.message}")
                // Called when preloaded ads failed to load.
            }
        }
        InterstitialAdPreloader.start(unitId, configuration, callback)
    }


    fun tryShow(activity: Activity, unitId: String, checkCooling: Boolean = true,
                thenAction: () -> Unit) {
        val activityRef = WeakReference(activity)
        val currentActivity = activityRef.get() ?: run {
            AdMobManager.logMsg("InterstitialAd tryShow skip: Activity is Recycled")
            thenAction()
            return
        }
        if (currentActivity.isFinishing || currentActivity.isDestroyed) {
            thenAction()
            return
        }
        if (isAdShowing.get()) {
            AdMobManager.logMsg("InterstitialAd skip: ad is showing")
            thenAction()
            return
        }
        val now = System.currentTimeMillis()
        if (checkCooling && (now - lastDismissStamp) < COOLING_INTERVAL) {
            val remaining = (COOLING_INTERVAL - (now - lastDismissStamp)) / 1000
            AdMobManager.logMsg("InterstitialAd tryShow skip: cooling, remaining $remaining s")
            thenAction()
            return
        }

        if (!InterstitialAdPreloader.isAdAvailable(unitId)) {
            AdMobManager.logMsg("InterstitialAd is not Available")
            thenAction()
            return
        }
        val ad = InterstitialAdPreloader.pollAd(unitId)

        ad?.onPaidEventListener = OnPaidEventListener {
            AdMobManager.logMsg("InterstitialAd onPaid")
            isAdShowing.set(false)
        }
        ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                AdMobManager.logMsg("InterstitialAd dismissed: unitId=$unitId")
                isAdShowing.set(false)
                lastDismissStamp = System.currentTimeMillis()
                if (currentActivity.isFinishing || currentActivity.isDestroyed) {
                    return
                }
                thenAction()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                AdMobManager.logMsg("InterstitialAd show failed: unitId=$unitId, code=${adError.code}")
                isAdShowing.set(false)
                if (currentActivity.isFinishing || currentActivity.isDestroyed) {
                    return
                }
                thenAction()
            }
        }
        // Show the ad immediately.
        isAdShowing.set(true)
        ad?.show(currentActivity)
        AdMobManager.logMsg("InterstitialAd show")
    }

    private fun destroyAd(unitId: String) {
        // [START destroy_ad]
        // Stops the preloading and destroy preloaded ads.
        InterstitialAdPreloader.destroy(unitId)
        // Stops the preloading and destroy all ads.
        InterstitialAdPreloader.destroyAll()
        // [END destroy_ad]
    }
}
