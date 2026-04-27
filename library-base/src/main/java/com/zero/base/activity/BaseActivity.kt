package com.zero.base.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    lateinit var binding: VB
    private var loadingDialog: LoadingDialog? = null

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
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            stateBarHeight = systemBars.top
            navigationBarHeight = systemBars.bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(binding.root)
        initData()
        initView()
        addListener()
    }

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


}
