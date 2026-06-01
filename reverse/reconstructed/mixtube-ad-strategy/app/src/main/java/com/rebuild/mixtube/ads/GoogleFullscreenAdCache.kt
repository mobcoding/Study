package com.rebuild.mixtube.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

object GoogleFullscreenAdCache {
    private val cachedAds = mutableMapOf<String, CachedFullscreenAd>()
    private val loadingKeys = mutableSetOf<String>()

    fun preload(context: Context, scene: String, unit: AdUnitConfig) {
        val type = unit.adtype.lowercase()
        if (type !in supportedTypes) return
        val key = cacheKey(scene, type)
        if (cachedAds.containsKey(key)) {
            Log.d(TAG, "preload skipped: cached key=$key already exists")
            return
        }
        if (!loadingKeys.add(key)) {
            Log.d(TAG, "preload skipped: key=$key is already loading")
            return
        }
        Log.d(TAG, "preload start: scene=$scene type=$type unit=${unit.placementid}")
        when (type) {
            "interstitial" -> InterstitialAd.load(
                context,
                unit.placementid,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        cachedAds[key] = CachedFullscreenAd(scene, unit, interstitialAd)
                        loadingKeys.remove(key)
                        Log.d(TAG, "preload success: key=$key")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingKeys.remove(key)
                        Log.d(TAG, "preload failed: key=$key error=${error.message}")
                    }
                }
            )
            "rewarded" -> RewardedAd.load(
                context,
                unit.placementid,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        cachedAds[key] = CachedFullscreenAd(scene, unit, rewardedAd)
                        loadingKeys.remove(key)
                        Log.d(TAG, "preload success: key=$key")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingKeys.remove(key)
                        Log.d(TAG, "preload failed: key=$key error=${error.message}")
                    }
                }
            )
            "interrewarded" -> RewardedInterstitialAd.load(
                context,
                unit.placementid,
                AdRequest.Builder().build(),
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(rewardedInterstitialAd: RewardedInterstitialAd) {
                        cachedAds[key] = CachedFullscreenAd(scene, unit, rewardedInterstitialAd)
                        loadingKeys.remove(key)
                        Log.d(TAG, "preload success: key=$key")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingKeys.remove(key)
                        Log.d(TAG, "preload failed: key=$key error=${error.message}")
                    }
                }
            )
            "open" -> AppOpenAd.load(
                context,
                unit.placementid,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(appOpenAd: AppOpenAd) {
                        cachedAds[key] = CachedFullscreenAd(scene, unit, appOpenAd)
                        loadingKeys.remove(key)
                        Log.d(TAG, "preload success: key=$key")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingKeys.remove(key)
                        Log.d(TAG, "preload failed: key=$key error=${error.message}")
                    }
                }
            )
        }
    }

    fun consume(scene: String, adType: String): CachedFullscreenAd? {
        val key = cacheKey(scene, adType.lowercase())
        val entry = cachedAds.remove(key)
        if (entry != null) {
            Log.d(TAG, "consume cached fullscreen ad: key=$key")
        }
        return entry
    }

    private fun cacheKey(scene: String, type: String): String = "$scene#$type"

    data class CachedFullscreenAd(
        val scene: String,
        val unit: AdUnitConfig,
        val payload: Any
    )

    private val supportedTypes = setOf("interstitial", "rewarded", "interrewarded", "open")

    private const val TAG = "GoogleFsAdCache"
}
