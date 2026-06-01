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
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeTabFragment : Fragment() {
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
        content.addView(header("Home", "Discover"))
        content.addView(searchEntry())
        content.addView(topCollectionsRow())
        content.addView(section("Trending mixes"))
        content.addView(feedCard("Late Night Drive", "Daily mix for your next resume", "32 songs") {
            host()?.openBusiness(getString(R.string.action_play), ProductEvent.PlayStart)
        })
        content.addView(feedCard("Moonlight Echo", "Playlist refreshed a moment ago", "18 songs") {
            host()?.openBusiness(getString(R.string.action_playlist), ProductEvent.OpenPlaylist)
        })
        val nativeSlot = FrameLayout(context).apply {
            layoutParams = marginParams(top = 18)
        }
        content.addView(nativeSlot)
        adRenderer?.load(AdScene.NativeHome.key, nativeSlot, InlineAdRenderer.Style.LARGE_NATIVE)
        content.addView(section("Recommended"))
        content.addView(rowItem("Night Runner", "Single", "3:21") {
            host()?.openBusiness(getString(R.string.action_play), ProductEvent.PlayStart)
        })
        content.addView(rowItem("Liked songs", "Favorites", "124 tracks") {
            host()?.openBusiness(getString(R.string.action_like), ProductEvent.Like)
        })
        content.addView(rowItem("Offline cache", "Library", "18 saved") {
            host()?.openBusiness(getString(R.string.action_download), ProductEvent.Download)
        })
        return ScrollView(context).apply { addView(content) }
    }

    override fun onDestroyView() {
        adRenderer?.destroy()
        adRenderer = null
        super.onDestroyView()
    }

    private fun header(title: String, subtitle: String): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
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
            })
            addView(TextView(requireContext()).apply {
                text = "Setting"
                textSize = 13f
                setTextColor(Color.WHITE)
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = roundedBackground("#26FFFFFF", "#33FFFFFF", 8)
                setOnClickListener {
                    host()?.openSceneBusiness(
                        scene = AdScene.Setting.key,
                        destination = android.content.Intent(requireContext(), SettingActivity::class.java)
                    )
                }
            })
        }
    }

    private fun searchEntry(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            layoutParams = marginParams(top = 16).apply {
                height = dp(40)
            }
            background = roundedBackground("#26FFFFFF", "#33FFFFFF", 8)
            setOnClickListener {
                host()?.openBusiness(getString(R.string.action_search), ProductEvent.Search)
            }
            addView(TextView(requireContext()).apply {
                text = "S"
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            addView(horizontalSpace(16))
            addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(1), dp(16))
                background = GradientDrawable().apply { setColor(Color.parseColor("#33FFFFFF")) }
            })
            addView(horizontalSpace(16))
            addView(TextView(requireContext()).apply {
                text = "Search songs, artists, albums"
                textSize = 14f
                setTextColor(Color.parseColor("#99FFFFFF"))
            })
        }
    }

    private fun topCollectionsRow(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = marginParams(top = 20)
            addView(collectionTile("Recently played", "32 songs", ProductEvent.PlayStart, getString(R.string.action_play)))
            addView(collectionTile("Liked songs", "124 tracks", ProductEvent.Like, getString(R.string.action_like), startMargin = 10))
            addView(collectionTile("Local cache", "18 saved", ProductEvent.Download, getString(R.string.action_download), startMargin = 10))
        }
    }

    private fun collectionTile(
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
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(101))
                background = roundedBackground("#303A46", "#303A46", 8)
            })
            addView(space(8))
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

    private fun feedCard(title: String, subtitle: String, meta: String, onClick: () -> Unit): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = marginParams(top = 14)
            background = roundedBackground("#26FFFFFF", "#33FFFFFF", 12)
            setOnClickListener { onClick() }
            addView(TextView(requireContext()).apply {
                text = title
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            addView(space(6))
            addView(TextView(requireContext()).apply {
                text = subtitle
                textSize = 13f
                setTextColor(Color.parseColor("#CCFFFFFF"))
            })
            addView(space(8))
            addView(TextView(requireContext()).apply {
                text = meta
                textSize = 12f
                setTextColor(Color.parseColor("#99FFFFFF"))
            })
        }
    }

    private fun rowItem(title: String, subtitle: String, meta: String, onClick: () -> Unit): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = marginParams(top = 12)
            background = roundedBackground("#26FFFFFF", "#33FFFFFF", 12)
            setOnClickListener { onClick() }
            addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
                background = roundedBackground("#303A46", "#303A46", 6)
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

    private fun section(text: String): View {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#D0D5DD"))
            layoutParams = marginParams(top = 20)
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

    private fun horizontalSpace(dpValue: Int): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(dpValue), 1)
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
