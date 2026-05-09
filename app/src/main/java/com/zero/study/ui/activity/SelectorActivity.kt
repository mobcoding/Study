package com.zero.study.ui.activity

import android.animation.ValueAnimator
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.GridLayoutManager
import com.zero.base.activity.BaseActivity
import com.zero.study.R
import com.zero.study.bean.Album
import com.zero.study.databinding.ActivitySelectorBinding
import com.zero.study.ui.adapter.AlbumAdapter

class SelectorActivity : BaseActivity<ActivitySelectorBinding>(ActivitySelectorBinding::inflate) {
    override val useRootWindowInsetsPadding: Boolean = false

    private val adapter = AlbumAdapter()
    private val bottomView get() = binding.bottomLayout
    private val workView get() = binding.ivHeader
    private val toolbar get() = binding.toolbarLayout.clToolbar
    private var isSelectMode = false
    private val maxImageScale = 0.7f

    override fun initView() {
        applySystemBarPadding(binding.toolbarLayout.root, top = true)
        applyRecyclerViewInsets(binding.recyclerView, bottom = true)

        binding.recyclerView.apply {
            this.adapter = this@SelectorActivity.adapter
            layoutManager = GridLayoutManager(this@SelectorActivity, 4)
            setHasFixedSize(true)
        }

        val albumList = ArrayList<Album>()
        repeat(150) {
            albumList.add(Album(R.mipmap.icon))
        }
        adapter.submitList(albumList)

        setupInitialState()
    }

    private fun setupInitialState() {
        bottomView.layoutParams.height = 0
        bottomView.requestLayout()
        toolbar.alpha = 1f

        workView.post {
            startEnterAnimation()
        }
    }

    private fun startEnterAnimation() {
        val imageHeight = workView.height.toFloat()
        val toolbarHeight = toolbar.height.toFloat()

        val scaleAnimator = ValueAnimator.ofFloat(1f, maxImageScale)
        scaleAnimator.duration = 300
        scaleAnimator.interpolator = AccelerateDecelerateInterpolator()
        scaleAnimator.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            val progress = (1f - scale) / (1f - maxImageScale)
            workView.scaleX = scale
            workView.scaleY = scale
            val translateY = -(toolbarHeight + (imageHeight * (1f - scale)) / 2)
            workView.translationY = translateY * progress
            toolbar.translationY = translateY * progress
            toolbar.alpha = 1f - progress
        }

        val panelHeightAnimator = ValueAnimator.ofInt(0, calculateBottomViewHeight())
        panelHeightAnimator.duration = 200
        panelHeightAnimator.interpolator = AccelerateDecelerateInterpolator()
        panelHeightAnimator.addUpdateListener { animation ->
            val height = animation.animatedValue as Int
            bottomView.layoutParams.height = height
            bottomView.requestLayout()
        }
        scaleAnimator.start()
        scaleAnimator.doOnEnd {
            panelHeightAnimator.start()
        }
        panelHeightAnimator.doOnEnd {
            isSelectMode = true
        }
    }

    private fun hideCommentPanel() {
        if (!isSelectMode) return
        isSelectMode = false

        val imageHeight = workView.height.toFloat()
        val toolbarHeight = toolbar.height.toFloat()

        val scaleAnimator = ValueAnimator.ofFloat(maxImageScale, 1f)
        scaleAnimator.duration = 300
        scaleAnimator.interpolator = AccelerateDecelerateInterpolator()
        scaleAnimator.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            val progress = (1f - scale) / (1f - maxImageScale)
            workView.scaleX = scale
            workView.scaleY = scale
            val translateY = -(toolbarHeight + (imageHeight * (1f - scale)) / 2)
            workView.translationY = translateY * progress
            toolbar.translationY = translateY * progress
            toolbar.alpha = 1f - progress
        }

        val panelAnimator = ValueAnimator.ofInt(bottomView.height, 0)
        panelAnimator.duration = 300
        panelAnimator.interpolator = AccelerateDecelerateInterpolator()
        panelAnimator.addUpdateListener { animation ->
            val height = animation.animatedValue as Int
            bottomView.layoutParams.height = height
            bottomView.requestLayout()
        }

        scaleAnimator.start()
        panelAnimator.start()
    }

    private fun calculateBottomViewHeight(): Int {
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        Log.d("TAG", "calculateBottomViewHeight: ${binding.root.height}")
        Log.d("TAG", "calculateBottomViewHeight: $screenHeight")
        Log.d("TAG", "calculateBottomViewHeight: $stateBarHeight")
        Log.d("TAG", "calculateBottomViewHeight: $navigationBarHeight")

        return (screenHeight - (workView.height * maxImageScale)).toInt()
    }

    override fun initData() = Unit

    override fun addListener() {
        binding.toolbarLayout.tvNext.setOnClickListener {
            startEnterAnimation()
        }
        binding.toolbarLayout.ivBack.setOnClickListener {
            finish()
        }
        binding.ivClose.setOnClickListener {
            hideCommentPanel()
        }
    }
}
