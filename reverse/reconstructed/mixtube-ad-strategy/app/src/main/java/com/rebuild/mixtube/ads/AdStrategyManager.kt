package com.rebuild.mixtube.ads

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log

class AdStrategyManager(
    private val context: Context,
    private val remoteConfig: RemoteConfigStore,
    private val repository: AdConfigRepository,
    private val frequency: FrequencyController,
    private val resolver: AdPlacementResolver,
    private val selector: WeightedAdSelector,
    private val lifecycle: AdLifecycleListener,
    private val adapters: List<AdNetworkAdapter>
) {
    private val loadedAds = mutableMapOf<String, LoadedAd>()
    private var adWinShowing = false

    fun bootstrapFromRemoteConfig(): String {
        remoteConfig.fetchAndActivate()
        val splashDuration = resolver.splashOpenDurationMillis()
        val scenes = resolver.startupScenes()
        scenes.forEach(::preloadScene)
        preloadStartupPresentation()
        return "RemoteConfig splash_open_duration=$splashDuration, ad_req_placement_and=$scenes"
    }

    fun startupDurationMillis(): Long {
        return resolver.splashOpenDurationMillis()
    }

    fun startupScenes(): List<String> {
        return resolver.startupScenes()
    }

    fun preloadScene(scene: String) {
        val units = repository.unitsFor(scene)
        if (units.isEmpty()) {
            Log.d(TAG, "preload skipped: scene=$scene has no configured units")
            return
        }
        val selected = selector.select(scene, units)
        val adapter = adapterFor(selected)
        loadedAds[scene] = adapter.load(scene, selected).also(lifecycle::onAdLoaded)
    }

    fun preloadStartupPresentation() {
        val presentation = resolveStartupPresentation() ?: return
        when (presentation.mode) {
            StartupPresentationMode.FULL_NATIVE -> {
                presentation.unit?.let { FullNativeAdCache.preload(context, presentation.scene, it) }
            }
            StartupPresentationMode.FULLSCREEN_SCENE -> preloadScene(presentation.scene)
        }
    }

    fun hasStartupPresentation(): Boolean = resolveStartupPresentation() != null

    fun presentStartupAd(activity: Activity): Boolean {
        val presentation = resolveStartupPresentation() ?: return false
        Log.d(
            TAG,
            "present startup: mode=${presentation.mode} scene=${presentation.scene} unit=${presentation.unit}"
        )
        return when (presentation.mode) {
            StartupPresentationMode.FULL_NATIVE -> {
                presentation.unit?.let { FullNativeAdCache.preload(context, presentation.scene, it) }
                showFullNative(activity, presentation.scene)
                true
            }
            StartupPresentationMode.FULLSCREEN_SCENE -> showScene(activity, presentation.scene).shown
        }
    }

    fun showScene(activity: Activity, scene: String): AdShowResult {
        if (!resolver.shouldShowAd()) {
            lifecycle.onAdFailed(scene, "ad_need_show disabled")
            return AdShowResult(false, scene, "blocked: ad_need_show disabled")
        }
        if (adWinShowing) {
            lifecycle.onAdFailed(scene, "AdWin self-render ad is showing")
            return AdShowResult(false, scene, "blocked: AdWin self-render ad is showing")
        }

        val config = repository.load()
        if (!frequency.canShow(scene, config)) {
            lifecycle.onAdFailed(scene, "frequency ${frequency.snapshot()}")
            return AdShowResult(false, scene, "blocked by frequency: ${frequency.snapshot()}")
        }

        val ad = loadedAds[scene] ?: run {
            preloadScene(scene)
            loadedAds[scene]
        } ?: return AdShowResult(false, scene, "no cached ad and no fallback config")

        val result = adapterFor(ad.unit).show(activity, ad)
        if (result.shown) {
            lifecycle.onAdShowed(ad)
            frequency.recordShow(scene)
            preloadScene(scene)
        }
        return result
    }

    fun showEvent(activity: Activity, event: ProductEvent): AdShowResult {
        return showScene(activity, resolver.sceneForEvent(event))
    }

    fun showTopOnSplash(activity: Activity, placementId: String) {
        activity.startActivity(
            Intent(activity, ToponSplashAdShowActivity::class.java)
                .putExtra(ToponSplashAdShowActivity.EXTRA_PLACEMENT_ID, placementId)
        )
    }

    fun showFullNative(activity: Activity, scene: String) {
        val type = remoteConfig.getString("adType", "full_native")
        activity.startActivity(
            Intent(activity, FullNativeAdActivity::class.java)
                .putExtra(FullNativeAdActivity.EXTRA_SCENE, scene)
                .putExtra(FullNativeAdActivity.EXTRA_AD_TYPE, type)
        )
    }

    fun showAdWin(activity: Activity) {
        activity.startActivity(Intent(activity, AdWinInterActivity::class.java))
    }

    fun setAdWinShowing(showing: Boolean) {
        adWinShowing = showing
        Log.d(TAG, "AdWin showing flag = $showing")
    }

    fun clearFrequency() = frequency.clear()

    fun configuredScenes(): Set<String> = repository.load().placements.keys

    fun selectedUnitForScene(scene: String): AdUnitConfig? {
        val units = repository.unitsFor(scene)
        if (units.isEmpty()) return null
        return loadedAds[scene]?.unit ?: selector.select(scene, units)
    }

    private fun resolveStartupPresentation(): StartupPresentation? {
        val scenes = startupScenes()
        if (scenes.isEmpty()) return null

        val fullNativeEnabled = remoteConfig.getString("full_native", "0") == "1" ||
            remoteConfig.getString("adType", "full_native").equals("full_native", ignoreCase = true)
        if (fullNativeEnabled) {
            val nativeScene = scenes.firstOrNull { scene ->
                repository.unitsFor(scene).any { it.adtype.equals("native", ignoreCase = true) }
            } ?: AdScene.NativeHome.key.takeIf { repository.unitsFor(it).isNotEmpty() }
            if (nativeScene != null) {
                return StartupPresentation(
                    scene = nativeScene,
                    unit = selectedUnitForScene(nativeScene),
                    mode = StartupPresentationMode.FULL_NATIVE
                )
            }
        }

        val fullscreenScene = scenes.firstOrNull { scene ->
            repository.unitsFor(scene).any {
                val type = it.adtype.lowercase()
                type == "open" || type == "interstitial" || type == "rewarded" || type == "interrewarded"
            }
        } ?: return null
        return StartupPresentation(
            scene = fullscreenScene,
            unit = selectedUnitForScene(fullscreenScene),
            mode = StartupPresentationMode.FULLSCREEN_SCENE
        )
    }

    private fun adapterFor(unit: AdUnitConfig): AdNetworkAdapter {
        return adapters.firstOrNull { it.supports(unit.adsource) }
            ?: MockAdNetworkAdapter(unit.adsource)
    }

    private data class StartupPresentation(
        val scene: String,
        val unit: AdUnitConfig?,
        val mode: StartupPresentationMode
    )

    private enum class StartupPresentationMode {
        FULL_NATIVE,
        FULLSCREEN_SCENE
    }

    private companion object {
        const val TAG = "AdStrategy"
    }
}
