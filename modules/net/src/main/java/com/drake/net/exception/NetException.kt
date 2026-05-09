

package com.drake.net.exception

import okhttp3.Request
import java.io.IOException


/**
 * 表示为Net发生的网络异常
 * 在转换器[com.drake.net.convert.NetConverter]中抛出的异常如果没有继承该类都会被视为数据转换异常[ConvertException], 该类一般用于自定义异常
 * @param request 请求信息
 * @param message 错误描述信息
 * @param cause 错误原因
 */
open class NetException(
    open val request: Request,
    message: String? = null,
    cause: Throwable? = null,
) : IOException(message, cause) {

    var occurred: String = ""

    override fun getLocalizedMessage(): String? {
        return "${if (message == null) "" else message + " "}${request.url}$occurred"
    }
}