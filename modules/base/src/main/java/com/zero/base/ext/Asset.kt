package com.zero.base.ext

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun Context.copyAssetsToFileDir(assetPath: String = "", createDir: Boolean = false) {
    try {
        val assetManager = assets
        val files = assets.list(assetPath)

        // 1. assetPath 是单个文件，直接复制到 filesDir 根目录
        if (files.isNullOrEmpty()) {
            val inputStream = assetManager.open(assetPath)
            val outFile = File(filesDir, assetPath.substringAfterLast('/'))

            val assetSize = inputStream.available().toLong()
            val needCopy = !outFile.exists() || outFile.length() != assetSize

            if (needCopy) {
                inputStream.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                inputStream.close()
            }
            return
        }

        // 2. assetPath 是目录
        for (fileName in files) {
            val fullPath = if (assetPath.isEmpty()) fileName else "$assetPath/$fileName"
            val subFiles = assetManager.list(fullPath)

            if (subFiles.isNullOrEmpty()) {
                // 是文件
                val inputStream = assetManager.open(fullPath)

                val outFile = if (createDir && assetPath.isNotEmpty()) {
                    val dir = File(filesDir, assetPath)
                    dir.mkdirs()
                    File(dir, fileName)
                } else {
                    File(filesDir, fileName)
                }

                val assetSize = inputStream.available().toLong()
                val needCopy = !outFile.exists() || outFile.length() != assetSize

                if (needCopy) {
                    inputStream.use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    inputStream.close()
                }
            } else {
                // 是目录，递归处理
                copyAssetsToFileDir(fullPath, createDir)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}



