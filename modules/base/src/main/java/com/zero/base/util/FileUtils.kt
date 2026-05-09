package com.zero.base.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.BufferedSource
import okio.appendingSink
import okio.buffer
import okio.sink
import okio.source
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException


/**
 * @date:2024/9/29 15:26
 * @path:com.zero.base.util.FilesUtils
 */
object FileUtils {


    @Throws(IOException::class)
    suspend fun createFolder(context: Context, folderName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            val folder = File(context.filesDir, folderName)
            if (!folder.exists()) {
                val success = folder.mkdir()
                if (success) {
                    Result.success("Success to create directory: $folderName")
                } else {
                    Result.failure(IOException("Failed to create directory: $folderName"))
                }
            } else {
                Result.failure(IOException("$folderName  is Exist"))
            }
        }
    }

    @Throws(IOException::class)
    suspend fun writeContentToFile(fileDir: File, folderName: String, fileName: String, content: String) {
        withContext(Dispatchers.IO) {
            val folder = File(fileDir, folderName)
            if (!folder.exists() && !folder.mkdirs()) {
                throw IOException("Failed to create directory: ${folder.absolutePath}")
            }
            val file = File(folder, fileName)
            val sink: BufferedSink = file.appendingSink().buffer()
            sink.writeUtf8("$content\n")
            sink.flush()
            sink.close()
        }
    }

    @Throws(IOException::class)
    suspend fun writeContentToFile2(context: Context, fileName: String, content: String?) {
        val file = File(context.filesDir, fileName)
        withContext(Dispatchers.IO) {
            BufferedWriter(FileWriter(file, true)).use { writer ->
                writer.write(content)
                writer.newLine()
            }
        }
    }

    @Throws(IOException::class)
    suspend fun writeContentToFile3(context: Context, fileName: String, content: String) {
        val file = File(context.filesDir, fileName)
        withContext(Dispatchers.IO) {
            context.openFileOutput(file.name, Context.MODE_APPEND).use { fos ->
                val contentWithNewLine = content + System.lineSeparator()
                fos.write(contentWithNewLine.toByteArray())
            }
        }
    }

    suspend fun deleteFiles(context: Context, folderName: String, vararg fileNames: String): List<Result<String>> {
        val folder = File(context.filesDir, folderName)
        if (!folder.exists()) {
            throw IOException("Failed to create directory: ${folder.absolutePath}")
        }
        return withContext(Dispatchers.IO) {
            fileNames.map { fileName ->
                try {
                    val file = File(folder, fileName)
                    if (file.exists()) {
                        val result = file.delete()
                        if (result) {
                            Result.success("$fileName deleted successfully.")
                        } else {
                            Result.failure(IOException("Failed to delete $fileName"))
                        }
                    } else {
                        Result.failure(IOException("$fileName does not exist."))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    suspend fun deleteFilesByFolder(context: Context, folderName: String): Int {
        return withContext(Dispatchers.IO) {
            val folder = File(context.filesDir, folderName)
            if (!folder.exists() || !folder.isDirectory) {
                return@withContext 0
            }
            val files = folder.listFiles() ?: return@withContext 0

            var deletedCount = 0
            files.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                }
            }
            folder.delete()
            return@withContext deletedCount
        }
    }


    @Throws(IOException::class)
    suspend fun readLines(context: Context, fileName: String, callback: (String) -> Unit) {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            return
        }
        withContext(Dispatchers.IO) {
            file.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    withContext(Dispatchers.Main) {
                        callback(line)
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    suspend fun readLines2(context: Context, fileName: String, callback: (String) -> Unit) {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            return
        }
        withContext(Dispatchers.IO) {
            val source: BufferedSource = file.source().buffer()
            while (true) {
                val lineContent = source.readUtf8Line() ?: break
                withContext(Dispatchers.Main) {
                    callback(lineContent)
                }
            }
            source.close()
        }
    }

    suspend fun copyFile(context: Context, sourceFileName: String, targetFileName: String) {
        val sourceFile = File(context.filesDir, sourceFileName)
        val targetFile = File(context.filesDir, targetFileName)

        if (!sourceFile.exists()) {
            return
        }
        withContext(Dispatchers.IO) {
            sourceFile.source().buffer().use { source ->
                targetFile.sink().buffer().use { sink ->
                    source.readAll(sink)
                }
            }
        }
    }

    fun listFilesByFolderName(context: Context, folderName: String): List<File> {
        val folder = File(context.filesDir, folderName)
        val fileList = mutableListOf<File>()
        if (folder.exists() && folder.isDirectory) {
            val files = folder.listFiles()
            if (files != null) {
                for (file in files) {
                    fileList.add(file)
                }
            } else {
                println("No files found in the directory.")
            }
        } else {
            println("Directory does not exist or is not a directory.")
        }

        return fileList
    }


}
