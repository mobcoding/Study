package com.toolkit.admob.manager

import android.app.Activity
import android.content.Context
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.toolkit.admob.listener.InterstitialAdListener
import com.toolkit.admob_libray.BuildConfig
import java.util.Locale
import kotlin.random.Random

/**
 * @author Admin
 */
object InterstitialAdManager {
    private var lastDismissStamp: Long = 0

    private val READY_ADS = HashMap<String, InterstitialAdWrapper>()
    private val LOADING_ADS = ArrayList<String>()
    private var COOLING_INTERVAL = if (BuildConfig.DEBUG) 3000L else 15 * 1000L
    private val INTER_ADS = arrayOf(BuildConfig.ADMOB_INTERSTITIAL_GUIDE, BuildConfig.ADMOB_INTERSTITIAL_LANGUAGE, BuildConfig.ADMOB_INTERSTITIAL_CONNECT_RESULT)
    var isAdShowing: Boolean = false
    fun loadAd(context: Context, unitId: String) {
        if (AdMobManager.atomicBoolean.get()) {
            doLoadInterstitialAd(context, unitId)
        }
    }

    fun loadSyncAd(context: Context, unitId: String, maxSeconds: Int, listener: InterstitialAdListener) {
        if (AdMobManager.atomicBoolean.get()) {
            val countDownTimer: CountDownTimer = object : CountDownTimer((maxSeconds * 1000).toLong(), 1000) {
                override fun onTick(secondsFinished: Long) {
                    val progress = maxSeconds - (secondsFinished.toInt() / 1000)
                    AdMobManager.logMsg("onTick:$progress")
                    doLoadInterstitialAd(context, unitId)
                    if (READY_ADS.containsKey(unitId)) {
                        AdMobManager.logMsg("InterstitialAd isLoaded")
                        cancel()
                        onFinish()
                    }
                }

                override fun onFinish() {
                    listener.callback(true)
                }
            }
            countDownTimer.start()

        }
    }

    /**
     * 加载插屏广告
     */
    private fun doLoadInterstitialAd(context: Context, unitId: String) {
        if (isLoading(unitId)) {
            AdMobManager.logMsg("InterstitialAd is loading return")
            return
        }
        if (isReady(unitId) && isAdValid(unitId)) {
            AdMobManager.logMsg("InterstitialAd is isReady but within the valid  period return")
            return
        }
        READY_ADS.remove(unitId)
        LOADING_ADS.add(unitId)
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, unitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                AdMobManager.logMsg("InterstitialAd onAdLoaded:${unitId}")
//                Enhancer.enhance(unitId, interstitialAd)
                LOADING_ADS.remove(unitId)
                if (AdMobManager.connected) {
                    //连接状态：如存在两个VPN状态的广告对象，则不加载广告，否则加载广告，使得VPN的广告对象变成两个
                    if (getAdvancedAdNum() < 2) {
                        READY_ADS[unitId] = InterstitialAdWrapper(unitId, true, interstitialAd, SystemClock.elapsedRealtime())
                        Log.e("vpp", "VPN已连接,不足两个VPN广告，加载广告使得VPN的广告对象变成两个,当前VPN广告数量：" + getAdvancedAdNum())
                    } else {
                        Log.e("vpp", "VPN已连接 ,存在两个VPN状态的广告对象，不加载广告")
                    }
                } else {
                    //未连接状态：如存在非VPN状态的广告对象，则不加载广告
                    //否则随机取一个未加载的广告id，加载广告
                    if (getNormalAdNum() < 1) {
                        READY_ADS[unitId] = InterstitialAdWrapper(unitId, false, interstitialAd, SystemClock.elapsedRealtime())
                        Log.e("vpp", "VPN没有连接 不存在非VPN状态的广告对象随机取一个未加载的广告id，加载广告，当前非VPN广告数量：" + getNormalAdNum())
                    } else {
                        Log.e("vpp", "VPN没有连接 已存在非VPN状态广告，不加载，当前非VPN广告数量：" + getNormalAdNum())
                    }
                }
                Log.e("vpp", "onAdLoaded: 当前VPN/非VPN广告数量：" + getAdvancedAdNum() + "|" + getNormalAdNum())
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val error = String.format(Locale.getDefault(), "domain: %s, code: %d, message: %s", loadAdError.domain, loadAdError.code, loadAdError.message)
                AdMobManager.logMsg("InterstitialAd onAdFailedToLoad:$error")
                LOADING_ADS.remove(unitId)
            }
        })
        AdMobManager.logMsg("InterstitialAd is Loading ....:${unitId}")
    }


    fun tryShow(activity: Activity, unitId: String, listener: InterstitialAdListener, checkCooling: Boolean) {
        val now = System.currentTimeMillis()
        if (activity.isDestroyed || isAdShowing) {
            return
        }
        if (checkCooling && now - lastDismissStamp < COOLING_INTERVAL) {
            AdMobManager.logMsg("Interstitial Ad is cooling  ...:${unitId}")
            listener.callback(false)
            return
        }
        if (!isReady(unitId)) {
            AdMobManager.logMsg("Interstitial Ad is not ready:${unitId}")
            listener.callback(false)
            return
        }
        if (!isAdValid(unitId)) {
            AdMobManager.logMsg("Interstitial Ad is not with in valid period:${unitId}")
            READY_ADS.remove(unitId)
            listener.callback(false)
            return
        }
        var ad = READY_ADS[unitId]
        if (getAdvancedAdNum() > 0) {
            //如果有VPN 广告优先展示VPN广告，否则展示普通广告
            ad = getAdvanceAd()
        }
        READY_ADS.remove(unitId)
        if (ad != null) {
            ad.interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    isAdShowing = false
                    listener.callback(true)
                    lastDismissStamp = System.currentTimeMillis()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    isAdShowing = false
                    if (activity.isFinishing || activity.isDestroyed) {
                        return
                    }
                    listener.callback(false)
                }

            }
            isAdShowing = true
            ad.interstitialAd.show(activity)
            AdMobManager.logMsg("Interstitial Ad is show and is advance:" + ad.advance)
        }
    }

    private fun isReady(unitId: String): Boolean {
        return READY_ADS[unitId] != null
    }

    private fun isLoading(unitId: String): Boolean {
        return LOADING_ADS.contains(unitId)
    }

    private fun isAdValid(unitId: String): Boolean {
        val ad = READY_ADS.getOrDefault(unitId, null) ?: return false
        val validTimeMill = if (BuildConfig.DEBUG) 30000 else (3600 * 1000)
        return SystemClock.elapsedRealtime() - ad.loadedStamp < validTimeMill
    }

    /**
     * return advanced ad number
     */
    private fun getAdvancedAdNum(): Int {
        return READY_ADS.count { it.value.advance }
    }

    /**
     *
     */
    private fun getAdvanceAd(): InterstitialAdWrapper? {
        val keys = READY_ADS.keys.toList()
        val randomKey = keys[Random.nextInt(keys.size)]
        return READY_ADS[randomKey]
    }

    /**
     * return normal ad number
     */
    private fun getNormalAdNum(): Int {
        return READY_ADS.count { !it.value.advance }
    }

    class InterstitialAdWrapper(val unitId: String, val advance: Boolean, val interstitialAd: InterstitialAd, val loadedStamp: Long)


}
