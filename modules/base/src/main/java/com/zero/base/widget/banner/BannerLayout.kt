package com.zero.base.widget.banner

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zero.base.ext.dp
import com.zero.base.layoutmanager.BannerLayoutManager
import com.zero.base.layoutmanager.CenterSnapHelper
import com.zero.library_base.R

class BannerLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private var autoPlayDuration = 0 //刷新间隔时间

    private var showIndicator = false //是否显示指示器
    private var indicatorContainer: RecyclerView? = null
    private var mSelectedDrawable: Drawable? = null
    private var mUnselectedDrawable: Drawable? = null
    private var indicatorAdapter: IndicatorAdapter? = null
    private var indicatorMargin = 0 //指示器间距
    private var mRecyclerView: RecyclerView? = null

    private var mLayoutManager: BannerLayoutManager? = null

    private val whatAutoPlay = 1000

    private var hasInit = false
    private var bannerSize = 1
    private var currentIndex = 0

    @set:Synchronized
    var isPlaying: Boolean = false
        /**
         * 设置是否自动播放（上锁）
         *
         * @param playing 开始播放
         */
        private set(playing) {
            if (isAutoPlaying && hasInit) {
                if (!field && playing) {
                    mHandler.sendEmptyMessageDelayed(whatAutoPlay, autoPlayDuration.toLong())
                    field = true
                } else if (field && !playing) {
                    mHandler.removeMessages(whatAutoPlay)
                    field = false
                }
            }
        }

    private var isAutoPlaying = true
    private var itemSpace: Int = 0
    private var centerScale: Float = 0f
    private var moveSpeed: Float = 0f
    private val mHandler: Handler by lazy {
        Handler(Looper.getMainLooper()) { msg ->
            if (msg.what == whatAutoPlay) {
                if (currentIndex == mLayoutManager?.currentPosition) {
                    ++currentIndex
                    mRecyclerView?.smoothScrollToPosition(currentIndex)
                    mHandler.sendEmptyMessageDelayed(whatAutoPlay, autoPlayDuration.toLong())
                    refreshIndicator()
                }
            }
            false
        }
    }

    init {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BannerLayout)
        showIndicator = a.getBoolean(R.styleable.BannerLayout_showIndicator, true)
        autoPlayDuration = a.getInt(R.styleable.BannerLayout_interval, 4000)
        isAutoPlaying = a.getBoolean(R.styleable.BannerLayout_autoPlaying, true)
        itemSpace = a.getInt(R.styleable.BannerLayout_itemSpace, 20)
        centerScale = a.getFloat(R.styleable.BannerLayout_centerScale, 1.2f)
        moveSpeed = a.getFloat(R.styleable.BannerLayout_moveSpeed, 1.0f)
        if (mSelectedDrawable == null) {
            //绘制默认选中状态图形
            val selectedGradientDrawable = GradientDrawable()
            selectedGradientDrawable.shape = GradientDrawable.RECTANGLE
            selectedGradientDrawable.setColor(Color.RED)
            selectedGradientDrawable.setSize(8.dp, 4.dp)
            selectedGradientDrawable.cornerRadius = 2f.dp
            mSelectedDrawable = LayerDrawable(arrayOf<Drawable>(selectedGradientDrawable))
        }
        if (mUnselectedDrawable == null) {
            //绘制默认未选中状态图形
            val unSelectedGradientDrawable = GradientDrawable()
            unSelectedGradientDrawable.shape = GradientDrawable.OVAL
            unSelectedGradientDrawable.setColor(Color.GRAY)
            unSelectedGradientDrawable.setSize(4.dp, 4.dp)
            unSelectedGradientDrawable.cornerRadius = 2f.dp
            mUnselectedDrawable = LayerDrawable(arrayOf<Drawable>(unSelectedGradientDrawable))
        }

        indicatorMargin = dp2px(4)
        val marginLeft = dp2px(16)
        val marginRight = dp2px(0)
        val marginBottom = dp2px(11)
        val gravity = GravityCompat.START
        val o = a.getInt(R.styleable.BannerLayout_orientation, 0)
        var orientation = RecyclerView.HORIZONTAL
        if (o == 0) {
            orientation = RecyclerView.HORIZONTAL
        } else if (o == 1) {
            orientation = RecyclerView.VERTICAL
        }
        a.recycle()
        //轮播图部分
        mRecyclerView = RecyclerView(context)
        val vpLayoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        addView(mRecyclerView, vpLayoutParams)
        mLayoutManager = BannerLayoutManager(orientation)
        mLayoutManager?.setItemSpace(itemSpace)
        mLayoutManager?.setCenterScale(centerScale)
        mLayoutManager?.setMoveSpeed(moveSpeed)
        mRecyclerView?.layoutManager = mLayoutManager
        CenterSnapHelper().attachToRecyclerView(mRecyclerView)


        //指示器部分
        indicatorContainer = RecyclerView(context)
        val indicatorLayoutManager = LinearLayoutManager(context, orientation, false)
        indicatorContainer?.layoutManager = indicatorLayoutManager
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

    // 设置是否禁止滚动播放
    fun setAutoPlaying(isAutoPlaying: Boolean) {
        this.isAutoPlaying = isAutoPlaying
        isPlaying = this.isAutoPlaying
    }

    //设置是否显示指示器
    fun setShowIndicator(showIndicator: Boolean) {
        this.showIndicator = showIndicator
        indicatorContainer?.visibility = if (showIndicator) VISIBLE else GONE
    }

    //设置当前图片缩放系数
    fun setCenterScale(centerScale: Float) {
        this.centerScale = centerScale
        mLayoutManager?.setCenterScale(centerScale)
    }

    //设置跟随手指的移动速度
    fun setMoveSpeed(moveSpeed: Float) {
        this.moveSpeed = moveSpeed
        mLayoutManager?.setMoveSpeed(moveSpeed)
    }

    //设置图片间距
    fun setItemSpace(itemSpace: Int) {
        this.itemSpace = itemSpace
        mLayoutManager?.setItemSpace(itemSpace)
    }

    /**
     * 设置轮播间隔时间
     *
     * @param autoPlayDuration 时间毫秒
     */
    fun setAutoPlayDuration(autoPlayDuration: Int) {
        this.autoPlayDuration = autoPlayDuration
    }

    fun setOrientation(orientation: Int) {
        mLayoutManager?.orientation = orientation
    }


    /**
     * 设置轮播数据集
     */
    fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        hasInit = false
        mRecyclerView?.adapter = adapter
        bannerSize = adapter.itemCount
        mLayoutManager?.infinite = bannerSize >= 3
        isPlaying = true
        mRecyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dx != 0) {
                    isPlaying = false
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val first = mLayoutManager?.currentPosition ?: -1
                Log.d("xxx", "onScrollStateChanged")
                if (currentIndex != first) {
                    currentIndex = first
                }
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    isPlaying = true
                }
                refreshIndicator()
            }
        })
        hasInit = true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> isPlaying = false
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isPlaying = true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isPlaying = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isPlaying = false
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            isPlaying = true
        } else {
            isPlaying = false
        }
    }

    /**
     * 标示点适配器
     */
    inner class IndicatorAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
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

    private fun dp2px(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), Resources.getSystem().displayMetrics).toInt()
    }

    /**
     * 改变导航的指示点
     */
    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun refreshIndicator() {
        if (showIndicator && bannerSize > 1) {
            indicatorAdapter?.setPosition(currentIndex % bannerSize)
            indicatorAdapter?.notifyDataSetChanged()
        }
    }

    interface OnBannerItemClickListener {
        fun onItemClick(position: Int)
    }
}