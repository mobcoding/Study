package com.rebuild.mixtube.ads

import android.app.Activity
import android.content.Intent
import android.util.Log

interface AdNetworkAdapter {
    fun supports(source: String): Boolean
    fun load(scene: String, unit: AdUnitConfig): LoadedAd
    fun show(activity: Activity, ad: LoadedAd): AdShowResult
}

class MockAdNetworkAdapter(private val sourceName: String) : AdNetworkAdapter {
    override fun supports(source: String): Boolean = source.equals(sourceName, ignoreCase = true)

    override fun load(scene: String, unit: AdUnitConfig): LoadedAd {
        Log.d(TAG, "load: scene=$scene source=${unit.adsource} type=${unit.adtype} placement=${unit.placementid}")
        return LoadedAd(scene = scene, unit = unit)
    }

    override fun show(activity: Activity, ad: LoadedAd): AdShowResult {
        val message = "show: scene=${ad.scene} source=${ad.unit.adsource} type=${ad.unit.adtype} placement=${ad.unit.placementid}"
        Log.d(TAG, message)
        if (sourceName.equals("topon", ignoreCase = true) && ad.unit.adtype.equals("open", ignoreCase = true)) {
            activity.startActivity(
                Intent(activity, ToponSplashAdShowActivity::class.java)
                    .putExtra(ToponSplashAdShowActivity.EXTRA_PLACEMENT_ID, ad.unit.placementid)
                    .putExtra(ToponSplashAdShowActivity.EXTRA_SCENE, ad.scene)
                    .putExtra(ToponSplashAdShowActivity.EXTRA_SOURCE, ad.unit.adsource)
                    .putExtra(ToponSplashAdShowActivity.EXTRA_TYPE, ad.unit.adtype)
            )
            return AdShowResult(true, ad.scene, "$message opened TopOn splash rebuild", ad.unit)
        }
        activity.startActivity(
            Intent(activity, ReconstructedMediationAdActivity::class.java)
                .putExtra(ReconstructedMediationAdActivity.EXTRA_SCENE, ad.scene)
                .putExtra(ReconstructedMediationAdActivity.EXTRA_SOURCE, ad.unit.adsource)
                .putExtra(ReconstructedMediationAdActivity.EXTRA_TYPE, ad.unit.adtype)
                .putExtra(ReconstructedMediationAdActivity.EXTRA_PLACEMENT, ad.unit.placementid)
                .putExtra(
                    ReconstructedMediationAdActivity.EXTRA_CLOSE_DELAY,
                    if (ad.unit.adtype.equals("open", ignoreCase = true)) 2 else 3
                )
        )
        return AdShowResult(true, ad.scene, "$message opened reconstructed mediation page", ad.unit)
    }

    private companion object {
        const val TAG = "AdAdapter"
    }
}
