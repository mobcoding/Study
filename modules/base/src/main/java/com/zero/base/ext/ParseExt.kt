package com.zero.base.ext

/**
 * @date:2025/9/25 10:39
 * @path:com.zero.base.ext.ParseExt
 */

/**
 *
 */
fun parseInt(str: String): Result<Int> {
    return runCatching { str.toInt() }
}