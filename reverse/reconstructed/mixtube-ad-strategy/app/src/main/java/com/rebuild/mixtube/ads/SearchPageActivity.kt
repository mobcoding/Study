package com.rebuild.mixtube.ads

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SearchPageActivity : AppCompatActivity() {
    private lateinit var historyLayout: LinearLayout
    private lateinit var resultLayout: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var clearButton: TextView
    private lateinit var historyChips: LinearLayout
    private lateinit var musicTabView: TextView
    private lateinit var youtubeTabView: TextView
    private lateinit var musicIndicator: View
    private lateinit var youtubeIndicator: View
    private lateinit var musicResults: LinearLayout
    private lateinit var youtubeResults: LinearLayout
    private lateinit var historyAdRenderer: InlineAdRenderer
    private lateinit var resultAdRenderer: InlineAdRenderer
    private lateinit var inlineScene: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inlineScene = intent.getStringExtra(BusinessPageActivity.EXTRA_INLINE_SCENE).orEmpty()
        historyAdRenderer = InlineAdRenderer(this)
        resultAdRenderer = InlineAdRenderer(this)
        setContentView(buildContent())
        bindInput()
        showHistory()
    }

    override fun onDestroy() {
        historyAdRenderer.destroy()
        resultAdRenderer.destroy()
        super.onDestroy()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#11161E"))
            setPadding(dp(20), dp(24), dp(20), dp(16))
        }
        root.addView(searchBar())
        historyLayout = historyPanel()
        resultLayout = resultPanel()
        root.addView(historyLayout, matchParams())
        root.addView(resultLayout, matchParams().apply { topMargin = dp(8) })
        return ScrollView(this).apply { addView(root) }
    }

    private fun bindInput() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearButton.visibility = if (s.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun searchBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(this@SearchPageActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, dp(32), 1f)
                background = roundedBackground("#26FFFFFF", "#33FFFFFF", 8)
                setPadding(dp(16), 0, dp(8), 0)
                addView(TextView(this@SearchPageActivity).apply {
                    text = "S"
                    textSize = 14f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                addView(horizontalSpace(16))
                addView(View(this@SearchPageActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(1), dp(16))
                    background = GradientDrawable().apply { setColor(Color.parseColor("#33FFFFFF")) }
                })
                addView(horizontalSpace(16))
                searchInput = EditText(this@SearchPageActivity).apply {
                    hint = "Search songs, artists, albums"
                    setHintTextColor(Color.parseColor("#99FFFFFF"))
                    setTextColor(Color.WHITE)
                    background = null
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setSingleLine()
                    setOnEditorActionListener { _, _, _ ->
                        showResults(text?.toString().orEmpty().ifBlank { "Moonlight Echo" })
                        true
                    }
                }
                addView(searchInput)
                clearButton = TextView(this@SearchPageActivity).apply {
                    text = "X"
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                    visibility = View.GONE
                    setPadding(dp(8), 0, dp(8), 0)
                    setOnClickListener {
                        searchInput.setText("")
                        showHistory()
                    }
                }
                addView(clearButton)
            })
            addView(TextView(this@SearchPageActivity).apply {
                text = "Cancel"
                textSize = 14f
                setTextColor(Color.parseColor("#99FFFFFF"))
                setPadding(dp(16), 0, 0, 0)
                setOnClickListener { finish() }
            })
        }
    }

    private fun historyPanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(LinearLayout(this@SearchPageActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(20)
                }
                addView(TextView(this@SearchPageActivity).apply {
                    text = "Search history"
                    textSize = 18f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(this@SearchPageActivity).apply {
                    text = "Delete"
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                    setOnClickListener {
                        historyChips.removeAllViews()
                        historyChips.addView(chip("No recent searches"))
                    }
                })
            })
            historyChips = LinearLayout(this@SearchPageActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                }
                addView(chip("Night Drive"))
                addView(chip("Workout", dp(10)))
                addView(chip("Playlist", dp(10)))
            }
            addView(historyChips)
            val historyAdSlot = FrameLayout(this@SearchPageActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(20)
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            addView(historyAdSlot)
            historyAdRenderer.load(inlineScene, historyAdSlot, InlineAdRenderer.Style.LARGE_NATIVE)
            addView(sectionTitle("Suggestions"))
            addView(historyItem("Moonlight Echo"))
            addView(historyItem("Late Night Drive"))
            addView(historyItem("Lo-fi Coding"))
        }
    }

    private fun resultPanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            addView(resultTabs())
            addView(categoryStrip())
            val nativeSlot = FrameLayout(this@SearchPageActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
                setPadding(0, dp(8), 0, 0)
            }
            addView(nativeSlot)
            resultAdRenderer.load(inlineScene, nativeSlot, InlineAdRenderer.Style.SEARCH_NATIVE)
            musicResults = LinearLayout(this@SearchPageActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(resultItem("Moonlight Echo", "Song", "3:21"))
                addView(resultItem("Moonlight Echo Playlist", "Playlist", "18 tracks"))
                addView(resultItem("Late Night Drive", "Artist mix", "New update"))
            }
            addView(musicResults)
            youtubeResults = LinearLayout(this@SearchPageActivity).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                addView(resultItem("Moonlight Echo Live", "YouTube video", "12:45"))
                addView(resultItem("Night Runner MV", "YouTube video", "4:20"))
                addView(resultItem("Mixtube Session", "Channel", "Updated today"))
            }
            addView(youtubeResults)
        }
    }

    private fun resultTabs(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val musicTab = topTab("Music") { showResultTab(true) }
            musicTabView = musicTab.first
            musicIndicator = musicTab.second
            addView(musicTab.third)
            val youtubeTab = topTab("YouTube") { showResultTab(false) }
            youtubeTabView = youtubeTab.first
            youtubeIndicator = youtubeTab.second
            addView(youtubeTab.third)
        }
    }

    private fun topTab(text: String, onClick: () -> Unit): Triple<TextView, View, View> {
        val label = TextView(this).apply {
            this.text = text
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#99FFFFFF"))
        }
        val indicator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(16), dp(2)).apply {
                topMargin = dp(8)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = roundedBackground("#D73F36", "#D73F36", 2)
            alpha = 0f
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { onClick() }
            addView(label)
            addView(indicator)
        }
        return Triple(label, indicator, container)
    }

    private fun categoryStrip(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
            addView(chip("Top result"))
            addView(chip("Playlist", dp(10)))
            addView(chip("Artist", dp(10)))
        }
    }

    private fun historyItem(text: String): View {
        return resultRow(text, "Tap to search again", "") {
            BusinessFlowNavigator.open(
                activity = this@SearchPageActivity,
                title = text,
                event = ProductEvent.OtherSearch
            )
        }
    }

    private fun resultItem(title: String, subtitle: String, meta: String): View {
        val event = when {
            subtitle.contains("Playlist", ignoreCase = true) -> ProductEvent.OtherPlaylist
            subtitle.contains("YouTube", ignoreCase = true) -> ProductEvent.OtherSearch
            else -> ProductEvent.PlayStart
        }
        return resultRow(title, subtitle, meta) {
            BusinessFlowNavigator.open(
                activity = this@SearchPageActivity,
                title = title,
                event = event
            )
        }
    }

    private fun resultRow(title: String, subtitle: String, meta: String, onClick: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBackground("#1F252E", "#263241", 10)
            setOnClickListener { onClick() }
            addView(View(this@SearchPageActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
                background = roundedBackground("#303A46", "#303A46", 6)
            })
            addView(LinearLayout(this@SearchPageActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(14)
                }
                addView(TextView(this@SearchPageActivity).apply {
                    text = title
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                addView(TextView(this@SearchPageActivity).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                })
            })
            if (meta.isNotBlank()) {
                addView(TextView(this@SearchPageActivity).apply {
                    text = meta
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                })
            }
        }
    }

    private fun chip(text: String, marginStart: Int = 0): View {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedBackground("#1F252E", "#263241", 20)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if (marginStart > 0) this.marginStart = marginStart
            }
        }
    }

    private fun sectionTitle(text: String): View {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
        }
    }

    private fun showHistory() {
        historyLayout.visibility = View.VISIBLE
        resultLayout.visibility = View.GONE
        clearButton.visibility = if (searchInput.text.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun showResults(query: String) {
        searchInput.setText(query)
        searchInput.setSelection(query.length)
        historyLayout.visibility = View.GONE
        resultLayout.visibility = View.VISIBLE
        clearButton.visibility = View.VISIBLE
        showResultTab(true)
    }

    private fun showResultTab(showMusic: Boolean) {
        musicResults.visibility = if (showMusic) View.VISIBLE else View.GONE
        youtubeResults.visibility = if (showMusic) View.GONE else View.VISIBLE
        musicTabView.setTextColor(Color.parseColor(if (showMusic) "#D73F36" else "#99FFFFFF"))
        youtubeTabView.setTextColor(Color.parseColor(if (showMusic) "#99FFFFFF" else "#D73F36"))
        musicIndicator.alpha = if (showMusic) 1f else 0f
        youtubeIndicator.alpha = if (showMusic) 0f else 1f
    }

    private fun roundedBackground(fill: String, stroke: String, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(radiusDp).toFloat()
            setColor(Color.parseColor(fill))
            setStroke(dp(1), Color.parseColor(stroke))
        }
    }

    private fun matchParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun horizontalSpace(size: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(size), 1)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
