

package com.drake.net.utils

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.drake.net.NetConfig
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.FileNotFoundException

fun Uri.fileName(): String? {
    return DocumentFile.fromSingleUri(NetConfig.app, this)?.name
}

fun Uri.mediaType(): MediaType? {
    val fileName = DocumentFile.fromSingleUri(NetConfig.app, this)?.name
    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileName)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)?.toMediaTypeOrNull()
}

/**
 * 当Uri指向的文件不存在时将抛出异常[FileNotFoundException]
 */
@Throws(FileNotFoundException::class)
fun Uri.toRequestBody(): RequestBody {
    val document = DocumentFile.fromSingleUri(NetConfig.app, this)
    val contentResolver = NetConfig.app.contentResolver
    val contentLength = document?.length() ?: -1L
    val contentType = mediaType()
    return object : RequestBody() {
        override fun contentType(): MediaType? {
            return contentType
        }

        override fun contentLength() = contentLength

        override fun writeTo(sink: BufferedSink) {
            contentResolver.openInputStream(this@toRequestBody)?.use {
                sink.writeAll(it.source())
            }
        }
    }
}