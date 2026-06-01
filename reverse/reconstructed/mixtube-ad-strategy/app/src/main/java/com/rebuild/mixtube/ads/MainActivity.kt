package com.rebuild.mixtube.ads

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {
    private lateinit var homeTabView: TextView
    private lateinit var libraryTabView: TextView
    private lateinit var playerTitleView: TextView
    private lateinit var bannerContainer: FrameLayout
    private lateinit var downloadLayout: LinearLayout
    private lateinit var downloadManagerView: TextView
    private lateinit var bannerRenderer: InlineAdRenderer
    private var currentTab: MainTab = MainTab.HOME
    private var skipNextForegroundReturnAd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        bindMiniPlayer()
        bindBottomTabs()
        bannerRenderer = InlineAdRenderer(this)
        bannerRenderer.load(
            scene = AdScene.NormalBanner.key,
            container = bannerContainer,
            style = InlineAdRenderer.Style.BOTTOM_BANNER
        )

        currentTab = savedInstanceState?.getString(STATE_TAB)
            ?.let(MainTab::valueOf)
            ?: MainTab.HOME
        selectTab(currentTab, triggerEvent = false)

        val skipStartupEntryAd = intent.getBooleanExtra(EXTRA_SKIP_STARTUP_ENTRY_AD, false)
        skipNextForegroundReturnAd = skipStartupEntryAd
        if (savedInstanceState == null && !skipStartupEntryAd) {
            window.decorView.post {
                val result = ServiceLocator.adManager.showScene(this@MainActivity, AdScene.Home.key)
                Log.d(TAG, "startup home entry -> ${result.message}")
            }
        } else if (skipStartupEntryAd) {
            ServiceLocator.appForegroundTracker.markColdStartHandled()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TAB, currentTab.name)
    }

    override fun onResume() {
        super.onResume()
        updateDownloadStrip()
        maybeShowForegroundReturnAd()
    }

    override fun onDestroy() {
        bannerRenderer.destroy()
        super.onDestroy()
    }

    fun openBusiness(title: String, event: ProductEvent) {
        BusinessFlowNavigator.open(this, title, event)
    }

    fun openSceneBusiness(scene: String, destination: Intent) {
        BusinessFlowNavigator.openScene(this, scene, destination)
    }

    fun openFullNative(scene: String = AdScene.Download.key) {
        ServiceLocator.adManager.showFullNative(this, scene)
    }

    private fun bindViews() {
        homeTabView = findViewById(R.id.ic_home_home)
        libraryTabView = findViewById(R.id.ic_home_library)
        playerTitleView = findViewById(R.id.player_video_title)
        bannerContainer = findViewById(R.id.banner_ad_view_container)
        downloadLayout = findViewById(R.id.download_layout)
        downloadManagerView = findViewById(R.id.download_manager)
        downloadManagerView.setOnClickListener {
            openBusiness(getString(R.string.action_download), ProductEvent.Download)
        }
    }

    private fun bindMiniPlayer() {
        playerTitleView.text = "Late Night Drive"
        findViewById<LinearLayout>(R.id.play_bar).setOnClickListener {
            openBusiness(getString(R.string.action_play), ProductEvent.PlayStart)
        }
        findViewById<TextView>(R.id.player_video_play).setOnClickListener {
            openBusiness(getString(R.string.action_play_pause), ProductEvent.PlayPause)
        }
        findViewById<TextView>(R.id.player_video_next).setOnClickListener {
            openBusiness(getString(R.string.action_play), ProductEvent.PlayStart)
        }
    }

    private fun bindBottomTabs() {
        homeTabView.setOnClickListener {
            selectTab(MainTab.HOME, triggerEvent = currentTab != MainTab.HOME)
        }
        libraryTabView.setOnClickListener {
            selectTab(MainTab.LIBRARY, triggerEvent = currentTab != MainTab.LIBRARY)
        }
    }

    private fun selectTab(tab: MainTab, triggerEvent: Boolean) {
        currentTab = tab
        showTab(tab)
        updateTabUi(tab)
        if (triggerEvent) {
            val event = if (tab == MainTab.HOME) ProductEvent.TabSwitch else ProductEvent.OtherTab
            val result = ServiceLocator.adManager.showEvent(this, event)
            Log.d(TAG, "tab switch event=$event -> ${result.message}")
        }
    }

    private fun showTab(tab: MainTab) {
        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()
        val targetTag = tab.tag
        val target = manager.findFragmentByTag(targetTag) ?: createTabFragment(tab).also {
            transaction.add(R.id.fragment_main_container, it, targetTag)
        }
        manager.findFragmentByTag(MainTab.HOME.tag)?.takeIf { it !== target }?.let(transaction::hide)
        manager.findFragmentByTag(MainTab.LIBRARY.tag)?.takeIf { it !== target }?.let(transaction::hide)
        transaction.show(target)
        transaction.commit()
    }

    private fun updateTabUi(tab: MainTab) {
        val selectedColor = ContextCompat.getColor(this, R.color.white)
        val unselectedColor = ContextCompat.getColor(this, R.color.main_small_text_color)
        homeTabView.setTextColor(if (tab == MainTab.HOME) selectedColor else unselectedColor)
        libraryTabView.setTextColor(if (tab == MainTab.LIBRARY) selectedColor else unselectedColor)
        homeTabView.alpha = if (tab == MainTab.HOME) 1f else 0.72f
        libraryTabView.alpha = if (tab == MainTab.LIBRARY) 1f else 0.72f
    }

    private fun updateDownloadStrip() {
        downloadLayout.visibility = if (ServiceLocator.hasQueuedDownloads) View.VISIBLE else View.GONE
    }

    private fun maybeShowForegroundReturnAd() {
        if (skipNextForegroundReturnAd) {
            skipNextForegroundReturnAd = false
            return
        }
        if (!ServiceLocator.appForegroundTracker.consumeForegroundReturn()) return
        window.decorView.post {
            val result = ServiceLocator.adManager.showEvent(this@MainActivity, ProductEvent.AppSwitchBack)
            Log.d(TAG, "foreground return -> ${result.message}")
        }
    }

    private fun createTabFragment(tab: MainTab): Fragment {
        return when (tab) {
            MainTab.HOME -> HomeTabFragment()
            MainTab.LIBRARY -> LibraryTabFragment()
        }
    }

    private enum class MainTab(val tag: String) {
        HOME("tab_home"),
        LIBRARY("tab_library")
    }

    companion object {
        const val TAG = "MainActivity"
        const val STATE_TAB = "state_tab"
        const val EXTRA_SKIP_STARTUP_ENTRY_AD = "skip_startup_entry_ad"
    }
}
