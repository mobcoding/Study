package com.toolkit.admob

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.toolkit.admob.activity.OpenActivity
import com.toolkit.admob.listener.AbstractLifecycleCallbacks
import com.toolkit.admob.manager.AdMobManager
import com.toolkit.admob.manager.AppOpenAdManager
import com.toolkit.admob.manager.InterstitialAdManager
import java.lang.ref.WeakReference

/**
 * @author Admin
 */
class AppLifecycle(application: Application, private var filterClassName: String) : DefaultLifecycleObserver, AbstractLifecycleCallbacks() {
    private var currentActivity: WeakReference<Activity>? = null

    private var enableAdTime: Long = 0

    /**
     * Constructor
     */
    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * LifecycleObserver method that shows the app open ad when the app moves to foreground.
     */
    override fun onStart(owner: LifecycleOwner) {
        if (currentActivity?.get() !== null && filterClassName == currentActivity?.get()?.javaClass?.simpleName) {
            return
        }
        val now = System.currentTimeMillis()
        if (now <= enableAdTime) {
            enableOpenAd()
            return
        }
        if (currentActivity?.get() == null) {
            return
        }
        val intent = Intent(currentActivity?.get(), OpenActivity::class.java)
        currentActivity?.get()?.startActivity(intent)

    }

    private fun setEnableAdTime(stamp: Long) {
        enableAdTime = stamp
    }


    fun enableOpenAd() {
        setEnableAdTime(0)
    }

    fun disableOpenAd(timeMill: Long) {
        setEnableAdTime(System.currentTimeMillis() + timeMill)
    }


    override fun onActivityStarted(activity: Activity) {
        currentActivity = null
        if (AppOpenAdManager.isAdShowing) {
            AdMobManager.logMsg("AppOpenAd iShowing return...")
            return
        }
        if (InterstitialAdManager.isAdShowing) {
            AdMobManager.logMsg("InterstitialAd isShowing return...")
            return
        }
        currentActivity = WeakReference(activity);
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    val lastOpenAdShowTime: Long
        /**
         * 返回上次开屏广告展示时间戳
         *
         * @return
         */
        get() = AppOpenAdManager.openAdLastShowTime


}