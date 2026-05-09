package com.drake.net.interceptor

import androidx.annotation.IntRange
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly

/**
 * 重试次数拦截器
 * OkHttp自带重试请求规则, 本拦截器并不推荐使用
 * 因为长时间阻塞用户请求是不合理的, 发生错误请让用户主动重试, 例如显示缺省页或者提示
 * @property retryCount 重试次数
 */
class RetryInterceptor(@param:IntRange(from = 1) var retryCount: Int = 3) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var retryCount = 0
        val request = chain.request()
        var response = chain.proceed(request)
        while (!response.isSuccessful && retryCount < this.retryCount) {
            retryCount++
            response.closeQuietly()
            response = chain.proceed(request)
        }
        return response
    }
}