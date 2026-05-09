
package com.drake.net.interfaces

import com.drake.net.component.Progress

/**
 * 进度监听器, 可能为下载或上传
 *
 * @param interval 进度监听器刷新的间隔时间, 单位为毫秒, 默认值为500ms
 */
abstract class ProgressListener(var interval: Long = 500) {
    // 上次进度变化的的开机时间
    var elapsedTime = 0L

    // 距离上次进度变化的新增字节数
    var intervalByteCount = 0L

    /**
     * 监听上传/下载进度回调函数
     * @param p 上传或者下载进度
     */
    abstract fun onProgress(p: Progress)
}