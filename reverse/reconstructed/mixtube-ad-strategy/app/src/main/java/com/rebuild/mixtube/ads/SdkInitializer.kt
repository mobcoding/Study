package com.rebuild.mixtube.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds

class SdkInitializer(private val context: Context) {
    fun initializeAll() {
        initializeAdMob()
        initializeTopOn()
        initializeAppLovin()
        initializeUnity()
        initializeBidMachine()
        initializePangle()
    }

    private fun initializeAdMob() {
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "MobileAds.initialize complete: ${status.adapterStatusMap.keys}")
        }
        Log.d(TAG, "MobileAds.initialize(appId=${context.getString(R.string.admob_test_app_id)}, productionAppId=${context.getString(R.string.admob_app_id)})")
    }

    private fun initializeTopOn() {
        Log.d(TAG, "TUSDK.init(appId=${context.getString(R.string.topon_app_id)}, appKey=${context.getString(R.string.topon_app_key)})")
    }

    private fun initializeAppLovin() {
        Log.d(TAG, "AppLovinSdk.initialize(appId=${context.getString(R.string.applovin_app_id)}, mediationProvider=max)")
    }

    private fun initializeUnity() {
        Log.d(TAG, "UnityAds.initialize(gameId=${context.getString(R.string.unity_app_id)})")
    }

    private fun initializeBidMachine() {
        Log.d(TAG, "BidMachine.initialize(appId=${context.getString(R.string.bidmachine_app_id)})")
    }

    private fun initializePangle() {
        Log.d(TAG, "PAGSdk.init(appId=${context.getString(R.string.pangle_app_id)})")
    }

    private companion object {
        const val TAG = "AdSdkInit"
    }
}
