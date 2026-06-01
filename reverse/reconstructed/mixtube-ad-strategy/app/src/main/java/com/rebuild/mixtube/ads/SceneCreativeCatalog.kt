package com.rebuild.mixtube.ads

object SceneCreativeCatalog {
    data class Creative(
        val headline: String,
        val body: String,
        val cta: String,
        val sponsor: String,
        val category: String,
        val backgroundBase: String,
        val cardFill: String,
        val accent: String,
        val ctaFill: String,
        val ctaText: String
    )

    fun resolve(scene: String, source: String, type: String): Creative {
        val sceneKey = scene.lowercase()
        return when {
            sceneKey.contains("switchback") -> switchback(source, type)
            sceneKey.contains("play") -> play(source, type)
            sceneKey.contains("download") -> download(source, type)
            sceneKey.contains("other") -> other(source, type)
            sceneKey.contains("cleanboost") || sceneKey.contains("setting") -> utility(source, type)
            sceneKey.contains("search") -> search(source, type)
            sceneKey.contains("native") && sceneKey.contains("home") -> homeNative(source, type)
            else -> defaultFor(source, type)
        }
    }

    private fun switchback(source: String, type: String) = Creative(
        headline = "Your music is waiting",
        body = "Continue where you left off. Discover new tracks and playlists hand-picked for your taste.",
        cta = "Resume now",
        sponsor = "Sponsored  \u00b7  Resume",
        category = "App Open",
        backgroundBase = "#0F1A2E",
        cardFill = "#1E293B",
        accent = "#38BDF8",
        ctaFill = "#38BDF8",
        ctaText = "#0F172A"
    )

    private fun play(source: String, type: String) = Creative(
        headline = "Unlock premium sound",
        body = if (type.equals("interrewarded", ignoreCase = true) || type.equals("rewarded", ignoreCase = true))
            "Watch this short sponsor message to keep playing without interruption."
        else "Take a quick break and come back to your track.",
        cta = if (type.equals("interrewarded", ignoreCase = true) || type.equals("rewarded", ignoreCase = true))
            "Claim reward"
        else "Install and continue",
        sponsor = "Sponsored  \u00b7  Player",
        category = "In-Play Ad",
        backgroundBase = "#1A1122",
        cardFill = "#2D1B3D",
        accent = "#C084FC",
        ctaFill = "#C084FC",
        ctaText = "#1A1122"
    )

    private fun download(source: String, type: String) = Creative(
        headline = "Free up space, keep the music",
        body = "Install this fast cleaner tool and reclaim storage for more songs.",
        cta = "Install now",
        sponsor = "Sponsored  \u00b7  Download",
        category = "Download Interstitial",
        backgroundBase = "#0B1F17",
        cardFill = "#1C3D2E",
        accent = "#4ADE80",
        ctaFill = "#4ADE80",
        ctaText = "#0B1F17"
    )

    private fun other(source: String, type: String) = Creative(
        headline = "Recommended for you",
        body = "Check out this app loved by millions. High ratings and easy to use.",
        cta = "Learn more",
        sponsor = "Sponsored  \u00b7  Recommendation",
        category = "Interstitial",
        backgroundBase = "#1C1917",
        cardFill = "#2D2622",
        accent = "#FB923C",
        ctaFill = "#FB923C",
        ctaText = "#1C1917"
    )

    private fun utility(source: String, type: String) = Creative(
        headline = "Boost your experience",
        body = "Keep your device running smooth with this lightweight optimizer.",
        cta = "Try it free",
        sponsor = "Sponsored  \u00b7  Utility",
        category = "Reward Gate",
        backgroundBase = "#0F1923",
        cardFill = "#1F3042",
        accent = "#FBBF24",
        ctaFill = "#FBBF24",
        ctaText = "#0F1923"
    )

    private fun search(source: String, type: String) = Creative(
        headline = "Find what you love",
        body = "Discover trending videos and music. Start exploring now.",
        cta = "Explore",
        sponsor = "Sponsored  \u00b7  Search",
        category = "Search Ad",
        backgroundBase = "#151922",
        cardFill = "#242B3D",
        accent = "#60A5FA",
        ctaFill = "#60A5FA",
        ctaText = "#151922"
    )

    private fun homeNative(scene: String, type: String) = Creative(
        headline = "Today's pick for you",
        body = "This app helps you stay organized, productive, and entertained all in one place.",
        cta = "Install",
        sponsor = "Sponsored  \u00b7  Home",
        category = "Native",
        backgroundBase = "#141414",
        cardFill = "#242424",
        accent = "#F5F5F5",
        ctaFill = "#5AEEEE",
        ctaText = "#042F2E"
    )

    private fun defaultFor(source: String, type: String) = Creative(
        headline = "Discover something new",
        body = "A fresh recommendation based on your activity.",
        cta = "Open",
        sponsor = "Sponsored",
        category = source.uppercase(),
        backgroundBase = "#111111",
        cardFill = "#222222",
        accent = "#E5E5E5",
        ctaFill = "#FFFFFF",
        ctaText = "#111111"
    )
}
