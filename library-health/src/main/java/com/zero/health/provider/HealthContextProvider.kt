package com.zero.health.provider

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.drake.net.NetConfig
import com.drake.net.interceptor.LogRecordInterceptor
import com.drake.net.okhttp.setConverter
import com.drake.net.okhttp.setDebug
import com.drake.net.okhttp.setRequestInterceptor
import com.zero.base.data.IpManager
import com.zero.base.net.convert.GsonConverter
import com.zero.base.net.interceptor.GlobalParamInterceptor
import com.zero.base.util.StorageUtils
import com.zero.library_base.BuildConfig
import okhttp3.Cache
import java.util.concurrent.TimeUnit

class HealthContextProvider : Initializer<Unit> {
    companion object {
        private var _appContext: Context? = null
        private const val TIME_OUT_SECONDS = 30L
        val appContext: Context
            get() = _appContext ?: throw IllegalStateException(
                "MusicInitializer 尚未初始化！请确保在 Manifest 中注册了 App Startup")
    }

    override fun create(context: Context) {
        val app = context.applicationContext as Application
        _appContext = context.applicationContext
        StorageUtils.init(app)
        NetConfig.initialize(IpManager.getDefaultIP(), context) {
            connectTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
            readTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
            writeTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
            cache(Cache(context.cacheDir, 1024 * 1024 * 64))
            setDebug(BuildConfig.DEBUG)
            setRequestInterceptor(GlobalParamInterceptor())
            addInterceptor(LogRecordInterceptor(BuildConfig.DEBUG))
            setConverter(GsonConverter())
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }

}