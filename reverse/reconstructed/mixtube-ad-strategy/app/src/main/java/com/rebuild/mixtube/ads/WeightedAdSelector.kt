package com.rebuild.mixtube.ads

import android.content.Context

import kotlin.math.max

class WeightedAdSelector(context: Context) {
    private val prefs = context.getSharedPreferences("weighted_ad_selector", Context.MODE_PRIVATE)

    fun select(scene: String, units: List<AdUnitConfig>): AdUnitConfig {
        require(units.isNotEmpty()) { "No ad units configured for $scene" }
        val expanded = units.flatMap { unit -> List(max(1, unit.adweight)) { unit } }
        val indexKey = "index_$scene"
        val index = prefs.getInt(indexKey, 0).floorMod(expanded.size)
        prefs.edit().putInt(indexKey, index + 1).apply()
        return expanded[index]
    }

    private fun Int.floorMod(modulus: Int): Int {
        val value = this % modulus
        return if (value < 0) value + modulus else value
    }
}
