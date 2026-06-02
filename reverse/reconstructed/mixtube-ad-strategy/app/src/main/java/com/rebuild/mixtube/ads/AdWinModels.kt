package com.rebuild.mixtube.ads

import android.content.Context
import android.util.Log
import java.util.Locale

data class AdWinNativeAd(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String,
    val imageUrl: String,
    val clickUrl: String,
    val impressionUrls: List<String>,
    val downloadUrl: String,
    val rating: Float = 4.5f,
    val ctaText: String = "Install",
    val category: String = "Utilities",
    val packageName: String = ""
)

class AdWinRepository {
    private var currentIndex = 0
    private val prefsName = "adwin_policy"

    private val adPool = listOf(
        AdWinNativeAd(
            id = "adwin_1001",
            title = "Phone Cleaner & Speed Booster",
            description = "Clean up junk files and boost your phone speed. One tap to free up storage, save battery, and scan for viruses. Trusted by 50M+ users worldwide.",
            iconUrl = "https://cdn.example.invalid/cleaner/icon.png",
            imageUrl = "https://cdn.example.invalid/cleaner/banner.jpg",
            clickUrl = "https://adwin.example.invalid/click/1001",
            impressionUrls = listOf("https://adwin.example.invalid/imp/1001/a", "https://adwin.example.invalid/imp/1001/b"),
            downloadUrl = "https://play.google.com/store/apps/details?id=com.example.cleaner",
            rating = 4.7f, ctaText = "Install", category = "Tools", packageName = "com.example.cleaner"
        ),
        AdWinNativeAd(
            id = "adwin_1002",
            title = "Music Downloader Pro",
            description = "Download any song or playlist for offline listening. Supports 1000+ music platforms with high-quality audio streaming. Free trial available.",
            iconUrl = "https://cdn.example.invalid/music/icon.png",
            imageUrl = "https://cdn.example.invalid/music/banner.jpg",
            clickUrl = "https://adwin.example.invalid/click/1002",
            impressionUrls = listOf("https://adwin.example.invalid/imp/1002/a", "https://adwin.example.invalid/imp/1002/b"),
            downloadUrl = "https://play.google.com/store/apps/details?id=com.example.musicpro",
            rating = 4.5f, ctaText = "Try Free", category = "Music & Audio", packageName = "com.example.musicpro"
        ),
        AdWinNativeAd(
            id = "adwin_1003",
            title = "VPN Proxy Master - Secure & Fast",
            description = "Browse anonymously with military-grade encryption. 3000+ servers in 90 countries. Unlimited bandwidth.",
            iconUrl = "https://cdn.example.invalid/vpn/icon.png",
            imageUrl = "https://cdn.example.invalid/vpn/banner.jpg",
            clickUrl = "https://adwin.example.invalid/click/1003",
            impressionUrls = listOf("https://adwin.example.invalid/imp/1003/a", "https://adwin.example.invalid/imp/1003/b"),
            downloadUrl = "https://play.google.com/store/apps/details?id=com.example.vpnpro",
            rating = 4.3f, ctaText = "Install", category = "Productivity", packageName = "com.example.vpnpro"
        ),
        AdWinNativeAd(
            id = "adwin_1004",
            title = "Photo Editor - Filters & Effects",
            description = "Transform your photos with professional-grade filters, stickers, text overlays, and beauty tools.",
            iconUrl = "https://cdn.example.invalid/photo/icon.png",
            imageUrl = "https://cdn.example.invalid/photo/banner.jpg",
            clickUrl = "https://adwin.example.invalid/click/1004",
            impressionUrls = listOf("https://adwin.example.invalid/imp/1004/a", "https://adwin.example.invalid/imp/1004/b"),
            downloadUrl = "https://play.google.com/store/apps/details?id=com.example.photoeditor",
            rating = 4.8f, ctaText = "Edit Now", category = "Photography", packageName = "com.example.photoeditor"
        ),
        AdWinNativeAd(
            id = "adwin_1005",
            title = "Video Downloader - All Formats",
            description = "Download videos and music from 10000+ websites. Support 4K quality, batch downloads, background playback.",
            iconUrl = "https://cdn.example.invalid/viddl/icon.png",
            imageUrl = "https://cdn.example.invalid/viddl/banner.jpg",
            clickUrl = "https://adwin.example.invalid/click/1005",
            impressionUrls = listOf("https://adwin.example.invalid/imp/1005/a", "https://adwin.example.invalid/imp/1005/b"),
            downloadUrl = "https://play.google.com/store/apps/details?id=com.example.viddl",
            rating = 4.6f, ctaText = "Download", category = "Video Players", packageName = "com.example.viddl"
        )
    )

