package com.zero.base.net.interceptor

import com.drake.net.interceptor.RequestInterceptor
import com.drake.net.request.BaseRequest

class GlobalParamInterceptor : RequestInterceptor {

    override fun interceptor(request: BaseRequest) {
        request.apply {
            setQuery("version", "1.1.0")
            setQuery("os", "android")
        }
    }
}