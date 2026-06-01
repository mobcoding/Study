package com.rebuild.mixtube.ads

import android.app.Application
import android.util.Log

class MixtubeRebuildApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        registerActivityLifecycleCallbacks(ServiceLocator.appForegroundTracker)
        ServiceLocator.sdkInitializer.initializeAll()
        Log.d(TAG, "Application onCreate: rebuilt ad strategy runtime is ready")
    }

    private companion object {
        const val TAG = "MixtubeRebuild"
    }
}