    fun canShow(context: Context, remoteConfig: RemoteConfigStore): AdWinDecision {
        val showTimeHours = remoteConfig.getLong("adwin_show_time", 48L).coerceAtLeast(0L)
        val maxCount = remoteConfig.getLong("adwin_show_max_count", 30L).coerceAtLeast(0L)
        val minIntervalHours = remoteConfig.getLong("adwin_show_inter_count", 1L).coerceAtLeast(0L)
        val allowCountriesRaw = remoteConfig.getString("adwin_show_country", "").trim()

        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val shownCount = prefs.getLong(KEY_SHOWN_COUNT, 0L)
        val lastShownAt = prefs.getLong(KEY_LAST_SHOWN_AT, 0L)

        if (maxCount > 0 && shownCount >= maxCount) return AdWinDecision(false, "max_count")

        val packageInfo = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        val firstInstallTime = packageInfo?.firstInstallTime ?: 0L
        if (firstInstallTime > 0 && showTimeHours > 0) {
            val requiredMillis = showTimeHours * 3_600_000L
            if (System.currentTimeMillis() - firstInstallTime < requiredMillis) return AdWinDecision(false, "install_window")
        }

        if (lastShownAt > 0 && minIntervalHours > 0) {
            val requiredMillis = minIntervalHours * 3_600_000L
            if (System.currentTimeMillis() - lastShownAt < requiredMillis) return AdWinDecision(false, "interval")
        }

        if (allowCountriesRaw.isNotBlank()) {
            val allow = allowCountriesRaw
                .split(",", ";", " ")
                .mapNotNull { it.trim().takeIf { s -> s.isNotBlank() } }
                .map { it.uppercase(Locale.US) }
                .toSet()
            val country = Locale.getDefault().country.uppercase(Locale.US)
            if (country.isNotBlank() && country !in allow) return AdWinDecision(false, "country")
        }

        return AdWinDecision(true, "ok")
    }

    fun recordShown(context: Context) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val count = prefs.getLong(KEY_SHOWN_COUNT, 0L) + 1L
        prefs.edit()
            .putLong(KEY_SHOWN_COUNT, count)
            .putLong(KEY_LAST_SHOWN_AT, System.currentTimeMillis())
            .apply()
    }

    fun currentAd(): AdWinNativeAd {
        val ad = adPool[currentIndex % adPool.size]
        currentIndex++
        Log.d(TAG, "currentAd: id=${ad.id} title=${ad.title}")
        return ad
    }

    fun sendImpressions(ad: AdWinNativeAd) {
        ad.impressionUrls.forEach { url ->
            Log.d(TAG, "send impression: id=${ad.id} url=$url")
        }
    }

    fun sendClick(ad: AdWinNativeAd) {
        Log.d(TAG, "send click: id=${ad.id} url=${ad.clickUrl}")
        Log.d(TAG, "redirect to Play Store: ${ad.downloadUrl}")
    }

    private companion object {
        const val TAG = "AdWin"
        const val KEY_SHOWN_COUNT = "shown_count"
        const val KEY_LAST_SHOWN_AT = "last_shown_at"
    }
}

data class AdWinDecision(
    val allowed: Boolean,
    val reason: String,
)
