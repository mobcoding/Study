

@file:Suppress("MemberVisibilityCanBePrivate")

package com.drake.net.exception

import okhttp3.Response

/**
 * 状态码在200..299, 但是返回数据不符合业务要求可以抛出该异常
 * @param response 响应信息
 * @param message 错误描述信息
 * @param cause 错误原因
 * @param tag 可携带任意对象, 一般用于在转换器/拦截器中添加然后传递给错误处理器去使用判断
 */
class ResponseException(
    response: Response,
    message: String? = null,
    cause: Throwable? = null,
    var tag: Any? = null
) : HttpResponseException(response, message, cause)