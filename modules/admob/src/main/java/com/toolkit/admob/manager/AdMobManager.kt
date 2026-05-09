package com.toolkit.admob.manager

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.toolkit.admob.AppLifecycle
import com.toolkit.admob.manager.InterstitialAdManager.loadAd
import com.toolkit.admob_libray.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean


object AdMobManager {

    val atomicBoolean = AtomicBoolean(false)
    var appLifecycle: AppLifecycle? = null

    var connected = false
    fun initLifecycle(application: Application, filterClassName: String) {
        appLifecycle = AppLifecycle(application, filterClassName)
    }

    fun initMobileAds(app: Context, timeFirst: Boolean) {
        if (atomicBoolean.getAndSet(true)) {
            logMsg("-->Admob initialized ")
            return
        }
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            logMsg("--> Admob initializing... ")
            MobileAds.initialize(app) {
                atomicBoolean.set(true)
                logMsg("--> Admob complete... ")
                if (timeFirst) {
                    loadAd(app, BuildConfig.ADMOB_INTERSTITIAL_GUIDE)
                }
            }

        }
    }

    fun dealConsentActionThen(activity: Activity, action: Runnable, timeFirst: Boolean) {
        val params: ConsentRequestParameters
        if (BuildConfig.DEBUG) {
            val debugSettings = ConsentDebugSettings.Builder(activity).setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA).addTestDeviceHashedId("314F2F7853AB35EEE665C2D365318FA0").build()
            params = ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings).build()
        } else {
            params = ConsentRequestParameters.Builder().build()
        }
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError: FormError? ->
                if (formError != null) {
                    Log.e("PVX", String.format(Locale.ENGLISH, "Error:-> %d: %s", formError.errorCode, formError.message))
                }
                if (consentInformation.canRequestAds()) {
                    initMobileAds(activity, timeFirst)
                }
                action.run()
            }
        }, { formError: FormError ->
            Log.e("PVX", String.format(Locale.ENGLISH, "Error: -> %d: %s", formError.errorCode, formError.message))
            if (consentInformation.canRequestAds()) {
                initMobileAds(activity, timeFirst)
            }
            action.run()
        })
        if (consentInformation.canRequestAds()) {
            initMobileAds(activity, timeFirst)
        }
    }


    fun logMsg(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d("zzz", "logMsg: $msg")
        }
    }
}
