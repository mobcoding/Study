package com.rebuild.mixtube.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

object FullNativeAdCache {
    private var cachedEntry: CachedFullNativeAd? = null
    private var loadingScene: String? = null

    fun preload(context: Context, scene: String, unit: AdUnitConfig) {
        if (cachedEntry?.scene == scene) {
            Log.d(TAG, "preload skipped: cached scene=$scene already exists")
            return
        }
        if (loadingScene == scene) {
            Log.d(TAG, "preload skipped: scene=$scene is already loading")
            return
        }

        val testUnitId = RealGoogleMobileAdsAdapter.testPlacementIdForType(unit.adtype)
        loadingScene = scene
        Log.d(
            TAG,
            "preload start: scene=$scene source=${unit.adsource} production=${unit.placementid} test=$testUnitId"
        )
        AdLoader.Builder(context, testUnitId)
            .forNativeAd { ad ->
                cachedEntry?.nativeAd?.destroy()
                cachedEntry = CachedFullNativeAd(
                    scene = scene,
                    unit = unit,
                    nativeAd = ad
                )
                loadingScene = null
                Log.d(TAG, "preload success: scene=$scene")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingScene = null
                    Log.d(TAG, "preload failed: scene=$scene error=${error.message}")
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    fun consume(scene: String): CachedFullNativeAd? {
        val entry = cachedEntry?.takeIf { it.scene == scene } ?: return null
        cachedEntry = null
        Log.d(TAG, "consume cached native: scene=$scene")
        return entry
    }

    fun clear() {
        cachedEntry?.nativeAd?.destroy()
        cachedEntry = null
        loadingScene = null
    }

    data class CachedFullNativeAd(
        val scene: String,
        val unit: AdUnitConfig,
        val nativeAd: NativeAd
    )

    private const val TAG = "FullNativeCache"
}
