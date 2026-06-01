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

class PlaylistImportActivity : AppCompatActivity() {
    private val playlistItems = mutableListOf(
        ImportItem("Workout 2026", 18, true),
        ImportItem("Late Night Drive", 32, true),
        ImportItem("Weekend Replay", 24, false),
        ImportItem("Focus Coding", 41, false)
    )
    private lateinit var selectAllView: TextView
    private lateinit var listContent: LinearLayout
    private var allSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        renderItems()
        updateSelectAll()
    }

    private fun buildContent(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#11161E"))
            setPadding(dp(20), dp(24), dp(20), dp(20))
        }
        content.addView(header())
        content.addView(divider(20))
        listContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }
        content.addView(listContent)
        content.addView(bottomBar())
        return ScrollView(this).apply { addView(content) }
    }

    private fun header(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(headerButton("Back") { finish() })
            addView(TextView(this@PlaylistImportActivity).apply {
                text = "Select playlist"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            selectAllView = TextView(this@PlaylistImportActivity).apply {
                textSize = 13f
                setTextColor(Color.WHITE)
                setOnClickListener {
                    allSelected = !allSelected
                    playlistItems.replaceAll { it.copy(selected = allSelected) }
                    renderItems()
                    updateSelectAll()
                }
            }
            addView(selectAllView)
        }
    }

    private fun renderItems() {
        listContent.removeAllViews()
        playlistItems.forEachIndexed { index, item ->
            listContent.addView(importRow(item).apply {
                if (index > 0) layoutParams = (layoutParams as LinearLayout.LayoutParams).apply {
                    topMargin = dp(10)
                }
            })
        }
    }

    private fun importRow(item: ImportItem): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBackground("#26FFFFFF", "#33FFFFFF", 10)
            setOnClickListener {
                val index = playlistItems.indexOfFirst { it.name == item.name }
                if (index >= 0) {
                    playlistItems[index] = playlistItems[index].copy(selected = !playlistItems[index].selected)
                    renderItems()
                    updateSelectAll()
                }
            }
            addView(selectionBadge(item.selected))
            addView(LinearLayout(this@PlaylistImportActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(14)
                }
                addView(TextView(this@PlaylistImportActivity).apply {
                    text = item.name
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                addView(TextView(this@PlaylistImportActivity).apply {
                    text = "${item.songCount} songs"
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                })
            })
        }
    }

    private fun selectionBadge(selected: Boolean): View {
        return TextView(this).apply {
            text = if (selected) "✓" else ""
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
            background = roundedBackground(
                if (selected) "#D73F36" else "#1F252E",
                if (selected) "#D73F36" else "#4B5563",
                12
            )
        }
    }

    private fun bottomBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
            addView(divider(0))
            addView(TextView(this@PlaylistImportActivity).apply {
                text = "Import"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = roundedBackground("#D73F36", "#D73F36", 10)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(44)
                ).apply {
                    topMargin = dp(12)
                }
                setOnClickListener {
                    BusinessFlowNavigator.open(
                        activity = this@PlaylistImportActivity,
                        title = "Imported playlist",
                        event = ProductEvent.OtherPlaylist
                    )
                }
            })
        }
    }

    private fun updateSelectAll() {
        allSelected = playlistItems.all { it.selected }
        selectAllView.text = if (allSelected) "Unselect all" else "Select all"
    }

    private fun headerButton(text: String, onClick: () -> Unit): View {
        return TextView(this).apply {
            this.text = text
            minWidth = dp(60)
            textSize = 13f
            setTextColor(Color.WHITE)
            setOnClickListener { onClick() }
        }
    }

    private fun divider(top: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(top)
            }
            background = roundedBackground("#14FFFFFF", "#14FFFFFF", 1)
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

    private data class ImportItem(
        val name: String,
        val songCount: Int,
        val selected: Boolean
    )
}
