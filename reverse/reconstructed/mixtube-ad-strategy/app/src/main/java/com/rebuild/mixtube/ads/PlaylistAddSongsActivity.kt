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

class PlaylistAddSongsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#11161E"))
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        content.addView(header())
        content.addView(sourceRow("My saved playlist", "124 songs") {
            BusinessFlowNavigator.open(this@PlaylistAddSongsActivity, "Liked songs", ProductEvent.OtherLike)
        })
        content.addView(sourceRow("Local cache", "18 songs") {
            BusinessFlowNavigator.open(this@PlaylistAddSongsActivity, "Downloading", ProductEvent.OtherDownload)
        })
        content.addView(section("Add playlist"))
        content.addView(playlistCard("Road Trip 2026", "43 songs") {
            BusinessFlowNavigator.open(this@PlaylistAddSongsActivity, "Road Trip 2026", ProductEvent.OtherPlaylist)
        })
        content.addView(playlistCard("Weekend Replay", "24 songs") {
            BusinessFlowNavigator.open(this@PlaylistAddSongsActivity, "Weekend Replay", ProductEvent.OtherPlaylist)
        })
        content.addView(section("Liked playlist"))
        content.addView(playlistCard("Mixtube Favorites", "36 songs") {
            BusinessFlowNavigator.open(this@PlaylistAddSongsActivity, "Mixtube Favorites", ProductEvent.OtherPlaylist)
        })
        return ScrollView(this).apply { addView(content) }
    }

    private fun header(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(headerButton("Back") { finish() })
            addView(TextView(this@PlaylistAddSongsActivity).apply {
                text = "Add songs to playlist"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(headerButton("", onClick = { }))
        }
    }

    private fun sourceRow(title: String, subtitle: String, onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
            setOnClickListener { onClick() }
            addView(View(this@PlaylistAddSongsActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
                background = roundedBackground("#D73F36", "#D73F36", 10)
            })
            addView(LinearLayout(this@PlaylistAddSongsActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(16)
                }
                addView(TextView(this@PlaylistAddSongsActivity).apply {
                    text = title
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                addView(TextView(this@PlaylistAddSongsActivity).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(6)
                    }
                })
            })
            addView(TextView(this@PlaylistAddSongsActivity).apply {
                text = ">"
                textSize = 14f
                setTextColor(Color.parseColor("#99FFFFFF"))
            })
        }
    }

    private fun section(text: String): View {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(28)
            }
        }
    }

    private fun playlistCard(title: String, subtitle: String, onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
            background = roundedBackground("#26FFFFFF", "#33FFFFFF", 10)
            setOnClickListener { onClick() }
            addView(View(this@PlaylistAddSongsActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
                background = roundedBackground("#303A46", "#303A46", 6)
            })
            addView(LinearLayout(this@PlaylistAddSongsActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(14)
                }
                addView(TextView(this@PlaylistAddSongsActivity).apply {
                    text = title
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                addView(TextView(this@PlaylistAddSongsActivity).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                })
            })
        }
    }

    private fun headerButton(text: String, onClick: () -> Unit): View {
        return TextView(this).apply {
            this.text = text
            minWidth = dp(40)
            textSize = 13f
            setTextColor(Color.WHITE)
            setOnClickListener { onClick() }
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
}
