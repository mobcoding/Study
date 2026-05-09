package com.zero.base.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.zero.library_base.R

abstract class RecyclerViewBanner<L : RecyclerView.LayoutManager?, A : RecyclerView.Adapter<*>?> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    /**
     * 刷新间隔时间
     */
    private var autoPlayDuration: Int = 4000

    /**
     * 是否显示指示器
     */
    private var showIndicator: Boolean = false
    private var indicatorContainer: RecyclerView? = null
    protected var mSelectedDrawable: Drawable? = null
    protected var mUnselectedDrawable: Drawable? = null
    private var indicatorAdapter: IndicatorAdapter? = null

    /**
     * 指示器间距
     */
    protected var indicatorMargin: Int = 0

    private var mRecyclerView: RecyclerView? = null

    protected var adapter: A? = null

    protected var mLayoutManager: L? = null

    private var hasInit: Boolean = false

    protected var bannerSize: Int = 1

    protected var currentIndex: Int = 0

    private var isPlaying: Boolean = false

    private var isAutoPlaying: Boolean = false

    private var tempUrlList: List<String> = ArrayList()


    private val mHandler: Handler by lazy {
        Handler(Looper.getMainLooper()) { msg ->
            if (msg.what == WHAT_AUTO_PLAY) {
                mRecyclerView?.smoothScrollToPosition(++currentIndex)
                refreshIndicator()
                mHandler.sendEmptyMessageDelayed(WHAT_AUTO_PLAY, autoPlayDuration.toLong())
            }
            false
        }
    }

    init {
        initView(context, attrs)
    }

    protected fun initView(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerViewBanner)
        showIndicator = a.getBoolean(R.styleable.RecyclerViewBanner_showIndicator, true)
        autoPlayDuration = a.getInt(R.styleable.RecyclerViewBanner_interval, 4000)
        isAutoPlaying = a.getBoolean(R.styleable.RecyclerViewBanner_autoPlaying, true)
        mSelectedDrawable = a.getDrawable(R.styleable.RecyclerViewBanner_indicatorSelectedSrc)
        mUnselectedDrawable = a.getDrawable(R.styleable.RecyclerViewBanner_indicatorUnselectedSrc)
        if (mSelectedDrawable == null) {
            //绘制默认选中状态图形
            val selectedGradientDrawable = GradientDrawable()
            selectedGradientDrawable.shape = GradientDrawable.OVAL
            selectedGradientDrawable.setColor(Color.RED)
            selectedGradientDrawable.setSize(dp2px(5), dp2px(5))
            selectedGradientDrawable.cornerRadius = (dp2px(5) / 2).toFloat()
            mSelectedDrawable = LayerDrawable(arrayOf<Drawable>(selectedGradientDrawable))
        }
        if (mUnselectedDrawable == null) {
            //绘制默认未选中状态图形
            val unSelectedGradientDrawable = GradientDrawable()
            unSelectedGradientDrawable.shape = GradientDrawable.OVAL
            unSelectedGradientDrawable.setColor(Color.GRAY)
            unSelectedGradientDrawable.setSize(dp2px(5), dp2px(5))
            unSelectedGradientDrawable.cornerRadius = dp2px(5).toFloat() / 2
            mUnselectedDrawable = LayerDrawable(arrayOf<Drawable>(unSelectedGradientDrawable))
        }

        indicatorMargin = a.getDimensionPixelSize(R.styleable.RecyclerViewBanner_indicatorSpace, dp2px(4))
        val marginLeft = a.getDimensionPixelSize(R.styleable.RecyclerViewBanner_indicatorMarginLeft, dp2px(16))
        val marginRight = a.getDimensionPixelSize(R.styleable.RecyclerViewBanner_indicatorMarginRight, dp2px(0))
        val marginBottom = a.getDimensionPixelSize(R.styleable.RecyclerViewBanner_indicatorMarginBottom, dp2px(11))
        val gravity = when (a.getInt(R.styleable.RecyclerViewBanner_indicatorGravity, 0)) {
            0 -> GravityCompat.START
            2 -> GravityCompat.END
            else -> Gravity.CENTER
        }
        val orientation = if (a.getInt(R.styleable.RecyclerViewBanner_orientation, 0) == 1) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL
        a.recycle()
        mRecyclerView = RecyclerView(context).apply {
            PagerSnapHelper().attachToRecyclerView(this)
            mLayoutManager = getLayoutManager(context, orientation)
            this.layoutManager = mLayoutManager
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    onBannerScrolled(recyclerView, dx, dy)
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    onBannerScrollStateChanged(recyclerView, newState)
                }
            })
        }
        addView(mRecyclerView, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        //指示器部分
        indicatorContainer = RecyclerView(context)

        indicatorContainer?.layoutManager = LinearLayoutManager(context, orientation, false)
        indicatorAdapter = IndicatorAdapter()
        indicatorContainer?.adapter = indicatorAdapter
        val params = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.BOTTOM or gravity
        params.setMargins(marginLeft, 0, marginRight, marginBottom)
        addView(indicatorContainer, params)
        if (!showIndicator) {
            indicatorContainer?.visibility = GONE
        }
    }

    protected open fun onBannerScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
    }

    protected open fun onBannerScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
    }

    protected abstract fun getLayoutManager(context: Context, orientation: Int): L

    protected abstract fun getAdapter(context: Context, list: List<String>?, onBannerItemClickListener: OnBannerItemClickListener?): A

    /**
     * 设置轮播间隔时间
     *
     * @param millisecond 时间毫秒
     */
    fun setIndicatorInterval(millisecond: Int) {
        this.autoPlayDuration = millisecond
    }

    /**
     * 设置是否自动播放（上锁）
     *
     * @param playing 开始播放
     */
    @Synchronized
    protected fun setPlaying(playing: Boolean) {
        if (isAutoPlaying && hasInit) {
            if (!isPlaying && playing) {
                mHandler.sendEmptyMessageDelayed(WHAT_AUTO_PLAY, autoPlayDuration.toLong())
                isPlaying = true
            } else if (isPlaying && !playing) {
                mHandler.removeMessages(WHAT_AUTO_PLAY)
                isPlaying = false
            }
        }
    }

    /**
     * 设置是否禁止滚动播放
     */
    fun setAutoPlaying(isAutoPlaying: Boolean) {
        this.isAutoPlaying = isAutoPlaying
        setPlaying(this.isAutoPlaying)
    }

    fun isPlaying(): Boolean {
        return isPlaying
    }

    fun setShowIndicator(showIndicator: Boolean) {
        this.showIndicator = showIndicator
        indicatorContainer?.visibility = if (showIndicator) VISIBLE else GONE
    }

    /**
     * 设置轮播数据集
     */
    /**
     * 设置轮播数据集
     */
    @SuppressLint("NotifyDataSetChanged")
    @JvmOverloads
    fun initBannerImageView(newList: List<String>, onBannerItemClickListener: OnBannerItemClickListener? = null) {
        if (compareListDifferent(newList, tempUrlList)) {
            hasInit = false
            visibility = VISIBLE
            setPlaying(false)
            adapter = getAdapter(context, newList, onBannerItemClickListener)
            mRecyclerView!!.adapter = adapter
            tempUrlList = newList
            bannerSize = tempUrlList.size
            if (bannerSize > 1) {
                indicatorContainer!!.visibility = VISIBLE
                currentIndex = bannerSize * 10000
                mRecyclerView?.scrollToPosition(currentIndex)
                indicatorAdapter?.notifyDataSetChanged()
                setPlaying(true)
            } else {
                indicatorContainer!!.visibility = GONE
                currentIndex = 0
            }
            hasInit = true
        }
        if (!showIndicator) {
            indicatorContainer!!.visibility = GONE
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> setPlaying(false)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> setPlaying(true)
        }
        //解决recyclerView嵌套问题
        try {
            return super.dispatchTouchEvent(ev)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }

    //解决recyclerView嵌套问题
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onTouchEvent(ev)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }

    //解决recyclerView嵌套问题
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onInterceptTouchEvent(ev)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setPlaying(true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        setPlaying(false)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        setPlaying(visibility == VISIBLE)
    }

    /**
     * 标示点适配器
     */
    protected inner class IndicatorAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
        private var currentPosition: Int = 0

        fun setPosition(currentPosition: Int) {
            this.currentPosition = currentPosition
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val bannerPoint = ImageView(context)
            val lp = RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(indicatorMargin, indicatorMargin, indicatorMargin, indicatorMargin)
            bannerPoint.layoutParams = lp
            return object : RecyclerView.ViewHolder(bannerPoint) {}
        }


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val bannerPoint = holder.itemView as ImageView
            bannerPoint.setImageDrawable(if (currentPosition == position) mSelectedDrawable else mUnselectedDrawable)
        }

        override fun getItemCount(): Int {
            return bannerSize
        }
    }


    /**
     * 改变导航的指示点
     */
    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    protected fun refreshIndicator() {
        if (showIndicator && bannerSize > 1) {
            indicatorAdapter?.setPosition(currentIndex % bannerSize)
            indicatorAdapter?.notifyDataSetChanged()
        }
    }

    interface OnBannerItemClickListener {
        fun onItemClick(position: Int)
    }

    private fun dp2px(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), Resources.getSystem().displayMetrics).toInt()
    }

    /**
     * 获取颜色
     */
    protected fun getColor(@ColorRes color: Int): Int {
        return ContextCompat.getColor(context, color)
    }

    private fun compareListDifferent(newTabList: List<String>, oldTabList: List<String>?): Boolean {
        if (oldTabList.isNullOrEmpty()) return true
        if (newTabList.size != oldTabList.size) return true
        for (i in newTabList.indices) {
            if (TextUtils.isEmpty(newTabList[i])) return true
            if (newTabList[i] != oldTabList[i]) {
                return true
            }
        }
        return false
    }

    companion object {
        protected const val WHAT_AUTO_PLAY: Int = 1000
    }
}