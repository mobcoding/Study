package com.zero.base.ext

import android.content.Context
import com.google.gson.Gson
import com.google.gson.internal.Primitives
import org.json.JSONArray
import org.json.JSONObject
import java.io.Reader
import java.lang.reflect.Type

/**
 * @date:2024/12/11 17:05
 * @path:com.zero.base.ext.GsonExt
 */

val gson: Gson = Gson()
inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, T::class.java)

fun Context.readJson(fileName: String): String =
    assets.open(fileName).bufferedReader().use { it.readText() }

/**
 * 对象转json
 */
fun Any?.toJson(): String = gson.toJson(this)

/**
 * json解析 {}
 */
fun <T> String?.fromJsonObj(cls: Class<T>): T? {
    if (isNullOrBlank()) return null
    val obj = gson.fromJson<Any>(this, cls as Type)
    return Primitives.wrap(cls).cast(obj)
}

fun <T> Reader?.fromJsonObj(cls: Class<T>): T? {
    val obj = gson.fromJson<Any>(this, cls as Type)
    return Primitives.wrap(cls).cast(obj)
}

/**
 * json解析 []
 *
 * @return 创建一个新的List可以避免类型转换异常
 */
fun <T> String?.fromJsonList(cls: Class<Array<T>>): MutableList<T>? {
    if (isNullOrBlank()) return null
    val arr = fromJsonObj(cls) ?: return null
    return mutableListOf(*arr)
}

fun <T> Reader?.fromJsonList(cls: Class<Array<T>>): MutableList<T>? {
    val arr = fromJsonObj(cls) ?: return null
    return mutableListOf(*arr)
}

/**
 * 获取JSONObject对象
 */
fun String?.jsonObject(): JSONObject? = try {
    this?.let { JSONObject(this) }
} catch (_: Exception) {
    null
}

/**
 * 获取JSONArray对象
 */
fun String?.jsonArray(): JSONArray? = try {
    this?.let { JSONArray(this) }
} catch (_: Exception) {
    null
}
