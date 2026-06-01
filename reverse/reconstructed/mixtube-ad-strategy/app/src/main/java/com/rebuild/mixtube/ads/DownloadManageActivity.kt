package com.rebuild.mixtube.ads

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DownloadManageActivity : AppCompatActivity() {
    private lateinit var listContent: LinearLayout
    private lateinit var emptyContent: LinearLayout
    private var isEmptyMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.markQueuedDownloads(true)
        setContentView(buildContent())
        showEmptyState(false)
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#11161E"))
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        root.addView(header())

        listContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(downloadRow("Moonlight Echo", "Waiting for Wi-Fi", 0, failed = false))
            addView(downloadRow("Late Night Drive", "Saving", 42, failed = false))
            addView(downloadRow("Electric Dawn", "Download failed", 76, failed = true))
            addView(downloadRow("Road Trip 2026", "Completed", 100, failed = false))
        }
        root.addView(listContent)

        emptyContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(120)
            }
            addView(TextView(this@DownloadManageActivity).apply {
                text = "No downloads yet"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            addView(TextView(this@DownloadManageActivity).apply {
                text = "Saved songs will appear here when caching starts."
                textSize = 12f
                setTextColor(Color.parseColor("#99FFFFFF"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(10)
                }
            })
            addView(TextView(this@DownloadManageActivity).apply {
                text = "Back to library"
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor("#D73F36"))
                setPadding(dp(24), dp(12), dp(24), dp(12))
                background = roundedBackground("#33D73F36", "#55D73F36", 18)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(24)
                }
            })
        }
        root.addView(emptyContent)
        return ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#11161E"))
            addView(root)
        }
    }

    private fun header(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(headerButton("Back") { finish() })
            addView(TextView(this@DownloadManageActivity).apply {
                text = "Downloading"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(10)
                    marginEnd = dp(10)
                }
            })
            addView(headerButton("Delete") {
                isEmptyMode = !isEmptyMode
                showEmptyState(isEmptyMode)
            })
        }
    }

    private fun showEmptyState(showEmpty: Boolean) {
        ServiceLocator.markQueuedDownloads(!showEmpty)
        listContent.visibility = if (showEmpty) View.GONE else View.VISIBLE
        emptyContent.visibility = if (showEmpty) View.VISIBLE else View.GONE
    }

    private fun downloadRow(title: String, subtitle: String, progress: Int, failed: Boolean): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72)
            ).apply {
                topMargin = dp(8)
            }
            setOnClickListener {
                BusinessFlowNavigator.open(
                    activity = this@DownloadManageActivity,
                    title = title,
                    event = ProductEvent.PlayStart
                )
            }
            addView(LinearLayout(this@DownloadManageActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@DownloadManageActivity).apply {
                    text = title
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    maxLines = 1
                })
                addView(LinearLayout(this@DownloadManageActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(12)
                    }
                    addView(TextView(this@DownloadManageActivity).apply {
                        text = subtitle
                        textSize = 12f
                        setTypeface(typeface, if (failed) Typeface.BOLD else Typeface.NORMAL)
                        setTextColor(Color.parseColor(if (failed) "#D73F36" else "#99FFFFFF"))
                    })
                    addView(ProgressBar(this@DownloadManageActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                        max = 100
                        this.progress = progress
                        layoutParams = LinearLayout.LayoutParams(0, dp(5), 1f).apply {
                            marginStart = dp(12)
                        }
                        progressDrawable = roundedProgress()
                    })
                })
            })
            addView(TextView(this@DownloadManageActivity).apply {
                text = "Delete"
                textSize = 12f
                setTextColor(Color.parseColor("#99FFFFFF"))
                setPadding(dp(12), dp(10), 0, dp(10))
                setOnClickListener { visibility = View.GONE }
            })
        }
    }

    private fun roundedProgress(): android.graphics.drawable.LayerDrawable {
        val background = GradientDrawable().apply {
            cornerRadius = dp(3).toFloat()
            setColor(Color.parseColor("#263241"))
        }
        val progress = GradientDrawable().apply {
            cornerRadius = dp(3).toFloat()
            setColor(Color.parseColor("#D73F36"))
        }
        return android.graphics.drawable.LayerDrawable(
            arrayOf(background, android.graphics.drawable.ScaleDrawable(progress, Gravity.START, 1f, -1f))
        ).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
    }

    private fun headerButton(text: String, onClick: () -> Unit): View {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.WHITE)
            minWidth = dp(48)
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
