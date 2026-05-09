package com.toolkit.admob.manager

import android.app.Activity
import android.content.Context
import android.os.CountDownTimer
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.toolkit.admob.listener.OpenAdStatusListener
import com.toolkit.admob_libray.BuildConfig
import java.util.Date

/**
 * @date:2024/7/10 11:32
 * @path:com.toolkit.admob.manager.AppOpenAdManager
 */
object AppOpenAdManager {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isAdLoadFailed = false
    var isAdShowing: Boolean = false

    /**
     * Keep track of the time an app open ad is loaded to ensure you don't show an expired ad.
     */
    private var loadTime: Long = 0

    /**
     * 最后一次开屏展示时间戳(要求开屏展示事件和插页广告展示时间间隔15秒)
     */
    var openAdLastShowTime: Long = 0

    fun loadAd(context: Context) {
        if (BuildConfig.DEBUG) {
            return
        }
        if (AdMobManager.atomicBoolean.get()) {
            doLoadAd(context)
        }
    }

    private fun doLoadAd(context: Context) {
        if (isLoadingAd || isAdAvailable) {
            AdMobManager.logMsg("open ad isLoading or isAdAvailable return")
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(context, BuildConfig.ADMOB_OPEN, request, object : AppOpenAd.AppOpenAdLoadCallback() {
            /**
             * Called when an app open ad has loaded.
             *
             * @param ad the loaded app open ad.
             */
            override fun onAdLoaded(ad: AppOpenAd) {
//                Enhancer.enhance(BuildConfig.ADMOB_OPEN, ad);
                appOpenAd = ad
                isLoadingAd = false
                isAdLoadFailed = false
                loadTime = Date().time
                AdMobManager.logMsg("open ad load complete")

            }

            /**
             * Called when an app open ad has failed to load.
             *
             * @param loadAdError the error.
             */
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isLoadingAd = false
                isAdLoadFailed = true
                AdMobManager.logMsg("open ad preload onAdFailedToLoad")
            }
        })
    }

    /**
     * Check if ad was loaded more than n hours ago.
     */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - this.loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return (dateDifference < (numMilliSecondsPerHour * numHours))
    }

    private val isAdAvailable: Boolean
        /**
         * Check if ad exists and can be shown.
         */
        get() = appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)


    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity         the activity that shows the app open ad
     * @param adStatusListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, adStatusListener: OpenAdStatusListener) {
        // If the app open ad is already showing, do not show the ad again.
        if (isAdShowing) {
            AdMobManager.logMsg("open ad is showing")
            return
        }

        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isAdAvailable) {
            adStatusListener.onNotReady(isAdLoadFailed)
            AdMobManager.logMsg("open ad is no ready,load failed:$isAdLoadFailed")
            loadAd(activity)
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            /** Called when full screen content is dismissed.  */
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isAdShowing = false
                openAdLastShowTime = System.currentTimeMillis()
                adStatusListener.onComplete()
                loadAd(activity)
            }

            /** Called when fullscreen content failed to show.  */
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isAdShowing = false
                adStatusListener.onComplete()
                loadAd(activity)
            }

            /** Called when fullscreen content is shown.  */
            override fun onAdShowedFullScreenContent() {
            }
        }

        isAdShowing = true
        appOpenAd?.show(activity)
    }


    fun tryWaitingAndLoadAd(activity: Activity, maxSeconds: Int, listener: OpenAdStatusListener) {
        val countDownTimer: CountDownTimer = object : CountDownTimer((maxSeconds * 1000).toLong(), 1000) {
            override fun onTick(secondsFinished: Long) {
                val progress = maxSeconds - (secondsFinished.toInt() / 1000)
                AdMobManager.logMsg("onTick:$progress")
                loadAd(activity)
                if (!isLoadingAd || isAdShowing) {
                    AdMobManager.logMsg("open ad isAdAvailable,try show")
                    cancel()
                    onFinish()
                }
            }

            override fun onFinish() {
                AdMobManager.logMsg("onTick finish")
                showAdIfAvailable(activity, object : OpenAdStatusListener {
                    override fun onNotReady(loadFailed: Boolean) {
                        listener.onComplete()
                        cancel()
                    }

                    override fun onComplete() {
                        listener.onComplete()
                        cancel()
                    }
                })
            }
        }
        countDownTimer.start()
    }
}