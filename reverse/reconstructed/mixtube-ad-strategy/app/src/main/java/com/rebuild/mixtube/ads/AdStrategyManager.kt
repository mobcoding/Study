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
        return showScene(activity, scene, trigger = null)
    }

    fun showScene(activity: Activity, scene: String, trigger: FrequencyController.FrequencyTrigger?): AdShowResult {
        if (!resolver.shouldShowAd()) {
            lifecycle.onAdFailed(scene, "ad_need_show disabled")
            return AdShowResult(false, scene, "blocked: ad_need_show disabled")
        }
        if (adWinShowing) {
            lifecycle.onAdFailed(scene, "AdWin self-render ad is showing")
            return AdShowResult(false, scene, "blocked: AdWin self-render ad is showing")
        }

        val config = repository.load()
        val units = repository.unitsFor(scene)
        if (units.isEmpty()) return AdShowResult(false, scene, "no config units for scene")

        val sortedUnits = units.sortedByDescending { it.adweight }
        val effectiveTrigger = trigger ?: frequencyTriggerFromScene(scene, sortedUnits.first())

        val topAd = loadedAds[scene]
        if (topAd != null) {
            if (!frequency.canShow(scene, config, topAd.unit, effectiveTrigger)) {
                lifecycle.onAdFailed(scene, "frequency ${frequency.snapshot()}")
                return AdShowResult(false, scene, "blocked by frequency: ${frequency.snapshot()}")
            }
        } else {
            if (!frequency.canShow(scene, config, sortedUnits.firstOrNull(), effectiveTrigger)) {
                lifecycle.onAdFailed(scene, "frequency ${frequency.snapshot()}")
                return AdShowResult(false, scene, "blocked by frequency: ${frequency.snapshot()}")
            }
        }

        var lastMessage = "no attempt"
        for ((index, unit) in sortedUnits.withIndex()) {
            val adapter = adapterFor(unit)
            val ad = if (loadedAds[scene]?.unit == unit) {
                loadedAds[scene]
            } else {
                adapter.load(scene, unit).also {
                    loadedAds[scene] = it
                    lifecycle.onAdLoaded(it)
                }
            } ?: continue

            val result = adapter.show(activity, ad)
            lastMessage = "try#${index + 1}/${sortedUnits.size} source=${unit.adsource} type=${unit.adtype} => ${result.message}"
            if (result.shown) {
                lifecycle.onAdShowed(ad)
                frequency.recordShow(scene, ad.unit, effectiveTrigger)
                preloadScene(scene)
                return result
            }
            lifecycle.onAdFailed(scene, lastMessage)
        }

        lifecycle.onAdFailed(scene, "all_failed $lastMessage")
        return AdShowResult(false, scene, "all failed: $lastMessage")
    }

    fun showEvent(activity: Activity, event: ProductEvent): AdShowResult {
        val scene = resolver.sceneForEvent(event)
        val trigger = when (event) {
            ProductEvent.AppSwitchBack -> FrequencyController.FrequencyTrigger.SWITCHBACK
            ProductEvent.PlayStart, ProductEvent.PlayCool -> FrequencyController.FrequencyTrigger.PLAY
            ProductEvent.Download, ProductEvent.OtherDownload -> FrequencyController.FrequencyTrigger.DOWNLOAD
            ProductEvent.Search, ProductEvent.OtherSearch -> FrequencyController.FrequencyTrigger.OTHER
            ProductEvent.OpenPlaylist, ProductEvent.OtherPlaylist -> FrequencyController.FrequencyTrigger.OTHER
            ProductEvent.TabSwitch, ProductEvent.OtherTab -> FrequencyController.FrequencyTrigger.OTHER
            ProductEvent.Like, ProductEvent.OtherLike -> FrequencyController.FrequencyTrigger.OTHER
            ProductEvent.PlayPause -> FrequencyController.FrequencyTrigger.OTHER
        }
        return showScene(activity, scene, trigger)
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
        val decision = ServiceLocator.adWinRepository.canShow(activity, remoteConfig)
        if (!decision.allowed) {
            Tracking.log(activity, "adwin_blocked", mapOf("reason" to decision.reason, "source" to "manager"))
            return
        }
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

    private fun frequencyTriggerFromScene(scene: String, unit: AdUnitConfig): FrequencyController.FrequencyTrigger {
        return when {
            unit.adtype.equals("open", ignoreCase = true) -> FrequencyController.FrequencyTrigger.SWITCHBACK
            scene.equals(AdScene.Download.key, ignoreCase = true) -> FrequencyController.FrequencyTrigger.DOWNLOAD
            scene.equals(AdScene.Play.key, ignoreCase = true) -> FrequencyController.FrequencyTrigger.PLAY
            scene.equals(AdScene.NativeHome.key, ignoreCase = true) || scene.equals(AdScene.NativeSearch.key, ignoreCase = true) ->
                FrequencyController.FrequencyTrigger.NATIVE
            scene.contains("Banner", ignoreCase = true) || unit.adtype.contains("banner", ignoreCase = true) ->
                FrequencyController.FrequencyTrigger.BANNER
            else -> FrequencyController.FrequencyTrigger.OTHER
        }
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
