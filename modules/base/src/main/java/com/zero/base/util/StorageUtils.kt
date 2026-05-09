package com.zero.base.util

import android.content.Context
import com.tencent.mmkv.MMKV

/**
 * @date:2024/6/15 16:57
 * @path:com.toolkit.openvpn.util.StorageUtils
 */
object StorageUtils {
    private var storage: MMKV? = null

    fun init(context: Context) {
        MMKV.initialize(context)
        try {
            storage = MMKV.mmkvWithID(context.packageName, MMKV.MULTI_PROCESS_MODE, "ug8ge6")
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun has(key: String?): Boolean {
        return storage!!.containsKey(key)
    }

    fun getBoolean(key: String?, def: Boolean): Boolean {
        return storage!!.getBoolean(key, def)
    }

    fun putBoolean(key: String?, value: Boolean) {
        storage!!.putBoolean(key, value)
    }

    fun getLong(key: String?): Long {
        return storage!!.getLong(key, 0)
    }

    fun getLong(key: String?, def: Long): Long {
        return storage!!.getLong(key, def)
    }

    fun getInt(key: String?): Int {
        return storage!!.getInt(key, 0)
    }


    fun getFloat(key: String?, defaultValue: Float): Float {
        return storage!!.getFloat(key, defaultValue)
    }

    fun getInt(key: String?, def: Int): Int {
        return storage!!.getInt(key, def)
    }

    fun getString(key: String?, def: String?): String? {
        return storage!!.getString(key, def)
    }

    fun putBoolean(key: String?, value: Boolean?) {
        storage!!.putBoolean(key, value!!)
    }

    fun putLong(key: String?, value: Long) {
        storage!!.putLong(key, value)
    }

    fun putInt(key: String?, value: Int) {
        storage!!.putInt(key, value)
    }

    fun putFloat(key: String?, value: Float) {
        storage!!.putFloat(key, value)
    }

    fun putString(key: String?, value: String?) {
        storage!!.putString(key, value)
    }

    fun remove(key: String?) {
        storage!!.remove(key)
    }
}
