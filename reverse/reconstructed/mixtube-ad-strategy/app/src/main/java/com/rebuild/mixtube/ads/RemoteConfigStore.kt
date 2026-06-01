package com.rebuild.mixtube.ads

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

interface RemoteConfigStore {
    fun fetchAndActivate(): Boolean
    fun getString(key: String, defaultValue: String = ""): String
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun getDouble(key: String, defaultValue: Double = 0.0): Double
}

class AssetRemoteConfigStore(
    private val context: Context,
    private val assetName: String = "remote_config.json"
) : RemoteConfigStore {
    private val gson = Gson()
    private var values: Map<String, Any> = emptyMap()

    override fun fetchAndActivate(): Boolean {
        val json = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, Any>>() {}.type
        values = gson.fromJson(json, type)
        Log.d(TAG, "fetchAndActivate: loaded ${values.size} keys from $assetName")
        return true
    }

    override fun getString(key: String, defaultValue: String): String {
        val value = values[key]?.toString() ?: defaultValue
        Log.d(TAG, "getString($key) = $value")
        return value
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        val value = when (val raw = values[key]) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull()
            else -> null
        } ?: defaultValue
        Log.d(TAG, "getLong($key) = $value")
        return value
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        val value = when (val raw = values[key]) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull()
            else -> null
        } ?: defaultValue
        Log.d(TAG, "getDouble($key) = $value")
        return value
    }

    private companion object {
        const val TAG = "RemoteConfig"
    }
}
