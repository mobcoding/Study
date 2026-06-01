package com.rebuild.mixtube.ads

import android.util.Log

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
    }
}
