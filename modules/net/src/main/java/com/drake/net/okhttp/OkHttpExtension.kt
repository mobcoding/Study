

package com.drake.net.okhttp

import com.drake.net.interceptor.NetOkHttpInterceptor
import okhttp3.OkHttpClient

/**
 * Net要求经过该函数处理创建特殊的OkHttpClient
 */
fun OkHttpClient.toNetOkhttp() = run {
    if (!interceptors.contains(NetOkHttpInterceptor)) {
        newBuilder().addInterceptor(NetOkHttpInterceptor).build()
    } else {
        this
    }
}