package com.toolkit.admob.listener

/**
 * @date:2024/7/10 14:27
 * @path:com.toolkit.admob.listener.InterstitialAdStatusListener
 */
fun interface InterstitialAdListener {
    fun callback(adShown: Boolean)
}