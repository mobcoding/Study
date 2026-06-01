package com.rebuild.mixtube.ads

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class AdConfigRepository(
    private val context: Context,
    private val assetName: String = "ad_config.json"
) {
    private val gson = Gson()
    private var cached: AdStrategyConfig? = null

    fun load(): AdStrategyConfig {
        cached?.let { return it }
        val json = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val root = gson.fromJson(json, JsonObject::class.java)
        val placements = root.entrySet()
            .filter { it.value is JsonArray }
            .associate { (scene, value) ->
                scene to value.asJsonArray.map { gson.fromJson(it, AdUnitConfig::class.java) }
            }
        return AdStrategyConfig(
            showCount = root.intValue("showCount", 100),
            sameInterval = root.intValue("sameInterval", 30),
            differentInterval = root.intValue("differentInterval", 30),
            timeOut = root.intValue("timeOut", 10),
            mixDownloadTotalcount = root.intValue("mixDownloadTotalcount", 1),
            mixDownloadInternalcount = root.intValue("mixDownloadInternalcount", 1),
            downloadivtime = root.intValue("downloadivtime", 0),
            openivtime = root.intValue("openivtime", 30),
            placements = placements
        ).also { cached = it }
    }

    fun unitsFor(scene: String): List<AdUnitConfig> = load().placements[scene].orEmpty()

    private fun JsonObject.intValue(name: String, defaultValue: Int): Int {
        return if (has(name) && !get(name).isJsonNull) get(name).asInt else defaultValue
    }
}
