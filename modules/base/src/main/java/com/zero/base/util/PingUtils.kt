package com.zero.base.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * @date:2024/8/27 14:58
 * @path:com.polarbear.linkplus.toolkit.DelayTestUtils
 */
object PingUtils {
    suspend fun pingDelay(host: String?): String {
        var delayTime = ""
        return try {
            withContext(Dispatchers.IO) {
                val processBuilder = ProcessBuilder("/system/bin/ping", "-c", "1", host)
                val process = processBuilder.start()
                val inputStream = BufferedReader(InputStreamReader(process.inputStream))
                while (true) {
                    val line = inputStream.readLine() ?: break
                    if (line.contains("time=")) {
                        val startIndex = line.indexOf("time=")
                        delayTime = line.substring(startIndex).replace("time=", "").trim()
                        break
                    }
                }
                inputStream.close()
                process.destroy()
                delayTime
            }
        } catch (e: Exception) {
            delayTime
        }
    }

    fun extractDelayNum(input: String): Int {
        val regex = Regex("\\d+")
        val matchResult = regex.find(input)
        val numberString = matchResult?.value ?: "0"
        return numberString.toInt()
    }
}