

package com.drake.net.exception

import com.drake.net.Net
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.CancellationException

/**
 * 取消网络任务的异常
 */
class NetCancellationException(
    coroutineScope: CoroutineScope,
    message: String? = null,
) : CancellationException(message) {
    init {
        Net.cancelGroup(coroutineScope.coroutineContext[CoroutineExceptionHandler])
    }
}


/**
 * 在作用域中抛出该异常将取消其作用域内所有的网络请求(如果存在的话)
 */
@Suppress("FunctionName")
fun CoroutineScope.NetCancellationException(message: String? = null): NetCancellationException {
    return NetCancellationException(this, message)
}