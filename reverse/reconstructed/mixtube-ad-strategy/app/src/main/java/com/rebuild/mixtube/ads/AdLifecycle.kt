package com.rebuild.mixtube.ads

import android.content.Context
import android.util.Log

interface AdLifecycleListener {
    fun onAdLoaded(ad: LoadedAd)
    fun onAdShowed(ad: LoadedAd)
    fun onAdClicked(ad: LoadedAd)
    fun onAdDismissed(ad: LoadedAd)
    fun onAdFailed(scene: String, reason: String)
}

class LoggingAdLifecycleListener(private val context: Context) : AdLifecycleListener {
    override fun onAdLoaded(ad: LoadedAd) {
        Log.d(TAG, "onAdLoaded: ${ad.describe()}")
        Tracking.log(context, "ad_loaded", ad.toProps())
    }

    override fun onAdShowed(ad: LoadedAd) {
        Log.d(TAG, "onAdShowed: ${ad.describe()}")
        Tracking.log(context, "ad_showed", ad.toProps())
    }

    override fun onAdClicked(ad: LoadedAd) {
        Log.d(TAG, "onAdClicked: ${ad.describe()}")
        Tracking.log(context, "ad_clicked", ad.toProps())
    }

    override fun onAdDismissed(ad: LoadedAd) {
        Log.d(TAG, "onAdDismissed: ${ad.describe()}")
        Tracking.log(context, "ad_dismissed", ad.toProps())
    }

    override fun onAdFailed(scene: String, reason: String) {
        Log.d(TAG, "onAdFailed: scene=$scene reason=$reason")
        Tracking.log(context, "ad_failed", mapOf("scene" to scene, "reason" to reason))
    }

    private fun LoadedAd.describe(): String {
        return "scene=$scene source=${unit.adsource} type=${unit.adtype} placement=${unit.placementid}"
    }

    private fun LoadedAd.toProps(): Map<String, Any?> {
        return mapOf(
            "scene" to scene,
            "adsource" to unit.adsource,
            "adtype" to unit.adtype,
            "placementid" to unit.placementid
        )
    }

    private companion object {
        const val TAG = "AdLifecycle"
    }
}
