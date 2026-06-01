package com.rebuild.mixtube.ads

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlayerPageActivity : AppCompatActivity() {
    private lateinit var overlayRenderer: InlineAdRenderer
    private lateinit var queueRenderer: InlineAdRenderer
    private lateinit var inlineScene: String
    private lateinit var playlistTabView: TextView
    private lateinit var lyricTabView: TextView
    private lateinit var relatedTabView: TextView
    private lateinit var playlistIndicator: View
    private lateinit var lyricIndicator: View
    private lateinit var relatedIndicator: View
    private lateinit var playHeadLayout: LinearLayout
    private lateinit var playlistContent: LinearLayout
    private lateinit var lyricContent: TextView
    private lateinit var relatedContent: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inlineScene = intent.getStringExtra(BusinessPageActivity.EXTRA_INLINE_SCENE).orEmpty()
        overlayRenderer = InlineAdRenderer(this)
        queueRenderer = InlineAdRenderer(this)
        setContentView(buildContent())
        showBottomTab(BottomTab.PLAYLIST)
    }

    override fun onDestroy() {
        overlayRenderer.destroy()
        queueRenderer.destroy()
        super.onDestroy()
    }

    private fun buildContent(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#11161E"))
            setPadding(dp(20), dp(24), dp(20), dp(0))
        }
        content.addView(header())
        content.addView(playerShell())
        content.addView(trackMeta())
        content.addView(progressBlock())
        content.addView(controlBlock())
        content.addView(bottomSheetPanel())
        return ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#11161E"))
            addView(content)
        }
    }

    private fun header(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(headerButton("Back") { finish() })
            addView(TextView(this@PlayerPageActivity).apply {
                text = "Now playing"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(20)
                    marginEnd = dp(20)
                }
            })
            addView(headerButton("More") { })
        }
    }

    private fun playerShell(): View {
        val shell = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(250)
            ).apply {
                topMargin = dp(20)
            }
            background = roundedBackground("#1A2029", "#303A46", 12)
        }
        shell.addView(TextView(this).apply {
            text = "Video surface"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#66FFFFFF"))
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        })
        val overlaySlot = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                dp(300),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
        shell.addView(overlaySlot)
        overlayRenderer.load(inlineScene, overlaySlot, InlineAdRenderer.Style.PLAYER_OVERLAY)
        return shell
    }

    private fun trackMeta(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(28)
            }
            addView(circleIcon("Like"))
            addView(LinearLayout(this@PlayerPageActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@PlayerPageActivity).apply {
                    text = "Late Night Drive"
                    textSize = 24f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                })
                addView(TextView(this@PlayerPageActivity).apply {
                    text = "Mixtube Originals"
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                    gravity = Gravity.CENTER
                })
            })
            addView(circleIcon("Save"))
        }
    }

    private fun progressBlock(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(24)
            }
            addView(SeekBar(this@PlayerPageActivity).apply {
                max = 100
                progress = 28
                isEnabled = false
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            })
            addView(LinearLayout(this@PlayerPageActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
                addView(timeLabel("00:42"))
                addView(TextView(this@PlayerPageActivity).apply {
                    text = "03:47"
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
            })
        }
    }

    private fun controlBlock(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(26)
            }
            addView(control("Shuffle"))
            addView(control("Prev"))
            addView(control("Pause", accent = true))
            addView(control("Next"))
            addView(control("Loop"))
        }
    }

    private fun bottomSheetPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(24)
            }
            setPadding(dp(24), dp(8), dp(24), dp(20))
            background = roundedBackground("#262626", "#303A46", 20)
        }
        panel.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = roundedBackground("#40FFFFFF", "#40FFFFFF", 4)
        })
        panel.addView(bottomTabs())
        panel.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(18)
            }
            background = roundedBackground("#26FFFFFF", "#26FFFFFF", 1)
        })

        playHeadLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
            addView(TextView(this@PlayerPageActivity).apply {
                text = "Play all"
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = roundedBackground("#40FFFFFF", "#40FFFFFF", 18)
            })
            addView(TextView(this@PlayerPageActivity).apply {
                text = "36 songs"
                textSize = 12f
                setTextColor(Color.parseColor("#99FFFFFF"))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(16)
                }
            })
        }
        panel.addView(playHeadLayout)

        val adContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
        }
        panel.addView(adContainer)
        queueRenderer.load(inlineScene, adContainer, InlineAdRenderer.Style.QUEUE_PANEL)

        playlistContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
            addView(queueItem("Electric Dawn", "Next in queue"))
            addView(queueItem("Heartline", "Autoplay"))
            addView(queueItem("Moonlight Echo", "Suggested"))
            addView(queueItem("No Signal", "Recently played"))
        }
        panel.addView(playlistContent)

        lyricContent = TextView(this).apply {
            text = "The highway fades into the midnight haze\nKeep the engine warm and let the city glow"
            textSize = 14f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
        }
        panel.addView(lyricContent)

        relatedContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
            addView(queueItem("Night Runner", "Related single"))
            addView(queueItem("Afterglow", "From the same artist"))
            addView(queueItem("City Lights", "Fans also played"))
        }
        panel.addView(relatedContent)
        return panel
    }

    private fun bottomTabs(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
            val playlistTab = tabColumn("Playlist", BottomTab.PLAYLIST)
            playlistTabView = playlistTab.label
            playlistIndicator = playlistTab.indicator
            addView(playlistTab.root)

            val lyricTab = tabColumn("Lyric", BottomTab.LYRIC)
            lyricTabView = lyricTab.label
            lyricIndicator = lyricTab.indicator
            addView(lyricTab.root)

            val relatedTab = tabColumn("Related", BottomTab.RELATED)
            relatedTabView = relatedTab.label
            relatedIndicator = relatedTab.indicator
            addView(relatedTab.root)
        }
    }

    private fun tabColumn(text: String, tab: BottomTab): BottomTabViews {
        val label = TextView(this).apply {
            this.text = text
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#80FFFFFF"))
        }
        val indicator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(2)).apply {
                topMargin = dp(10)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = roundedBackground("#FFFFFFFF", "#FFFFFFFF", 2)
            alpha = 0f
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showBottomTab(tab) }
            addView(label)
            addView(indicator)
        }
        return BottomTabViews(root = column, label = label, indicator = indicator)
    }

    private fun showBottomTab(tab: BottomTab) {
        updateBottomTab(playlistTabView, playlistIndicator, tab == BottomTab.PLAYLIST)
        updateBottomTab(lyricTabView, lyricIndicator, tab == BottomTab.LYRIC)
        updateBottomTab(relatedTabView, relatedIndicator, tab == BottomTab.RELATED)
        playHeadLayout.visibility = if (tab == BottomTab.PLAYLIST) View.VISIBLE else View.GONE
        playlistContent.visibility = if (tab == BottomTab.PLAYLIST) View.VISIBLE else View.GONE
        lyricContent.visibility = if (tab == BottomTab.LYRIC) View.VISIBLE else View.GONE
        relatedContent.visibility = if (tab == BottomTab.RELATED) View.VISIBLE else View.GONE
    }

    private fun updateBottomTab(label: TextView, indicator: View, selected: Boolean) {
        label.setTextColor(Color.parseColor(if (selected) "#FFFFFFFF" else "#80FFFFFF"))
        indicator.alpha = if (selected) 1f else 0f
    }

    private fun queueItem(title: String, subtitle: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
            addView(View(this@PlayerPageActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                background = roundedBackground("#303A46", "#3F4A59", 6)
            })
            addView(LinearLayout(this@PlayerPageActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(14)
                }
                addView(TextView(this@PlayerPageActivity).apply {
                    text = title
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                addView(TextView(this@PlayerPageActivity).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                })
            })
            addView(TextView(this@PlayerPageActivity).apply {
                text = "..."
                textSize = 16f
                setTextColor(Color.parseColor("#99FFFFFF"))
            })
        }
    }

    private fun circleIcon(text: String): View {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            background = roundedBackground("#1F252E", "#303A46", 20)
        }
    }

    private fun headerButton(text: String, onClick: () -> Unit): View {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.WHITE)
            setOnClickListener { onClick() }
            minWidth = dp(44)
        }
    }

    private fun timeLabel(text: String): View {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.parseColor("#99FFFFFF"))
        }
    }

    private fun control(text: String, accent: Boolean = false): View {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(dp(4), dp(12), dp(4), dp(12))
            if (accent) {
                background = roundedBackground("#D73F36", "#D73F36", 20)
            }
        }
    }

    private fun roundedBackground(fill: String, stroke: String, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(radiusDp).toFloat()
            setColor(Color.parseColor(fill))
            setStroke(dp(1), Color.parseColor(stroke))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class BottomTab {
        PLAYLIST,
        LYRIC,
        RELATED
    }

    private data class BottomTabViews(
        val root: View,
        val label: TextView,
        val indicator: View
    )
}
