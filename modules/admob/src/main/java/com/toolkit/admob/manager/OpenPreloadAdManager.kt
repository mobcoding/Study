package com.toolkit.admob.manager


import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.appopen.AppOpenAdPreloader
import com.google.android.gms.ads.preload.PreloadCallbackV2
import com.google.android.gms.ads.preload.PreloadConfiguration
import com.toolkit.admob_libray.BuildConfig
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

object OpenAdMobManager {
    val isLoadingAd = AtomicBoolean(false)
    val isAdShowing = AtomicBoolean(false)
    val loadFailed = AtomicBoolean(false)
    var openAdLastShowTime: Long = 0L
    const val OPEN_AD_ID: String = BuildConfig.ADMOB_OPEN
    fun tryLoad() {
        AdMobManager.logMsg("OpenAd tryLoad...$OPEN_AD_ID")
        if (isLoadingAd.get() || isAvailable()) {
            AdMobManager.logMsg(
                "OpenAd loadAd skip: loading=${isLoadingAd.get()}, available=${isAvailable()}")
            return
        }

        isLoadingAd.set(true)
        loadFailed.set(false)
        val callback = object : PreloadCallbackV2() {
            override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo?) {
                AdMobManager.logMsg("OpenAd onAdLoaded()")
                isLoadingAd.set(false)
                loadFailed.set(false)
            }

            override fun onAdsExhausted(preloadId: String) {
                AdMobManager.logMsg("OpenAd onAdsExhausted(): $preloadId")
            }

            override fun onAdFailedToPreload(preloadId: String, adError: AdError) {
                AdMobManager.logMsg("OpenAd onAdFailedToPreload(): $preloadId->${adError.message}")
                isLoadingAd.set(false)
                loadFailed.set(true)
            }
        }
        val configuration = PreloadConfiguration.Builder(OPEN_AD_ID).setBufferSize(1).build()
        AppOpenAdPreloader.start(OPEN_AD_ID, configuration, callback)
    }

    /**
     * 展示开屏广告（基础版：直接展示可用广告）
     */
    fun showAdIfAvailable(activity: Activity, andThen: (Boolean) -> Unit) {
        AdMobManager.logMsg("OpenAd showAdIfAvailable...")
        if (isAdShowing.get() || !isAvailable()) {
            AdMobManager.logMsg(
                "OpenAd showAdIfAvailable: showing=${isAdShowing.get()}, available=${isAvailable()}")
            andThen(false)
            tryLoad()
            return
        }

        val activityRef = WeakReference(activity)
        val appOpenAd = AppOpenAdPreloader.pollAd(OPEN_AD_ID)

        appOpenAd?.onPaidEventListener = OnPaidEventListener {
            AdMobManager.logMsg("OpenAd onPaid")
        }
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                AdMobManager.logMsg("OpenAd closed")
                isAdShowing.set(false)
                openAdLastShowTime = System.currentTimeMillis()
//                activityRef.get()?.let { if (!it.isFinishing) tryLoad() }
                andThen(true)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdMobManager.logMsg("OpenAd failed to show:${adError.code}")
                isAdShowing.set(false)
                andThen(true)
//                activityRef.get()?.let { if (!it.isFinishing) tryLoad() }
            }

            override fun onAdShowedFullScreenContent() {
                AdMobManager.logMsg("OpenAd showing...")
            }
        }
        isAdShowing.set(true)
        activityRef.get()?.let { if (!it.isFinishing) appOpenAd?.show(it) }
    }


    fun isAvailable(): Boolean {
        val isAdAvailable = AppOpenAdPreloader.isAdAvailable(OPEN_AD_ID)
        AdMobManager.logMsg("OpenAd is Available?->$isAdAvailable")
        return isAdAvailable
    }

    /**
     * 释放资源（应用退后台时调用）
     */
    fun release() {
        isLoadingAd.set(false)
        isAdShowing.set(false)
        AdMobManager.logMsg("资源已释放")
    }
}


