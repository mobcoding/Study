package com.rebuild.mixtube.ads

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment

class LibraryTabFragment : Fragment() {
    private var adRenderer: InlineAdRenderer? = null

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        adRenderer = InlineAdRenderer(context)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#11161E"))
            }
        }
        content.addView(header("Library", "Collections"))
        content.addView(topEntranceRow())
        content.addView(cleanBoostRow())
        content.addView(youtubeImportRow())
        val pageSlot = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(20)
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
        }
        content.addView(pageSlot)
        adRenderer?.load(AdScene.PageBanner.key, pageSlot, InlineAdRenderer.Style.LARGE_NATIVE)
        content.addView(section("Created playlists"))
        content.addView(listRow("Road Trip 2026", "43 songs", "Updated today") {
            host()?.openBusiness(getString(R.string.action_playlist), ProductEvent.OpenPlaylist)
        })
        content.addView(listRow("Gym Power", "29 songs", "Downloaded") {
            host()?.openBusiness(getString(R.string.action_playlist), ProductEvent.OpenPlaylist)
        })
        content.addView(section("Saved playlists"))
        content.addView(listRow("Liked songs", "124 tracks", "Daily mix updates") {
            host()?.openBusiness(getString(R.string.action_like), ProductEvent.Like)
        })
        content.addView(listRow("Offline cache", "Saved songs and downloads", "Open local cache") {
            host()?.openBusiness(getString(R.string.action_download), ProductEvent.Download)
        })
        return ScrollView(context).apply { addView(content) }
    }

    override fun onDestroyView() {
        adRenderer?.destroy()
        adRenderer = null
        super.onDestroyView()
    }

    private fun topEntranceRow(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = marginParams(top = 18)
            addView(coverCard("Recently played", "32 songs", ProductEvent.PlayStart, getString(R.string.action_play)))
            addView(coverCard("Liked songs", "124 tracks", ProductEvent.Like, getString(R.string.action_like), startMargin = 10))
            addView(coverCard("Local cache", "18 saved", ProductEvent.Download, getString(R.string.action_download), startMargin = 10))
        }
    }

    private fun coverCard(
        title: String,
        subtitle: String,
        event: ProductEvent,
        buttonTitle: String,
        startMargin: Int = 0
    ): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(startMargin)
            }
            setOnClickListener { host()?.openBusiness(buttonTitle, event) }
            addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(104)
                )
                background = coverBackground()
            })
            addView(space(10))
            addView(TextView(requireContext()).apply {
                text = title
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            addView(TextView(requireContext()).apply {
                text = subtitle
                textSize = 12f
                setTextColor(Color.parseColor("#99FFFFFF"))
            })
        }
    }

    private fun cleanBoostRow(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = marginParams(top = 16)
            addView(actionCard("Clean", CleanBoostActivity.Mode.CLEAN))
            addView(actionCard("Boost", CleanBoostActivity.Mode.BOOST, startMargin = 15))
        }
    }

    private fun actionCard(text: String, mode: CleanBoostActivity.Mode, startMargin: Int = 0): View {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background = panelBackground()
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(startMargin)
            }
            setOnClickListener {
                host()?.openSceneBusiness(
                    scene = AdScene.CleanBoost.key,
                    destination = android.content.Intent(requireContext(), CleanBoostActivity::class.java)
                        .putExtra(CleanBoostActivity.EXTRA_MODE, mode.name)
                )
            }
        }
    }

    private fun youtubeImportRow(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 0)
            layoutParams = marginParams(top = 20)
            setOnClickListener {
                BusinessFlowNavigator.openEventToDestination(
                    activity = requireActivity(),
                    event = ProductEvent.OtherPlaylist,
                    destination = android.content.Intent(requireContext(), PlaylistImportActivity::class.java)
                )
            }
            addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                background = roundedBackground("#B91C1C", "#B91C1C", 8)
            })
            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = dp(16)
                }
                addView(TextView(requireContext()).apply {
                    text = "Import YouTube playlist"
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                addView(TextView(requireContext()).apply {
                    text = "Sync saved mixes into library"
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                })
            })
        }
    }

    private fun listRow(title: String, subtitle: String, meta: String, onClick: () -> Unit): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = marginParams(top = 12)
            background = panelBackground()
            setOnClickListener { onClick() }
            addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
                background = coverBackground()
            })
            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(14)
                }
                addView(TextView(requireContext()).apply {
                    text = title
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                addView(TextView(requireContext()).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                })
            })
            addView(TextView(requireContext()).apply {
                text = meta
                textSize = 12f
                setTextColor(Color.parseColor("#99FFFFFF"))
            })
        }
    }

    private fun header(title: String, subtitle: String): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(requireContext()).apply {
                text = title
                textSize = 28f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            addView(TextView(requireContext()).apply {
                text = subtitle
                textSize = 13f
                setTextColor(Color.parseColor("#99FFFFFF"))
            })
        }
    }

    private fun section(text: String): View {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#D0D5DD"))
            layoutParams = marginParams(top = 18)
        }
    }

    private fun panelBackground(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(Color.parseColor("#26FFFFFF"))
            setStroke(dp(1), Color.parseColor("#33FFFFFF"))
        }
    }

    private fun coverBackground(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(Color.parseColor("#303A46"))
        }
    }

    private fun roundedBackground(fill: String, stroke: String, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(radiusDp).toFloat()
            setColor(Color.parseColor(fill))
            setStroke(dp(1), Color.parseColor(stroke))
        }
    }

    private fun marginParams(top: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = top
        }
    }

    private fun space(dpValue: Int): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(dpValue)
            )
        }
    }

    private fun dp(value: Int): Int {
        return (value * requireContext().resources.displayMetrics.density).toInt()
    }

    private fun host(): MainActivity? = activity as? MainActivity
}
