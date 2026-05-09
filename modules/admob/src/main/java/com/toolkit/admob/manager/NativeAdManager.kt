package com.toolkit.admob.manager

import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.toolkit.admob.view.AdContainer


/**
 * @author Admin
 */
class NativeAdManager(private val mContext: Context, private val mUnitId: String, private val mRootView: ViewGroup, private val mLayoutRes: Int) {
    private var mIsUserVisible = false

    private var mIsAdLoading = false

    private var mAdContainer: AdContainer? = null

    private var mNativeAd: NativeAd? = null

    private var mAdShowTime: Long = 0

    private var mAdLoadTime: Long = 0

    fun onUserVisible() {
        mIsUserVisible = true
        loadNativeAd()
    }

    fun onUserInvisible() {
        mIsUserVisible = false
        tryRemoveAd(true)
    }

    fun onDestroy() {
        if (mNativeAd != null) {
            mNativeAd?.destroy()
            mNativeAd = null
        }
        mRootView.removeAllViews()
        mAdContainer = null
        mIsAdLoading = false
    }


    fun tryRemoveAd(checkCooling: Boolean) {
        if (System.currentTimeMillis() - mAdShowTime < 15 * 1000 && checkCooling) {
            AdMobManager.logMsg("tryRemoveAd : Ad is kept")
            return
        }
        mRootView.removeAllViews()
        AdMobManager.logMsg("NativeAd Removed")
        if (mNativeAd != null) {
            mNativeAd?.destroy()
        }
        mAdContainer = null
        mIsAdLoading = false
    }

    private fun loadNativeAd() {
        if (AdMobManager.atomicBoolean.get()) {
            doLoadNativeAd()
        }
    }

    private fun doLoadNativeAd() {
        val now = System.currentTimeMillis()
        // 广告加载中
        if (mIsAdLoading) {
            AdMobManager.logMsg("NativeAd is loading")
            return
        }

        if (now - mAdLoadTime < 5 * 1000) {
            AdMobManager.logMsg("NativeAd load too frequently")
            return
        }

        if (mRootView.childCount > 0) {
            AdMobManager.logMsg(" NativeAd is exist")
            return
        }
        AdMobManager.logMsg("load NativeAd")
        mIsAdLoading = true
        mAdContainer = AdContainer(mContext, mLayoutRes)
        mAdLoadTime = now
        val adLoader = AdLoader.Builder(mContext, mUnitId).forNativeAd { nativeAd: NativeAd ->
            AdMobManager.logMsg("NativeAd is loaded")
//            Enhancer.enhance(mUnitId, nativeAd);
            // 修正加载中标识
            mIsAdLoading = false
            if (!mIsUserVisible || mAdContainer == null) {
                // 销毁新加载的广告
                nativeAd.destroy()
                mRootView.removeAllViews()
                mAdContainer = null
                return@forNativeAd
            }

            // You must call destroy on old ads when you are done with them,
            // otherwise you will have a memory leak.
            if (mNativeAd != null) {
                // 销毁旧广告
                mNativeAd?.destroy()
            }
            mNativeAd = nativeAd
            // 展示新广告
            mAdContainer?.setNativeAd(mNativeAd!!)
            mAdShowTime = System.currentTimeMillis()
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                AdMobManager.logMsg("NativeAd is loading failure : ${adError.message}")
                mIsAdLoading = false
                mRootView.removeAllViews()
                mAdContainer = null
            }

            override fun onAdClicked() {
                super.onAdClicked()
                AdMobManager.appLifecycle?.disableOpenAd(60 * 1000)
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().setVideoOptions(VideoOptions.Builder().setStartMuted(true).build()).build()).build()
        adLoader.loadAd(AdRequest.Builder().build())
        mRootView.addView(mAdContainer)
    }
}
