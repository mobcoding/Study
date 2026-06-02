package com.rebuild.mixtube.ads

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

class RealGoogleMobileAdsAdapter(private val context: Context) : AdNetworkAdapter {
    override fun supports(source: String): Boolean = source.equals("admob", ignoreCase = true)

    override fun load(scene: String, unit: AdUnitConfig): LoadedAd {
        val testUnit = unit.testPlacementId()
        val cachedUnit = unit.copy(placementid = testUnit)
        Log.d(TAG, "load real AdMob: scene=$scene type=${unit.adtype} production=${unit.placementid} test=$testUnit")
        GoogleFullscreenAdCache.preload(context, scene, cachedUnit)
        return LoadedAd(scene = scene, unit = cachedUnit, payload = null)
    }

    override fun show(activity: Activity, ad: LoadedAd): AdShowResult {
        return when (ad.unit.adtype.lowercase()) {
            "interstitial" -> showInterstitial(activity, ad)
            "rewarded" -> showRewarded(activity, ad)
            "interrewarded" -> showRewardedInterstitial(activity, ad)
            "open" -> showAppOpen(activity, ad)
            "native" -> showNativeActivity(activity, ad)
            "banner" -> showBannerActivity(activity, ad)
            else -> AdShowResult(false, ad.scene, "unsupported AdMob type=${ad.unit.adtype}", ad.unit)
        }
    }

    private fun showInterstitial(activity: Activity, ad: LoadedAd): AdShowResult {
        val cached = GoogleFullscreenAdCache.consume(ad.scene, ad.unit.adtype)?.payload as? InterstitialAd
        if (cached != null) {
            cached.fullScreenContentCallback = loggingCallback(activity, ad)
            cached.show(activity)
            return AdShowResult(true, ad.scene, "show cached AdMob interstitial", ad.unit)
        }
        GoogleFullscreenAdCache.preload(activity.applicationContext, ad.scene, ad.unit)
        return AdShowResult(false, ad.scene, "cache miss AdMob interstitial", ad.unit)
    }

    private fun showRewarded(activity: Activity, ad: LoadedAd): AdShowResult {
        val cached = GoogleFullscreenAdCache.consume(ad.scene, ad.unit.adtype)?.payload as? RewardedAd
        if (cached != null) {
            cached.fullScreenContentCallback = loggingCallback(activity, ad)
            cached.show(activity) { reward -> Log.d(TAG, "cached reward earned: ${reward.amount} ${reward.type}") }
            return AdShowResult(true, ad.scene, "show cached AdMob rewarded", ad.unit)
        }
        GoogleFullscreenAdCache.preload(activity.applicationContext, ad.scene, ad.unit)
        return AdShowResult(false, ad.scene, "cache miss AdMob rewarded", ad.unit)
    }

    private fun showRewardedInterstitial(activity: Activity, ad: LoadedAd): AdShowResult {
        val cached = GoogleFullscreenAdCache.consume(ad.scene, ad.unit.adtype)?.payload as? RewardedInterstitialAd
        if (cached != null) {
            cached.fullScreenContentCallback = loggingCallback(activity, ad)
            cached.show(activity) { reward -> Log.d(TAG, "cached reward interstitial earned: ${reward.amount} ${reward.type}") }
            return AdShowResult(true, ad.scene, "show cached AdMob rewarded interstitial", ad.unit)
        }
        GoogleFullscreenAdCache.preload(activity.applicationContext, ad.scene, ad.unit)
        return AdShowResult(false, ad.scene, "cache miss AdMob rewarded interstitial", ad.unit)
    }

    private fun showAppOpen(activity: Activity, ad: LoadedAd): AdShowResult {
        val cached = GoogleFullscreenAdCache.consume(ad.scene, ad.unit.adtype)?.payload as? AppOpenAd
        if (cached != null) {
            cached.fullScreenContentCallback = loggingCallback(activity, ad)
            cached.show(activity)
            return AdShowResult(true, ad.scene, "show cached AdMob app-open", ad.unit)
        }
        GoogleFullscreenAdCache.preload(activity.applicationContext, ad.scene, ad.unit)
        return AdShowResult(false, ad.scene, "cache miss AdMob app-open", ad.unit)
    }

    private fun showNativeActivity(activity: Activity, ad: LoadedAd): AdShowResult {
        activity.startActivity(Intent(activity, GoogleNativeAdActivity::class.java).putExtra(EXTRA_AD_UNIT, ad.unit.placementid))
        return AdShowResult(true, ad.scene, "opened real AdMob native test activity", ad.unit)
    }

    private fun showBannerActivity(activity: Activity, ad: LoadedAd): AdShowResult {
        activity.startActivity(Intent(activity, GoogleBannerAdActivity::class.java).putExtra(EXTRA_AD_UNIT, ad.unit.placementid))
        return AdShowResult(true, ad.scene, "opened real AdMob banner test activity", ad.unit)
    }

    private fun loggingCallback(activity: Activity, ad: LoadedAd): FullScreenContentCallback {
        return object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Log.d(TAG, "onAdClicked: ${ad.scene}")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "onAdDismissed: ${ad.scene}")
                PostAdNavigationManager.consumeAndStart(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.d(TAG, "onAdFailedToShow: ${error.message}")
                PostAdNavigationManager.consumeAndStart(activity)
            }

            override fun onAdImpression() {
                Log.d(TAG, "onAdImpression: ${ad.scene}")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "onAdShowed: ${ad.scene}")
            }
        }
    }

    private fun AdUnitConfig.testPlacementId(): String {
        return testPlacementIdForType(adtype)
    }

    companion object {
        const val EXTRA_AD_UNIT = "ad_unit"
        private const val TAG = "RealAdMob"
        const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379"
        const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
        private const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"

        fun testPlacementIdForType(adType: String): String {
            return when (adType.lowercase()) {
                "banner" -> TEST_BANNER
                "native" -> TEST_NATIVE
                "open" -> TEST_APP_OPEN
                "rewarded" -> TEST_REWARDED
                "interrewarded" -> TEST_REWARDED_INTERSTITIAL
                else -> TEST_INTERSTITIAL
            }
        }
    }
}
