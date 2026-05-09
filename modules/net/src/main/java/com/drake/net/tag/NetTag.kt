

package com.drake.net.tag

import com.drake.net.interfaces.ProgressListener
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KType

sealed class NetTag {
    class Extras : HashMap<String, Any?>()
    class UploadListeners : ConcurrentLinkedQueue<ProgressListener>()
    class DownloadListeners : ConcurrentLinkedQueue<ProgressListener>()

    @JvmInline
    value class RequestId(val value: Any)

    @JvmInline
    value class RequestGroup(val value: Any)

    @JvmInline
    value class RequestKType(val value: KType)

    @JvmInline
    value class DownloadFileMD5Verify(val value: Boolean = true)

    @JvmInline
    value class DownloadFileNameDecode(val value: Boolean = true)

    @JvmInline
    value class DownloadTempFile(val value: Boolean = true)

    @JvmInline
    value class DownloadFileConflictRename(val value: Boolean = true)

    @JvmInline
    value class DownloadFileName(val value: String)

    @JvmInline
    value class CacheKey(val value: String)

    @JvmInline
    value class CacheValidTime(val value: Long)

    @JvmInline
    value class DownloadFileDir(val value: String) {
        constructor(fileDir: File) : this(fileDir.absolutePath)
    }
}
