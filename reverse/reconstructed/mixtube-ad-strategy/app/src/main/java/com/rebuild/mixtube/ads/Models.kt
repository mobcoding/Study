package com.rebuild.mixtube.ads

data class AdUnitConfig(
    val adsource: String,
    val adweight: Int,
    val adtype: String,
    val placementid: String
)

data class AdStrategyConfig(
    val showCount: Int = 100,
    val sameInterval: Int = 30,
    val differentInterval: Int = 30,
    val timeOut: Int = 10,
    val mixDownloadTotalcount: Int = 1,
    val mixDownloadInternalcount: Int = 1,
    val downloadivtime: Int = 0,
    val openivtime: Int = 30,
    val coolISeconds: Int = 60,
    val coolNSeconds: Int = 30,
    val coolPSeconds: Int = 180,
    val placements: Map<String, List<AdUnitConfig>> = emptyMap()
)

data class LoadedAd(
    val scene: String,
    val unit: AdUnitConfig,
    val loadedAtMillis: Long = System.currentTimeMillis(),
    val payload: Any? = null
)

data class AdShowResult(
    val shown: Boolean,
    val scene: String,
    val message: String,
    val unit: AdUnitConfig? = null
)

enum class AdScene(val key: String) {
    SwitchBack("mixIVswitchback"),
    Play("mixIVplay"),
    Other("mixIVother"),
    Download("mixIVDownload"),
    Home("mixIV_h"),
    CleanBoost("mixIVcleanBoost"),
    Setting("mixIVSetting"),
    NativeHome("mixnative"),
    NativeSearch("NVsearch"),
    PageBanner("PageBanner"),
    NormalBanner("NormalBanner")
}
