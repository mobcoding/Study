package com.zero.base.net.convert

import com.drake.net.convert.JSONConvert
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.lang.reflect.Type

class GsonConverter : JSONConvert() {
    companion object {
        private val gson = GsonBuilder().serializeNulls().create()
    }

    override fun <R> String.parseBody(succeed: Type): R? {
        val string = try {
            JSONObject(this).getString("data")
        } catch (e: Exception) {
            this
        }
        return Gson().fromJson<R>(string, succeed)
    }
}