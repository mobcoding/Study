package com.zero.base.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.zero.base.fragment.LoadingDialog
import com.zero.base.theme.AppTheme
import com.zero.base.util.StorageUtils

/**

 * @date:2024/5/24 18:35
 * @path:com.toolkit.base.ui.activity.AbstractActivity
 */
abstract class BaseActivity<VB : ViewBinding>(private val inflate: (LayoutInflater) -> VB) :
    AppCompatActivity() {
    protected open val useRootWindowInsetsPadding: Boolean = true

    lateinit var binding: VB
    private var loadingDialog: LoadingDialog? = null
    private var systemBarInsets: Insets = Insets.NONE

    override fun attachBaseContext(newBase: Context) {
        // 加载本地配置的主题
        val theme = getAppTheme()
        delegate.localNightMode = theme.mode
        return super.attachBaseContext(newBase)
    }

    private fun getAppTheme(): AppTheme {
        val name = StorageUtils.getString(THEME_KEY, AppTheme.AUTO.name)!!
        return AppTheme.valueOf(name)
    }

    fun setTheme(theme: AppTheme) {
        if (theme == AppTheme.AUTO) {
            // delete theme
            StorageUtils.remove(THEME_KEY)
            return
        }
        StorageUtils.putString(THEME_KEY, theme.name)
    }

    var stateBarHeight = 0
    var navigationBarHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //去掉导航栏半透明遮罩
            window.isNavigationBarContrastEnforced = false
        }
        binding = inflate(layoutInflater)
        val initialRootPadding = binding.root.recordInitialPadding()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            stateBarHeight = systemBars.top
            navigationBarHeight = systemBars.bottom
            systemBarInsets = systemBars
            if (useRootWindowInsetsPadding) {
                v.updatePadding(
                    left = initialRootPadding.left + systemBars.left,
                    top = initialRootPadding.top + systemBars.top,
                    right = initialRootPadding.right + systemBars.right,
                    bottom = initialRootPadding.bottom + systemBars.bottom
                )
            }
            onSystemBarInsetsChanged(systemBars)
            insets
        }
        setContentView(binding.root)
        initData()
        initView()
        addListener()
        ViewCompat.requestApplyInsets(binding.root)
    }

    protected open fun onSystemBarInsetsChanged(insets: Insets) = Unit

    protected fun applySystemBarPadding(
        view: View,
        left: Boolean = false,
        top: Boolean = false,
        right: Boolean = false,
        bottom: Boolean = false
    ) {
        val initialPadding = view.recordInitialPadding()
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = initialPadding.left + if (left) systemBars.left else 0,
                top = initialPadding.top + if (top) systemBars.top else 0,
                right = initialPadding.right + if (right) systemBars.right else 0,
                bottom = initialPadding.bottom + if (bottom) systemBars.bottom else 0
            )
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    protected fun applyRecyclerViewInsets(
        recyclerView: RecyclerView,
        top: Boolean = false,
        bottom: Boolean = true,
        left: Boolean = false,
        right: Boolean = false
    ) {
        recyclerView.clipToPadding = false
        applySystemBarPadding(
            view = recyclerView,
            left = left,
            top = top,
            right = right,
            bottom = bottom
        )
    }

    protected fun currentSystemBarInsets(): Insets = systemBarInsets

    fun showLoading() {
        if (isFinishing || isDestroyed) return

        val fm = supportFragmentManager
        if (fm.findFragmentByTag("loading") != null) return

        loadingDialog = LoadingDialog()
        loadingDialog?.show(fm, "loading")
    }

    fun hideLoading() {
        val dialog = supportFragmentManager.findFragmentByTag("loading") as? LoadingDialog
        dialog?.dismissAllowingStateLoss()
        loadingDialog = null
    }


    /**
     * 使用 WindowInsetsCompat.Type.statusBars() 仅隐藏状态栏。
     * 使用 WindowInsetsCompat.Type.navigationBars() 仅隐藏导航栏。
     * 使用 WindowInsetsCompat.Type.systemBars() 可隐藏这两个系统栏。
     */
    fun hideSystemBars(type: Int) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        //控制状态栏内容（例如时间、电池图标、通知图标）的外观：
        windowInsetsController.isAppearanceLightStatusBars = false
        //控制导航栏内容（例如返回、主页、最近应用按钮）的外观：
        windowInsetsController.isAppearanceLightNavigationBars = false
        windowInsetsController.hide(type)
    }

    fun showSystemBars(type: Int) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.show(type)
    }

    abstract fun initView()

    abstract fun initData()

    abstract fun addListener()

    companion object {
        const val THEME_KEY = "theme"
    }

    private fun View.recordInitialPadding() = InitialPadding(
        left = paddingLeft,
        top = paddingTop,
        right = paddingRight,
        bottom = paddingBottom
    )

    private data class InitialPadding(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )


}
