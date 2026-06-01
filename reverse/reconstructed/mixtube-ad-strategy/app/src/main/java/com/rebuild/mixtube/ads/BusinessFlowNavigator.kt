package com.rebuild.mixtube.ads

import android.app.Activity
import android.content.Intent
import android.util.Log

object BusinessFlowNavigator {
    fun open(activity: Activity, title: String, event: ProductEvent) {
        if (event == ProductEvent.Download || event == ProductEvent.OtherDownload) {
            ServiceLocator.markQueuedDownloads(true)
        }
        val result = ServiceLocator.adManager.showEvent(activity, event)
        Log.d(TAG, "open event=$event -> ${result.message}")
        val businessIntent = buildIntent(activity, title, result.scene, targetFor(event))
        if (result.shown && shouldOpenAfterAd(result.unit)) {
            PostAdNavigationManager.setPending(businessIntent)
            Log.d(TAG, "defer event navigation until ad completes")
        } else {
            PostAdNavigationManager.clear()
            activity.startActivity(businessIntent)
        }
    }

    fun openScene(activity: Activity, scene: String, destination: Intent) {
        val result = ServiceLocator.adManager.showScene(activity, scene)
        Log.d(TAG, "open scene=$scene -> ${result.message}")
        if (result.shown && shouldOpenAfterAd(result.unit)) {
            PostAdNavigationManager.setPending(destination)
            Log.d(TAG, "defer scene navigation until ad completes")
        } else {
            PostAdNavigationManager.clear()
            activity.startActivity(destination)
        }
    }

    fun openEventToDestination(activity: Activity, event: ProductEvent, destination: Intent) {
        openScene(activity, ServiceLocator.placementResolver.sceneForEvent(event), destination)
    }

    private fun buildIntent(
        activity: Activity,
        title: String,
        triggerScene: String,
        target: BusinessTarget
    ): Intent {
        val destination = when (target.pageKind) {
            BusinessPageActivity.PageKind.SEARCH -> SearchPageActivity::class.java
            BusinessPageActivity.PageKind.PLAYER -> PlayerPageActivity::class.java
            BusinessPageActivity.PageKind.PLAYLIST,
            BusinessPageActivity.PageKind.LIKE -> PlaylistDetailActivity::class.java
            BusinessPageActivity.PageKind.DOWNLOAD -> DownloadManageActivity::class.java
            else -> BusinessPageActivity::class.java
        }
        return Intent(activity, destination)
            .putExtra(BusinessPageActivity.EXTRA_TITLE, title)
            .putExtra(BusinessPageActivity.EXTRA_SCENE, triggerScene)
            .putExtra(BusinessPageActivity.EXTRA_PAGE_KIND, target.pageKind.name)
            .putExtra(BusinessPageActivity.EXTRA_INLINE_SCENE, target.inlineScene)
    }

    private fun targetFor(event: ProductEvent): BusinessTarget {
        return when (event) {
            ProductEvent.TabSwitch,
            ProductEvent.OtherTab -> BusinessTarget(
                pageKind = BusinessPageActivity.PageKind.HOME,
                inlineScene = AdScene.NativeHome.key
            )
            ProductEvent.Like,
            ProductEvent.OtherLike -> BusinessTarget(
                pageKind = BusinessPageActivity.PageKind.LIKE,
                inlineScene = AdScene.NativeHome.key
            )
            ProductEvent.Search,
            ProductEvent.OtherSearch -> BusinessTarget(
                pageKind = BusinessPageActivity.PageKind.SEARCH,
                inlineScene = AdScene.NativeSearch.key
            )
            ProductEvent.OpenPlaylist,
            ProductEvent.OtherPlaylist -> BusinessTarget(
                pageKind = BusinessPageActivity.PageKind.PLAYLIST,
                inlineScene = AdScene.PageBanner.key
            )
            ProductEvent.Download,
            ProductEvent.OtherDownload -> BusinessTarget(
                pageKind = BusinessPageActivity.PageKind.DOWNLOAD,
                inlineScene = AdScene.NormalBanner.key
            )
            ProductEvent.PlayStart,
            ProductEvent.PlayPause,
            ProductEvent.PlayCool,
            ProductEvent.AppSwitchBack -> BusinessTarget(
                pageKind = BusinessPageActivity.PageKind.PLAYER,
                inlineScene = AdScene.PageBanner.key
            )
        }
    }

    private fun shouldOpenAfterAd(unit: AdUnitConfig?): Boolean {
        val type = unit?.adtype?.lowercase() ?: return false
        return type == "interstitial" || type == "rewarded" || type == "interrewarded" || type == "open"
    }

    private data class BusinessTarget(
        val pageKind: BusinessPageActivity.PageKind,
        val inlineScene: String
    )

    private const val TAG = "BusinessFlowNav"
}
