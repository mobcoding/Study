package com.rebuild.mixtube.ads

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

class BusinessPageActivity : AppCompatActivity() {
    private var nativeAd: NativeAd? = null
    private var bannerAdView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val scene = intent.getStringExtra(EXTRA_SCENE).orEmpty()
        val inlineScene = intent.getStringExtra(EXTRA_INLINE_SCENE).orEmpty()
        val pageKind = intent.getStringExtra(EXTRA_PAGE_KIND)
            ?.let(PageKind::valueOf)
            ?: PageKind.HOME
        setContentView(buildPage(title = title, triggerScene = scene, pageKind = pageKind, inlineScene = inlineScene))
        Log.d(TAG, "open page=$pageKind triggerScene=$scene inlineScene=$inlineScene")
    }

    private fun buildPage(
        title: String,
        triggerScene: String,
        pageKind: PageKind,
        inlineScene: String
    ): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
            setBackgroundColor(Color.parseColor("#F4F7FB"))
        }
        content.addView(pageHeader(title, pageKind))
        content.addView(metaStrip(triggerScene, inlineScene))
        when (pageKind) {
            PageKind.HOME -> buildHome(content, inlineScene)
            PageKind.LIKE -> buildLike(content, inlineScene)
            PageKind.SEARCH -> buildSearch(content, inlineScene)
            PageKind.PLAYLIST -> buildPlaylist(content, inlineScene)
            PageKind.DOWNLOAD -> buildDownload(content, inlineScene)
            PageKind.PLAYER -> buildPlayer(content, inlineScene)
        }
        return ScrollView(this).apply { addView(content) }
    }

    private fun buildHome(root: LinearLayout, inlineScene: String) {
        root.addView(heroCard("Trending for you", "Fresh music picks and hot playlists tuned for a switch-tab comeback."))
        root.addView(sectionLabel("Mixes"))
        root.addView(songCard("Late Night Drive", "Daily Mix", "32 songs"))
        root.addView(songCard("Summer Replay", "Smart Playlist", "18 songs"))
        appendInlinePlacement(root, inlineScene, "Sponsored Mix")
        root.addView(sectionLabel("Recently played"))
        root.addView(songCard("Afterglow", "The Midnight", "3:42"))
        root.addView(songCard("Lights Down Low", "MAX", "4:08"))
    }

    private fun buildLike(root: LinearLayout, inlineScene: String) {
        root.addView(heroCard("Liked songs", "Your favorite tracks stay one tap away with quick resume and daily recommendations."))
        root.addView(sectionLabel("Favorites"))
        root.addView(songCard("Stargazing", "Liked track", "4:13"))
        root.addView(songCard("Golden Hour", "Liked track", "3:15"))
        appendInlinePlacement(root, inlineScene, "Recommended for fans")
        root.addView(sectionLabel("Because you liked this"))
        root.addView(songCard("Runaway Lights", "Suggested artist", "2:58"))
    }

    private fun buildSearch(root: LinearLayout, inlineScene: String) {
        root.addView(searchBox("Search songs, artists, albums"))
        appendInlinePlacement(root, inlineScene, "Sponsored result")
        root.addView(sectionLabel("Top results"))
        root.addView(songCard("Mixtube Originals", "Artist channel", "New release"))
        root.addView(songCard("Moonlight Echo", "Album", "12 tracks"))
        root.addView(songCard("Night Runner", "Song", "3:21"))
    }

    private fun buildPlaylist(root: LinearLayout, inlineScene: String) {
        root.addView(heroCard("Playlists", "Import, reorder, and resume playlists with lightweight business-page ad slots inline."))
        root.addView(sectionLabel("Your collections"))
        root.addView(songCard("Road Trip 2026", "43 songs", "Updated today"))
        root.addView(songCard("Gym Power", "29 songs", "Downloaded"))
        appendInlinePlacement(root, inlineScene, "Playlist spotlight")
        root.addView(sectionLabel("Continue editing"))
        root.addView(songCard("Lo-fi Coding", "15 songs", "Tap to manage"))
    }

    private fun buildDownload(root: LinearLayout, inlineScene: String) {
        root.addView(heroCard("Offline downloads", "Manage saved songs, storage health, and download progress in one place."))
        root.addView(sectionLabel("Storage"))
        root.addView(statCard("Used offline space", "1.8 GB of 8 GB"))
        appendInlinePlacement(root, inlineScene, "Sponsored install banner")
        root.addView(sectionLabel("Saved songs"))
        root.addView(songCard("Headlights", "Completed", "4:01"))
        root.addView(songCard("Echo City", "Queued", "Waiting for Wi-Fi"))
    }

    private fun buildPlayer(root: LinearLayout, inlineScene: String) {
        root.addView(heroCard("Now playing", "Keep listening while monetization falls through the same business-entry chain as the original app."))
        root.addView(controlRow())
        appendInlinePlacement(root, inlineScene, "Up next sponsor")
        root.addView(sectionLabel("Queue"))
        root.addView(songCard("Electric Dawn", "Next in queue", "3:47"))
        root.addView(songCard("Heartline", "Autoplay", "4:12"))
    }

    private fun appendInlinePlacement(root: LinearLayout, scene: String, label: String) {
        if (scene.isBlank()) return
        root.addView(sectionLabel(label))
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = roundedBackground("#FFFFFF", "#DCE5F0")
        }
        root.addView(container, matchWidthParams())
        loadInlineAd(scene, container)
    }

    private fun loadInlineAd(scene: String, container: LinearLayout) {
        val units = ServiceLocator.adConfigRepository.unitsFor(scene)
        if (units.isEmpty()) {
            container.addView(infoText("No inline placement config for $scene"))
            return
        }
        val unit = ServiceLocator.weightedSelector.select(scene, units)
        val testPlacementId = RealGoogleMobileAdsAdapter.testPlacementIdForType(unit.adtype)
        Log.d(TAG, "inline scene=$scene source=${unit.adsource} type=${unit.adtype} production=${unit.placementid} test=$testPlacementId")
        container.addView(infoText("scene=$scene  source=${unit.adsource}  type=${unit.adtype}"))
        when (unit.adtype.lowercase()) {
            "banner" -> attachBannerAd(container, testPlacementId)
            else -> attachNativeAd(container, testPlacementId)
        }
    }

    private fun attachBannerAd(container: LinearLayout, testAdUnitId: String) {
        val adView = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = testAdUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "banner loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "banner failed: ${error.message}")
                    container.addView(infoText("Banner failed: ${error.message}"))
                }
            }
        }
        bannerAdView?.destroy()
        bannerAdView = adView
        container.addView(FrameLayout(this).apply {
            foregroundGravity = Gravity.CENTER
            addView(adView)
        }, matchWidthParams())
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun attachNativeAd(container: LinearLayout, adUnitId: String) {
        val slot = FrameLayout(this).apply {
            addView(infoText(getString(R.string.business_inline_loading)))
        }
        container.addView(slot, matchWidthParams())
        AdLoader.Builder(this, adUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                slot.removeAllViews()
                slot.addView(renderNativeAd(ad))
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "native failed: ${error.message}")
                    slot.removeAllViews()
                    slot.addView(infoText("Native failed: ${error.message}"))
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    private fun renderNativeAd(ad: NativeAd): NativeAdView {
        val nativeAdView = NativeAdView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBackground("#FFFFFF", "#DCE5F0")
        }
        val badge = TextView(this).apply {
            text = "Ad"
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#17324D"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = roundedBackground("#CBE6FF", "#CBE6FF")
        }
        val headline = TextView(this).apply {
            text = ad.headline
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#101828"))
        }
        val body = TextView(this).apply {
            text = ad.body ?: "Suggested for you"
            textSize = 14f
            setTextColor(Color.parseColor("#475467"))
        }
        val mediaView = MediaView(this)
        val iconHolder = TextView(this).apply {
            text = ad.advertiser ?: "Promoted"
            textSize = 13f
            setTextColor(Color.parseColor("#667085"))
        }
        val cta = Button(this).apply {
            text = ad.callToAction ?: "Install"
            setTextColor(Color.parseColor("#042F2E"))
            background = roundedBackground("#5AEEEE", "#5AEEEE")
        }
        content.addView(badge)
        content.addView(space(10))
        content.addView(headline)
        content.addView(space(6))
        content.addView(body)
        content.addView(space(12))
        content.addView(mediaView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(180)))
        content.addView(space(12))
        content.addView(iconHolder)
        content.addView(space(12))
        content.addView(cta, matchWidthParams())
        nativeAdView.addView(content)
        nativeAdView.headlineView = headline
        nativeAdView.bodyView = body
        nativeAdView.mediaView = mediaView
        nativeAdView.callToActionView = cta
        nativeAdView.advertiserView = iconHolder
        nativeAdView.setNativeAd(ad)
        return nativeAdView
    }

    private fun pageHeader(title: String, pageKind: PageKind): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(10))
            addView(TextView(this@BusinessPageActivity).apply {
                text = title
                textSize = 28f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
            })
            addView(TextView(this@BusinessPageActivity).apply {
                text = pageKind.label
                textSize = 14f
                setTextColor(Color.parseColor("#667085"))
            })
        }
    }

    private fun metaStrip(triggerScene: String, inlineScene: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBackground("#EAF2FF", "#D5E2F7")
            addView(infoText("fullscreen trigger: $triggerScene"))
            if (inlineScene.isNotBlank()) {
                addView(infoText("inline placement: $inlineScene"))
            }
        }
    }

    private fun heroCard(title: String, subtitle: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBackground("#111827", "#111827")
            addView(TextView(this@BusinessPageActivity).apply {
                text = title
                textSize = 22f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            addView(space(8))
            addView(TextView(this@BusinessPageActivity).apply {
                text = subtitle
                textSize = 14f
                setTextColor(Color.parseColor("#D1D5DB"))
            })
        }
    }

    private fun searchBox(hint: String): View {
        return TextView(this).apply {
            text = hint
            textSize = 15f
            setTextColor(Color.parseColor("#98A2B3"))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBackground("#FFFFFF", "#D0D5DD")
        }
    }

    private fun controlRow(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(14), dp(8), dp(14))
            background = roundedBackground("#FFFFFF", "#DCE5F0")
            addView(controlButton("Prev"))
            addView(controlButton("Pause"))
            addView(controlButton("Next"))
        }
    }

    private fun controlButton(label: String): View {
        return Button(this).apply {
            text = label
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        }
    }

    private fun statCard(title: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBackground("#FFFFFF", "#DCE5F0")
            addView(TextView(this@BusinessPageActivity).apply {
                text = title
                textSize = 13f
                setTextColor(Color.parseColor("#667085"))
            })
            addView(space(6))
            addView(TextView(this@BusinessPageActivity).apply {
                text = value
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#101828"))
            })
        }
    }

    private fun songCard(title: String, subtitle: String, meta: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBackground("#FFFFFF", "#DCE5F0")
            addView(TextView(this@BusinessPageActivity).apply {
                text = title
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#101828"))
            })
            addView(space(4))
            addView(TextView(this@BusinessPageActivity).apply {
                text = subtitle
                textSize = 14f
                setTextColor(Color.parseColor("#475467"))
            })
            addView(space(2))
            addView(TextView(this@BusinessPageActivity).apply {
                text = meta
                textSize = 12f
                setTextColor(Color.parseColor("#98A2B3"))
            })
        }.also {
            val params = matchWidthParams()
            params.topMargin = dp(10)
            it.layoutParams = params
        }
    }

    private fun sectionLabel(text: String): View {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#344054"))
            setPadding(dp(2), dp(18), dp(2), dp(8))
        }
    }

    private fun infoText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.parseColor("#475467"))
        }
    }

    private fun matchWidthParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun roundedBackground(fillColor: String, strokeColor: String) =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(Color.parseColor(fillColor))
            setStroke(dp(1), Color.parseColor(strokeColor))
        }

    private fun space(dpValue: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(dpValue)
            )
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        nativeAd?.destroy()
        bannerAdView?.destroy()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BusinessPage"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SCENE = "scene"
        const val EXTRA_PAGE_KIND = "page_kind"
        const val EXTRA_INLINE_SCENE = "inline_scene"
    }

    enum class PageKind(val label: String) {
        HOME("Discover"),
        LIKE("Favorites"),
        SEARCH("Search"),
        PLAYLIST("Playlists"),
        DOWNLOAD("Offline"),
        PLAYER("Player")
    }
}
