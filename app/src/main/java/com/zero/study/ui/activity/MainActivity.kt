package com.zero.study.ui.activity

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.addListener
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.drawToBitmap
import com.google.android.material.tabs.TabLayoutMediator
import com.toolkit.admob.manager.InterstitialPreloadAdMobManager
import com.toolkit.admob_libray.BuildConfig
import com.zero.base.activity.BaseActivity
import com.zero.base.ext.checkAndRequestNotification
import com.zero.base.ext.registerNotificationLauncher
import com.zero.base.theme.AppTheme
import com.zero.base.transformer.CustomPageTransformer
import com.zero.base.util.StorageUtils
import com.zero.base.widget.ClipImageView
import com.zero.base.widget.TRANSITION_DATA_KEY
import com.zero.base.widget.TransitionData
import com.zero.base.widget.TransitionType
import com.zero.study.R
import com.zero.study.databinding.ActivityMainBinding
import com.zero.study.databinding.TabItemBinding
import com.zero.study.ui.adapter.ViewPagerAdapter
import com.zero.study.ui.fragment.FourFragment
import com.zero.study.ui.fragment.HomeFragment
import com.zero.study.ui.fragment.SecondFragment
import com.zero.study.ui.fragment.ThirdFragment
import kotlin.math.hypot

/**
 * @author Admin
 */
class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    private var recreateTransitionData: TransitionData? = null

    override fun initView() {
        StorageUtils.putBoolean(SplashActivity.TIME_START, false)
    }

    private val notificationPermissionLauncher = registerNotificationLauncher(onGranted = {
        // 权限刚刚获取成功，可以执行初始化推送等操作
    }, onDenied = {
        Log.d("zzz", "User denied notification permission")
    })


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            recreateTransitionData = BundleCompat.getParcelable(savedInstanceState, TRANSITION_DATA_KEY, TransitionData::class.java)
            recreateTransitionData?.let { transitionAnimation(it) }
        }
        checkAndRequestNotification(notificationPermissionLauncher) {
            // 如果已经授权过了，直接走这里的逻辑
        }
        if (!checkNotificationListener(this)) {
//            startNotificationListener(this)
        }
    }

    /**
     * 获取读取通知信息权限
     * */
    fun startNotificationListener(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.data = "package:${context.packageName}".toUri()
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            context.startActivity(intent)
            true
        }
    }

    /**
     * 能否获取读取通知信息权限
     * */
    fun checkNotificationListener(context: Context): Boolean {
        return try {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (recreateTransitionData != null) {
            outState.putParcelable(TRANSITION_DATA_KEY, recreateTransitionData)
        }
    }

    override fun initData() {
        val viewPagerAdapter = ViewPagerAdapter(this);
        viewPagerAdapter.addFragment(ViewPagerAdapter.FragmentWrapper(getString(R.string.tab_home), R.drawable.tab_home_selector, HomeFragment()))
        viewPagerAdapter.addFragment(ViewPagerAdapter.FragmentWrapper(getString(R.string.tab_editor), R.drawable.tab_editor_selector, SecondFragment()))
        viewPagerAdapter.addFragment(ViewPagerAdapter.FragmentWrapper(getString(R.string.tab_free_style), R.drawable.tab_free_style_selector, ThirdFragment.newInstance()))
        viewPagerAdapter.addFragment(ViewPagerAdapter.FragmentWrapper(getString(R.string.tab_setting), R.drawable.tab_setting_selector, FourFragment.newInstance()))
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.setPageTransformer(CustomPageTransformer())
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val itemBinding = TabItemBinding.inflate(layoutInflater, null, false)
            itemBinding.tabIcon.setImageResource(viewPagerAdapter.getIconRes(position))
            itemBinding.tabTitle.text = viewPagerAdapter.getTitle(position)
            tab.customView = itemBinding.root
        }.attach()
    }

    private fun transitionAnimation(transitionData: TransitionData) {
        binding.ivTransition.visibility = View.VISIBLE
        binding.ivTransition.setImageBitmap(transitionData.screenBitmap)

        binding.ivTransition.post {
            val animator = ValueAnimator.ofFloat()
            var clipType = ClipImageView.ClipType.CIRCLE
            when (transitionData.type) {
                TransitionType.ENTER -> {
                    // 进入动画，裁切掉圆内的区域 圆由小变大
                    animator.setFloatValues(0f, hypot(binding.ivTransition.width.toFloat(), binding.ivTransition.height.toFloat()))
                    clipType = ClipImageView.ClipType.CIRCLE_REVERSE
                }

                TransitionType.EXIT -> {
                    // 退出动画，裁切掉圆外的区域 圆由大变小
                    animator.setFloatValues(hypot(binding.ivTransition.width.toFloat(), binding.ivTransition.height.toFloat()), 0f)
                    clipType = ClipImageView.ClipType.CIRCLE
                }
            }
            animator.duration = resources.getInteger(android.R.integer.config_longAnimTime).toLong()
            animator.addListener(onEnd = {
                // 动画结束后隐藏 ImageView
                binding.ivTransition.visibility = View.GONE
            })
            animator.addUpdateListener {
                val radius = it.animatedValue as Float
                // 更新裁切区域
                binding.ivTransition.clipCircle(transitionData.centerX, transitionData.centerY, radius, clipType)
            }
            animator.start()
        }
    }

    private fun transitionRecreate(type: TransitionType) {
        // 获取切换主题menu的坐标（以menuItemView的中心点为圆形揭露动画的中心点）
        val menuItemView = binding.root
        val location = IntArray(2)
        menuItemView.getLocationOnScreen(location)
        val centerX = location[0] + menuItemView.width / 2f
        val centerY = location[1] + menuItemView.height / 2f
        // Activity截图
        val screenBitmap = window.decorView.drawToBitmap()
        recreateTransitionData = TransitionData(centerX, centerY, screenBitmap, type)
        // 重建Activity
        recreate()
    }

    override fun addListener() {
        supportFragmentManager.setFragmentResultListener(this::class.java.simpleName, this) { requestKey, result ->
            if (requestKey == this::class.java.simpleName) {
                val screenHeight = result.getInt("screenHeight")
                Log.d("zzz", "setFragmentResultListener: $screenHeight")
            }
        }
        supportFragmentManager.setFragmentResultListener("changeTheme", this) { _, _ ->
            run {
                val localNightMode = delegate.localNightMode
                val theme = if (localNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
                    AppTheme.LIGHT
                } else {
                    AppTheme.DARK
                }
                if (delegate.localNightMode == theme.mode) {
                    return@run
                }
                setTheme(theme)
                // 使用过渡动画重建Activity
                transitionRecreate(
                    // 根据当前主题设置过渡动画类型
                    if (theme == AppTheme.DARK) TransitionType.ENTER
                    else TransitionType.EXIT)
            }
        }


    }

    override fun onResume() {
        super.onResume()
        InterstitialPreloadAdMobManager.preLoadInterstitialAd(BuildConfig.ADMOB_INTERSTITIAL_CONNECT_RESULT)
    }
}