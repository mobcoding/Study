package com.toolkit.admob.listener

/**
 * @date:2024/7/10 11:34
 * @path:com.toolkit.admob.OnShowAdListener
 */
interface OpenAdStatusListener {
    fun onNotReady(loadFailed: Boolean)
    fun onComplete()
}