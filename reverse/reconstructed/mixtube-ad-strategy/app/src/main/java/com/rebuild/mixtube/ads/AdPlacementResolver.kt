package com.rebuild.mixtube.ads

import android.util.Log

class AdPlacementResolver(private val remoteConfig: RemoteConfigStore) {
    fun startupScenes(): List<String> {
        return remoteConfig.getString("ad_req_placement_and")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
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
        val duration = remoteConfig.getLong("splash_open_duration", 5000L)
        val forceShortSplash = remoteConfig.getString("full_native", "0") == "1"
        return if (forceShortSplash) 3000L else duration
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
