

package com.drake.net.transform

import kotlinx.coroutines.Deferred

/**
 * 可以将[Deferred]返回结果进行转换
 * [block]在[Deferred]执行成功返回结果时执行
 */
fun <T, R> Deferred<T>.transform(block: (T) -> R): DeferredTransform<T, R> {
    return DeferredTransform(this, block)
}

data class DeferredTransform<T, R>(val deferred: Deferred<T>, val block: (T) -> R)