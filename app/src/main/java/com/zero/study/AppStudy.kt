package com.zero.study

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import android.widget.Toast
import com.toolkit.admob.manager.AdMobManager.initLifecycle
import com.zero.study.ui.activity.SplashActivity
import kotlin.properties.Delegates


class AppStudy : Application() {


    companion object {
        var appContext: Context by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        if (isMainProcess(this)) {
            initLifecycle(this, SplashActivity::class.java.simpleName)
//            Gloading.default?.initDefault(LoadingAdapter())
//            OneSignal.Debug.logLevel = LogLevel.VERBOSE
//            OneSignal.initWithContext(this, "a4075666-c2b1-41aa-b8e4-a668bbb1036c")
//            CoroutineScope(Dispatchers.IO).launch {
//                OneSignal.Notifications.requestPermission(true)
//            }
        }
    }

    private fun isMainProcess(context: Context): Boolean {
        val myPid = Process.myPid()
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val processes = activityManager.runningAppProcesses
        if (processes != null) {
            for (processInfo in processes) {
                if (processInfo.pid == myPid) {
                    return context.packageName == processInfo.processName
                }
            }
        }
        return false
    }


    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            Toast.makeText(this@AppStudy, "已进入后台运行,请注意使用安全",
                Toast.LENGTH_SHORT).show()
        }
    }
}
