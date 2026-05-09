package com.zero.base.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object LogUtils {
    private const val TAG = "LogUtils"
    private const val FILE_NAME = "update_log.txt" // 文件名
    private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
    private const val REQUEST_WRITE_PERMISSION = 1001 // 权限请求码
    private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss" // 日期格式

    // 单线程池用于异步写入
    private val writeExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // 全局变量：下载目录
    private val downloadDir: File by lazy {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }

    // 日期格式化器（线程安全）
    private val dateFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    }

    // 检查是否有写入外部存储的权限
    private fun hasWritePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 10+ 公共下载目录无需权限
        }
    }

    // 申请写入权限
    fun requestWritePermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasWritePermission(context)) {
            ActivityCompat.requestPermissions(context as android.app.Activity, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_PERMISSION)
        }
    }

    // 异步写入字符串（自动添加时间戳和换行）
    fun writeLine(context: Context, content: String, append: Boolean = true) {
        writeExecutor.execute {
            try {
                if (!hasWritePermission(context)) {
                    Log.e(TAG, "无写入权限，无法保存文件")
                    return@execute
                }

                // 确保目录存在
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                // 创建文件
                val file = File(downloadDir, FILE_NAME)

                // 检查文件大小，超过限制则备份
                if (append) {
                    checkAndBackupFile(file)
                }

                // 生成带时间戳的内容
                val contentWithTime = formatContentWithTime(content)

                // 写入文件
                FileWriter(file, append).use { writer ->
                    writer.appendLine(contentWithTime)
                    writer.flush()
                }
                Log.d(TAG, "内容写入成功：${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "内容写入失败", e)
            }
        }
    }

    // 格式化内容，添加时间戳
    private fun formatContentWithTime(content: String): String {
        val timestamp = dateFormat.get()?.format(Date())
        return "$content [$timestamp]"
    }

    // 检查并备份文件
    private fun checkAndBackupFile(file: File) {
        try {
            if (file.exists() && file.length() > MAX_FILE_SIZE) {
                val timestamp = dateFormat.get()?.format(Date())?.replace(":", "_")
                val backupFile = File(file.parentFile, "app_data_$timestamp.txt")
                file.renameTo(backupFile)
                Log.d(TAG, "文件超过大小限制，已备份：${backupFile.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "文件备份失败", e)
        }
    }

    // 关闭线程池
    fun shutdown() {
        writeExecutor.shutdown()
    }
}