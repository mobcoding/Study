package com.zero.base.layoutmanager

import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnFlingListener
import kotlin.math.abs

class CenterSnapHelper : OnFlingListener() {
    var mRecyclerView: RecyclerView? = null
    var mGravityScroller: Scroller? = null

    /**
     * when the dataSet is extremely large
     * [.snapToCenterView]
     * may keep calling itself because the accuracy of float
     */
    private var snapToCenter = false

    // Handles the snap on scroll case.
    private val mScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        var mScrolled: Boolean = false

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            val layoutManager = recyclerView.layoutManager as BannerLayoutManager?
            val onPageChangeListener = layoutManager?.onPageChangeListener
            onPageChangeListener?.onPageScrollStateChanged(newState)

            if (newState == RecyclerView.SCROLL_STATE_IDLE && mScrolled) {
                mScrolled = false
                if (!snapToCenter) {
                    snapToCenter = true
                    snapToCenterView(layoutManager, onPageChangeListener)
                } else {
                    snapToCenter = false
                }
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dx != 0 || dy != 0) {
                mScrolled = true
            }
        }
    }

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        val layoutManager = mRecyclerView!!.layoutManager as BannerLayoutManager? ?: return false
        val adapter = mRecyclerView!!.adapter ?: return false

        if (!layoutManager.infinite && (layoutManager.mOffset == layoutManager.maxOffset || layoutManager.mOffset == layoutManager.minOffset)) {
            return false
        }

        val minFlingVelocity = mRecyclerView!!.minFlingVelocity
        mGravityScroller!!.fling(0, 0, velocityX, velocityY, Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)

        if (layoutManager.mOrientation == BannerLayoutManager.VERTICAL && abs(velocityY.toDouble()) > minFlingVelocity) {
            val currentPosition = layoutManager.currentPosition
            val offsetPosition = (mGravityScroller!!.finalY / layoutManager.mInterval / layoutManager.distanceRatio).toInt()
            mRecyclerView!!.smoothScrollToPosition(if (layoutManager.reverseLayout) currentPosition - offsetPosition else currentPosition + offsetPosition)
            return true
        } else if (layoutManager.mOrientation == BannerLayoutManager.HORIZONTAL && abs(velocityX.toDouble()) > minFlingVelocity) {
            val currentPosition = layoutManager.currentPosition
            val offsetPosition = (mGravityScroller!!.finalX / layoutManager.mInterval / layoutManager.distanceRatio).toInt()
            mRecyclerView!!.smoothScrollToPosition(if (layoutManager.reverseLayout) currentPosition - offsetPosition else currentPosition + offsetPosition)
            return true
        }

        return true
    }

    @Throws(IllegalStateException::class)
    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (mRecyclerView === recyclerView) {
            return  // nothing to do
        }
        if (mRecyclerView != null) {
            destroyCallbacks()
        }
        mRecyclerView = recyclerView
        if (mRecyclerView != null) {
            val layoutManager = mRecyclerView!!.layoutManager as? BannerLayoutManager ?: return

            setupCallbacks()
            mGravityScroller = Scroller(mRecyclerView!!.context, DecelerateInterpolator())

            snapToCenterView(layoutManager, layoutManager.onPageChangeListener)
        }
    }

    fun snapToCenterView(layoutManager: BannerLayoutManager?, listener: BannerLayoutManager.OnPageChangeListener?) {
        val delta = layoutManager!!.offsetToCenter
        if (delta != 0) {
            if (layoutManager.orientation == BannerLayoutManager.VERTICAL) mRecyclerView!!.smoothScrollBy(0, delta)
            else mRecyclerView!!.smoothScrollBy(delta, 0)
        } else {
            // set it false to make smoothScrollToPosition keep trigger the listener
            snapToCenter = false
        }

        listener?.onPageSelected(layoutManager.currentPosition)
    }

    /**
     * Called when an instance of a [RecyclerView] is attached.
     */
    @Throws(IllegalStateException::class)
    fun setupCallbacks() {
        check(mRecyclerView!!.onFlingListener == null) { "An instance of OnFlingListener already set." }
        mRecyclerView!!.addOnScrollListener(mScrollListener)
        mRecyclerView!!.onFlingListener = this
    }

    /**
     * Called when the instance of a [RecyclerView] is detached.
     */
    fun destroyCallbacks() {
        mRecyclerView!!.removeOnScrollListener(mScrollListener)
        mRecyclerView!!.onFlingListener = null
    }
}
