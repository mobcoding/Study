package com.rebuild.mixtube.ads

import android.util.Log

interface AdLifecycleListener {
    fun onAdLoaded(ad: LoadedAd)
    fun onAdShowed(ad: LoadedAd)
    fun onAdClicked(ad: LoadedAd)
    fun onAdDismissed(ad: LoadedAd)
    fun onAdFailed(scene: String, reason: String)
}

class LoggingAdLifecycleListener : AdLifecycleListener {
    override fun onAdLoaded(ad: LoadedAd) {
        Log.d(TAG, "onAdLoaded: ${ad.describe()}")
    }

    override fun onAdShowed(ad: LoadedAd) {
        Log.d(TAG, "onAdShowed: ${ad.describe()}")
    }

    override fun onAdClicked(ad: LoadedAd) {
        Log.d(TAG, "onAdClicked: ${ad.describe()}")
    }

    override fun onAdDismissed(ad: LoadedAd) {
        Log.d(TAG, "onAdDismissed: ${ad.describe()}")
    }

    override fun onAdFailed(scene: String, reason: String) {
        Log.d(TAG, "onAdFailed: scene=$scene reason=$reason")
    }

    private fun LoadedAd.describe(): String {
        return "scene=$scene source=${unit.adsource} type=${unit.adtype} placement=${unit.placementid}"
    }

    private companion object {
        const val TAG = "AdLifecycle"
    }
}
