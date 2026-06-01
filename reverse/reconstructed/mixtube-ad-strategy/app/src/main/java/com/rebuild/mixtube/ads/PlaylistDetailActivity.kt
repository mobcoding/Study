package com.rebuild.mixtube.ads

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlaylistDetailActivity : AppCompatActivity() {
    private lateinit var pageKind: BusinessPageActivity.PageKind

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageKind = intent.getStringExtra(BusinessPageActivity.EXTRA_PAGE_KIND)
            ?.let(BusinessPageActivity.PageKind::valueOf)
            ?: BusinessPageActivity.PageKind.PLAYLIST
        setContentView(buildContent(intent.getStringExtra(BusinessPageActivity.EXTRA_TITLE).orEmpty()))
    }

    private fun buildContent(title: String): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#11161E"))
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        content.addView(header(title))
        if (pageKind == BusinessPageActivity.PageKind.LIKE) {
            content.addView(downloadStrip())
        }
        content.addView(heroBlock(title))
        content.addView(listHeader())
        repeat(7) { index ->
            content.addView(trackRow(index))
        }
        content.addView(emptyStateHint())
        return ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#11161E"))
            addView(content)
        }
    }

    private fun header(title: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(headerButton("Back") { finish() })
            addView(TextView(this@PlaylistDetailActivity).apply {
                text = title.ifBlank { pageTitle() }
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(10)
                    marginEnd = dp(10)
                }
            })
            addView(headerButton(if (pageKind == BusinessPageActivity.PageKind.PLAYLIST) "More" else "") { })
        }
    }

    private fun downloadStrip(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40)
            ).apply {
                topMargin = dp(12)
            }
            setPadding(dp(16), 0, dp(16), 0)
            background = roundedBackground("#3C2418", "#5C3826", 10)
            addView(TextView(this@PlaylistDetailActivity).apply {
                text = "Saving songs in queue"
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#FF7F45"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@PlaylistDetailActivity).apply {
                text = "Manage"
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#FF7F45"))
                setPadding(dp(8), dp(6), dp(8), dp(6))
                background = roundedBackground("#33FF7F45", "#55FF7F45", 6)
                setOnClickListener {
                    ServiceLocator.markQueuedDownloads(true)
                    startActivity(android.content.Intent(this@PlaylistDetailActivity, DownloadManageActivity::class.java))
                }
            })
        }
    }

    private fun heroBlock(title: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
            addView(LinearLayout(this@PlaylistDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(LinearLayout(this@PlaylistDetailActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(136), dp(136))
                    background = roundedBackground("#262626", "#303A46", 8)
                    gravity = Gravity.TOP or Gravity.START
                    addView(TextView(this@PlaylistDetailActivity).apply {
                        text = songCountText()
                        textSize = 11f
                        setTextColor(Color.WHITE)
                        setPadding(dp(8), dp(4), dp(8), dp(4))
                        background = roundedBackground("#80262626", "#80262626", 12)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dp(10)
                            marginStart = dp(10)
                        }
                    })
                })
                addView(LinearLayout(this@PlaylistDetailActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.BOTTOM
                    layoutParams = LinearLayout.LayoutParams(0, dp(136), 1f).apply {
                        marginStart = dp(20)
                    }
                    addView(TextView(this@PlaylistDetailActivity).apply {
                        text = title.ifBlank { pageTitle() }
                        textSize = 24f
                        setTypeface(typeface, Typeface.BOLD)
                        setTextColor(Color.WHITE)
                        maxLines = 3
                    })
                    addView(space(10))
                    addView(actionButtons())
                })
            })
            if (pageKind == BusinessPageActivity.PageKind.LIKE) {
                addView(localCacheTabs())
            }
        }
    }

    private fun actionButtons(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            when (pageKind) {
                BusinessPageActivity.PageKind.LIKE -> {
                    addView(actionChip("Collected", accent = false) {
                        BusinessFlowNavigator.open(this@PlaylistDetailActivity, "Liked songs", ProductEvent.OtherLike)
                    })
                    addView(actionChip("Add songs", accent = false, marginStart = 10) {
                        BusinessFlowNavigator.openEventToDestination(
                            activity = this@PlaylistDetailActivity,
                            event = ProductEvent.OtherPlaylist,
                            destination = android.content.Intent(this@PlaylistDetailActivity, PlaylistAddSongsActivity::class.java)
                        )
                    })
                }
                else -> {
                    addView(actionChip("Like", accent = true) {
                        BusinessFlowNavigator.open(this@PlaylistDetailActivity, "Liked songs", ProductEvent.OtherLike)
                    })
                    addView(actionChip("Refresh", accent = false, marginStart = 10) {
                        BusinessFlowNavigator.openEventToDestination(
                            activity = this@PlaylistDetailActivity,
                            event = ProductEvent.OtherPlaylist,
                            destination = android.content.Intent(this@PlaylistDetailActivity, PlaylistAddSongsActivity::class.java)
                        )
                    })
                }
            }
        }
    }

    private fun localCacheTabs(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
            addView(cacheTab("All", true))
            addView(cacheTab("Offline", false))
            addView(cacheTab("Local", false))
        }
    }

    private fun cacheTab(text: String, selected: Boolean): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(20)
            }
            addView(TextView(this@PlaylistDetailActivity).apply {
                this.text = text
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor(if (selected) "#D73F36" else "#99FFFFFF"))
            })
            addView(View(this@PlaylistDetailActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(12), dp(2)).apply {
                    topMargin = dp(6)
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                background = roundedBackground("#D73F36", "#D73F36", 2)
                alpha = if (selected) 1f else 0f
            })
        }
    }

    private fun listHeader(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(24)
            }
            addView(TextView(this@PlaylistDetailActivity).apply {
                text = "Play all"
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = roundedBackground("#26FFFFFF", "#33FFFFFF", 18)
            })
            addView(TextView(this@PlaylistDetailActivity).apply {
                text = if (pageKind == BusinessPageActivity.PageKind.LIKE) "Liked collection" else "Playlist queue"
                textSize = 12f
                setTextColor(Color.parseColor("#99FFFFFF"))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(16)
                }
            })
        }
    }

    private fun trackRow(index: Int): View {
        val title = when (pageKind) {
            BusinessPageActivity.PageKind.LIKE -> "Liked track ${index + 1}"
            else -> "Playlist item ${index + 1}"
        }
        val subtitle = when (pageKind) {
            BusinessPageActivity.PageKind.LIKE -> "Artist route 3:${40 + index}"
            else -> "Playlist detail route 3:${40 + index}"
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
            setOnClickListener {
                BusinessFlowNavigator.open(
                    activity = this@PlaylistDetailActivity,
                    title = title,
                    event = ProductEvent.PlayStart
                )
            }
            addView(View(this@PlaylistDetailActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
                background = roundedBackground("#262626", "#303A46", 6)
            })
            addView(LinearLayout(this@PlaylistDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(16)
                }
                addView(TextView(this@PlaylistDetailActivity).apply {
                    text = title
                    textSize = 14f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                addView(TextView(this@PlaylistDetailActivity).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                })
            })
            addView(TextView(this@PlaylistDetailActivity).apply {
                text = durationLabel(index)
                textSize = 12f
                setTextColor(Color.parseColor("#99FFFFFF"))
            })
            addView(TextView(this@PlaylistDetailActivity).apply {
                text = "..."
                textSize = 16f
                setTextColor(Color.parseColor("#99FFFFFF"))
                setPadding(dp(12), 0, 0, 0)
            })
        }
    }

    private fun emptyStateHint(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(28)
            }
            addView(TextView(this@PlaylistDetailActivity).apply {
                text = "Need a faster refill?"
                textSize = 13f
                setTextColor(Color.parseColor("#99FFFFFF"))
            })
            addView(TextView(this@PlaylistDetailActivity).apply {
                text = if (pageKind == BusinessPageActivity.PageKind.LIKE) "Add songs" else "Refresh playlist"
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#D73F36"))
                setPadding(dp(24), dp(12), dp(24), dp(12))
                background = roundedBackground("#33D73F36", "#55D73F36", 18)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(14)
                }
            })
        }
    }

    private fun actionChip(text: String, accent: Boolean, marginStart: Int = 0, onClick: () -> Unit): View {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(if (accent) Color.parseColor("#D73F36") else Color.WHITE)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = roundedBackground(
                if (accent) "#40D73F36" else "#262626",
                if (accent) "#D73F36" else "#303A46",
                6
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if (marginStart > 0) this.marginStart = dp(marginStart)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun headerButton(text: String, onClick: () -> Unit): View {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.WHITE)
            minWidth = dp(40)
            setOnClickListener { onClick() }
        }
    }

    private fun pageTitle(): String {
        return when (pageKind) {
            BusinessPageActivity.PageKind.LIKE -> "Liked songs"
            else -> "Playlist"
        }
    }

    private fun songCountText(): String {
        return when (pageKind) {
            BusinessPageActivity.PageKind.LIKE -> "124 songs"
            else -> "43 songs"
        }
    }

    private fun durationLabel(index: Int): String = "3:${10 + index}"

    private fun roundedBackground(fill: String, stroke: String, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(radiusDp).toFloat()
            setColor(Color.parseColor(fill))
            setStroke(dp(1), Color.parseColor(stroke))
        }
    }

    private fun space(dpValue: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(dpValue)
            )
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
