package com.rebuild.mixtube.ads

import android.util.Log

class AdPlacementResolver(private val remoteConfig: RemoteConfigStore, private val repository: AdConfigRepository) {
    /**
     * 该APP使用的启动场景列表（直接来自 AdConfig.json 配置文件中注册的key）。
     * 只有 remote_config 中 ad_placement_and=1 时才启用。
     */
    fun startupScenes(): List<String> {
        val enabled = remoteConfig.getString("ad_placement_and", "0") == "1"
        if (!enabled) return emptyList()
        val allScenes = repository.load().placements.keys
        val startupOrder = listOf(
            "mixIVswitchback", "mixIVplay", "mixIVother",
            "mixIVDownload", "mixIV_h",
            "mixIVcleanBoost", "mixIVSetting",
            "mixnative", "NVsearch", "PageBanner", "NormalBanner"
        )
        val ordered = startupOrder.filter { it in allScenes }
        Log.d(TAG, "startupScenes: ad_placement_and=$enabled scenes=$ordered")
        return ordered
    }

    fun sceneForEvent(event: ProductEvent): String {
        val key = when (event) {
            ProductEvent.AppSwitchBack -> "open_cool"
            ProductEvent.PlayStart -> "play_play"
            ProductEvent.PlayPause -> "play_pause"
            ProductEvent.PlayCool -> "play_cool"
            ProductEvent.Download -> "download"
            ProductEvent.OpenPlaylist -> "playlist"
            ProductEvent.TabSwitch -> "tab_switch"
            ProductEvent.Like -> "like"
            ProductEvent.Search -> "search"
            ProductEvent.OtherTab -> "other_tab"
            ProductEvent.OtherLike -> "other_like"
            ProductEvent.OtherSearch -> "other_search"
            ProductEvent.OtherPlaylist -> "other_playlist"
            ProductEvent.OtherDownload -> "other_download"
        }
        val scene = remoteConfig.getString(key, event.defaultScene)
        Log.d(TAG, "sceneForEvent: event=$event key=$key scene=$scene")
        return scene
    }

    fun shouldShowAd(): Boolean {
        return remoteConfig.getString("ad_need_show", "1") == "1"
    }

    fun splashOpenDurationMillis(): Long {
        return remoteConfig.getLong("splash_open_duration", 7000L)
    }

    private companion object {
        const val TAG = "PlacementResolver"
    }
}

enum class ProductEvent(val defaultScene: String) {
    AppSwitchBack(AdScene.SwitchBack.key),
    PlayStart(AdScene.Play.key),
    PlayPause(AdScene.Other.key),
    PlayCool(AdScene.Play.key),
    Download(AdScene.Download.key),
    OpenPlaylist(AdScene.Other.key),
    TabSwitch(AdScene.Home.key),
    Like(AdScene.Other.key),
    Search(AdScene.Other.key),
    OtherTab(AdScene.Other.key),
    OtherLike(AdScene.Other.key),
    OtherSearch(AdScene.Other.key),
    OtherPlaylist(AdScene.Other.key),
    OtherDownload(AdScene.Download.key)
}
