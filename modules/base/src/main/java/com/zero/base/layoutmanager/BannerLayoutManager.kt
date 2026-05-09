package com.zero.base.layoutmanager

import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import kotlin.math.abs

class BannerLayoutManager @JvmOverloads constructor(var orientation: Int = HORIZONTAL, var reverseLayout: Boolean = false) : RecyclerView.LayoutManager() {
    private val positionCache = SparseArray<View>()
    private var mDecoratedMeasurement: Int = 0
    private var mDecoratedMeasurementInOther: Int = 0
    var mOrientation: Int = 0
    private var mSpaceMain: Int = 0
    private var mSpaceInOther: Int = 0
    var mOffset: Float = 0f
    private var mOrientationHelper: OrientationHelper? = null
    private var mReverseLayout = false
    private var mShouldReverseLayout = false
    private var smoothScrollbarEnabled: Boolean = true
    private var mPendingScrollPosition = RecyclerView.NO_POSITION
    private var mPendingSavedState: SavedState? = null
    var mInterval: Float = 0f
    var onPageChangeListener: OnPageChangeListener? = null
    private var recycleChildrenOnDetach: Boolean = false
    private var mInfinite = true
    private var mEnableBringCenterToFront = false
    private var mLeftItems = 0
    private var mRightItems = 0
    private var mMaxVisibleItemCount = DETERMINE_BY_MAX_AND_MIN
    private var mSmoothScrollInterpolator: Interpolator? = null
    private var mDistanceToBottom = INVALID_SIZE
    private var currentFocusView: View? = null
    private var itemSpace = 20
    private var centerScale = 1.2f
    private var moveSpeed = 1.0f

    val distanceRatio: Float
        get() {
            if (moveSpeed == 0f) {
                return Float.MAX_VALUE
            }
            return 1 / moveSpeed
        }


    protected fun setInterval(): Float {
        return mDecoratedMeasurement * ((centerScale - 1) / 2 + 1) + itemSpace
    }

    fun setItemSpace(itemSpace: Int) {
        this.itemSpace = itemSpace
    }

    fun setCenterScale(centerScale: Float) {
        this.centerScale = centerScale
    }

    fun setMoveSpeed(moveSpeed: Float) {
        assertNotInLayoutOrScroll(null)
        if (this.moveSpeed == moveSpeed) {
            return
        }
        this.moveSpeed = moveSpeed
    }

    protected fun setItemViewProperty(itemView: View, targetOffset: Float) {
        val scale = calculateScale(targetOffset + mSpaceMain)
        itemView.scaleX = scale
        itemView.scaleY = scale
    }

    /**
     * @param x start positon of the view you want scale
     * @return the scale rate of current scroll mOffset
     */
    private fun calculateScale(x: Float): Float {
        val deltaX = abs((x - (mOrientationHelper!!.totalSpace - mDecoratedMeasurement) / 2f).toDouble()).toFloat()
        var diff = 0f
        if ((mDecoratedMeasurement - deltaX) > 0) {
            diff = mDecoratedMeasurement - deltaX
        }
        return (centerScale - 1f) / mDecoratedMeasurement * diff + 1
    }

    /**
     * cause elevation is not support below api 21,
     * so you can set your elevation here for supporting it below api 21
     * or you can just setElevation in [.setItemViewProperty]
     */
    protected fun setViewElevation(itemView: View?, targetOffset: Float): Float {
        return 0F
    }

    /**
     * Creates a horizontal ViewPagerLayoutManager
     */
    /**
     * @param orientation Layout orientation. Should be [.HORIZONTAL] or [.VERTICAL]
     */
    init {
        isItemPrefetchEnabled = false
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDetachedFromWindow(view: RecyclerView, recycler: Recycler) {
        super.onDetachedFromWindow(view, recycler)
        if (recycleChildrenOnDetach) {
            removeAndRecycleAllViews(recycler)
            recycler.clear()
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        if (mPendingSavedState != null) {
            return SavedState(mPendingSavedState!!)
        }
        val savedState = SavedState()
        savedState.position = mPendingScrollPosition
        savedState.offset = mOffset
        savedState.isReverseLayout = mShouldReverseLayout
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            mPendingSavedState = SavedState(state)
            requestLayout()
        }
    }

    /**
     * @return true if [.getOrientation] is [.HORIZONTAL]
     */
    override fun canScrollHorizontally(): Boolean {
        return mOrientation == HORIZONTAL
    }

    /**
     * @return true if [.getOrientation] is [.VERTICAL]
     */
    override fun canScrollVertically(): Boolean {
        return mOrientation == VERTICAL
    }

    var maxVisibleItemCount: Int
        /**
         * Returns the max visible item count, [.DETERMINE_BY_MAX_AND_MIN] means it haven't been set now
         * And it will use [.maxRemoveOffset] and [.minRemoveOffset] to handle the range
         *
         * @return Max visible item count
         */
        get() = mMaxVisibleItemCount
        /**
         * Set the max visible item count, [.DETERMINE_BY_MAX_AND_MIN] means it haven't been set now
         * And it will use [.maxRemoveOffset] and [.minRemoveOffset] to handle the range
         *
         * @param mMaxVisibleItemCount Max visible item count
         */
        set(mMaxVisibleItemCount) {
            assertNotInLayoutOrScroll(null)
            if (this.mMaxVisibleItemCount == mMaxVisibleItemCount) {
                return
            }
            this.mMaxVisibleItemCount = mMaxVisibleItemCount
            removeAllViews()
        }

    /**
     * Calculates the view layout order. (e.g. from end to start or start to end)
     * RTL layout support is applied automatically. So if layout is RTL and
     * [.getReverseLayout] is `true`, elements will be laid out starting from left.
     */
    private fun resolveShouldLayoutReverse() {
        if (mOrientation == HORIZONTAL && layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            mReverseLayout = !mReverseLayout
        }
    }

    fun setSmoothScrollInterpolator(smoothScrollInterpolator: Interpolator?) {
        this.mSmoothScrollInterpolator = smoothScrollInterpolator
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
        val offsetPosition = getOffsetToPosition(position)
        if (mOrientation == VERTICAL) {
            recyclerView.smoothScrollBy(0, offsetPosition, mSmoothScrollInterpolator)
        } else {
            recyclerView.smoothScrollBy(offsetPosition, 0, mSmoothScrollInterpolator)
        }
    }

    override fun scrollToPosition(position: Int) {
        if (!mInfinite && (position < 0 || position >= itemCount)) {
            return
        }
        mPendingScrollPosition = position
        mOffset = if (mShouldReverseLayout) position * -mInterval else position * mInterval
        requestLayout()
    }

    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        if (state.itemCount == 0) {
            removeAndRecycleAllViews(recycler)
            mOffset = 0f
            return
        }

        ensureLayoutState()
        resolveShouldLayoutReverse()

        //make sure properties are correct while measure more than once
        val scrap = recycler.getViewForPosition(0)
        measureChildWithMargins(scrap, 0, 0)
        mDecoratedMeasurement = mOrientationHelper!!.getDecoratedMeasurement(scrap)
        mDecoratedMeasurementInOther = mOrientationHelper!!.getDecoratedMeasurementInOther(scrap)
        mSpaceMain = (mOrientationHelper!!.totalSpace - mDecoratedMeasurement) / 2
        mSpaceInOther = if (mDistanceToBottom == INVALID_SIZE) {
            (totalSpaceInOther - mDecoratedMeasurementInOther) / 2
        } else {
            totalSpaceInOther - mDecoratedMeasurementInOther - mDistanceToBottom
        }

        mInterval = setInterval()
        setUp()
        mLeftItems = abs((minRemoveOffset() / mInterval).toDouble()).toInt() + 1
        mRightItems = abs((maxRemoveOffset() / mInterval).toDouble()).toInt() + 1

        if (mPendingSavedState != null) {
            mShouldReverseLayout = mPendingSavedState!!.isReverseLayout
            mPendingScrollPosition = mPendingSavedState!!.position
            mOffset = mPendingSavedState!!.offset
        }

        if (mPendingScrollPosition != RecyclerView.NO_POSITION) {
            mOffset = if (mShouldReverseLayout) mPendingScrollPosition * -mInterval else mPendingScrollPosition * mInterval
        }

        detachAndScrapAttachedViews(recycler)
        layoutItems(recycler)
    }

    val totalSpaceInOther: Int
        get() = if (mOrientation == HORIZONTAL) {
            (height - paddingTop - paddingBottom)
        } else {
            (width - paddingLeft - paddingRight)
        }

    override fun onLayoutCompleted(state: RecyclerView.State) {
        super.onLayoutCompleted(state)
        mPendingSavedState = null
        mPendingScrollPosition = RecyclerView.NO_POSITION
    }

    override fun onAddFocusables(recyclerView: RecyclerView, views: ArrayList<View>, direction: Int, focusableMode: Int): Boolean {
        val currentPosition = currentPosition
        val currentView = findViewByPosition(currentPosition) ?: return true
        if (recyclerView.hasFocus()) {
            val movement = getMovement(direction)
            if (movement != DIRECTION_NO_WHERE) {
                val targetPosition = if (movement == DIRECTION_BACKWARD) currentPosition - 1 else currentPosition + 1
                recyclerView.smoothScrollToPosition(targetPosition)
            }
        } else {
            currentView.addFocusables(views, direction, focusableMode)
        }
        return true
    }

    override fun onFocusSearchFailed(focused: View, focusDirection: Int, recycler: Recycler, state: RecyclerView.State): View? {
        return null
    }

    private fun getMovement(direction: Int): Int {
        return if (mOrientation == VERTICAL) {
            if (direction == View.FOCUS_UP) {
                if (mShouldReverseLayout) DIRECTION_FORWARD else DIRECTION_BACKWARD
            } else if (direction == View.FOCUS_DOWN) {
                if (mShouldReverseLayout) DIRECTION_BACKWARD else DIRECTION_FORWARD
            } else {
                DIRECTION_NO_WHERE
            }
        } else {
            if (direction == View.FOCUS_LEFT) {
                if (mShouldReverseLayout) DIRECTION_FORWARD else DIRECTION_BACKWARD
            } else if (direction == View.FOCUS_RIGHT) {
                if (mShouldReverseLayout) DIRECTION_BACKWARD else DIRECTION_FORWARD
            } else {
                DIRECTION_NO_WHERE
            }
        }
    }

    fun ensureLayoutState() {
        if (mOrientationHelper == null) {
            mOrientationHelper = OrientationHelper.createOrientationHelper(this, mOrientation)
        }
    }

    /**
     * You can set up your own properties here or change the exist properties like mSpaceMain and mSpaceInOther
     */
    protected fun setUp() {
    }

    private fun getProperty(position: Int): Float {
        return if (mShouldReverseLayout) position * -mInterval else position * mInterval
    }

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        removeAllViews()
        mOffset = 0f
    }


    override fun computeHorizontalScrollOffset(state: RecyclerView.State): Int {
        return computeScrollOffset()
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        return computeScrollOffset()
    }

    override fun computeHorizontalScrollExtent(state: RecyclerView.State): Int {
        return computeScrollExtent()
    }

    override fun computeVerticalScrollExtent(state: RecyclerView.State): Int {
        return computeScrollExtent()
    }

    override fun computeHorizontalScrollRange(state: RecyclerView.State): Int {
        return computeScrollRange()
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State): Int {
        return computeScrollRange()
    }

    private fun computeScrollOffset(): Int {
        if (childCount == 0) {
            return 0
        }

        if (!smoothScrollbarEnabled) {
            return if (!mShouldReverseLayout) currentPosition else itemCount - currentPosition - 1
        }

        val realOffset = offsetOfRightAdapterPosition
        return if (!mShouldReverseLayout) realOffset.toInt() else ((itemCount - 1) * mInterval + realOffset).toInt()
    }

    private fun computeScrollExtent(): Int {
        if (childCount == 0) {
            return 0
        }

        if (!smoothScrollbarEnabled) {
            return 1
        }

        return mInterval.toInt()
    }

    private fun computeScrollRange(): Int {
        if (childCount == 0) {
            return 0
        }

        if (!smoothScrollbarEnabled) {
            return itemCount
        }

        return (itemCount * mInterval).toInt()
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: Recycler, state: RecyclerView.State): Int {
        if (mOrientation == VERTICAL) {
            return 0
        }
        return scrollBy(dx, recycler, state)
    }

    override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: RecyclerView.State): Int {
        if (mOrientation == HORIZONTAL) {
            return 0
        }
        return scrollBy(dy, recycler, state)
    }

    private fun scrollBy(dy: Int, recycler: Recycler, state: RecyclerView.State): Int {
        if (childCount == 0 || dy == 0) {
            return 0
        }
        ensureLayoutState()
        var willScroll = dy

        var realDx = dy / distanceRatio
        if (abs(realDx.toDouble()) < 0.00000001f) {
            return 0
        }
        val targetOffset = mOffset + realDx

        //handle the boundary
        if (!mInfinite && targetOffset < minOffset) {
            willScroll = (willScroll - (targetOffset - minOffset) * this.distanceRatio).toInt()
        } else if (!mInfinite && targetOffset > maxOffset) {
            willScroll = ((maxOffset - mOffset) * this.distanceRatio).toInt()
        }

        realDx = willScroll / distanceRatio

        mOffset += realDx

        //handle recycle
        layoutItems(recycler)

        return willScroll
    }

    private fun layoutItems(recycler: Recycler) {
        detachAndScrapAttachedViews(recycler)
        positionCache.clear()

        val itemCount = itemCount
        if (itemCount == 0) {
            return
        }

        // make sure that current position start from 0 to 1
        val currentPos = if (mShouldReverseLayout) -currentPositionOffset else currentPositionOffset
        var start = currentPos - mLeftItems
        var end = currentPos + mRightItems

        // handle max visible count
        if (useMaxVisibleCount()) {
            val isEven = mMaxVisibleItemCount % 2 == 0
            if (isEven) {
                val offset = mMaxVisibleItemCount / 2
                start = currentPos - offset + 1
                end = currentPos + offset + 1
            } else {
                val offset = (mMaxVisibleItemCount - 1) / 2
                start = currentPos - offset
                end = currentPos + offset + 1
            }
        }

        if (!mInfinite) {
            if (start < 0) {
                start = 0
                if (useMaxVisibleCount()) {
                    end = mMaxVisibleItemCount
                }
            }
            if (end > itemCount) {
                end = itemCount
            }
        }

        var lastOrderWeight = Float.MIN_VALUE
        for (i in start until end) {
            if (useMaxVisibleCount() || !removeCondition(getProperty(i) - mOffset)) {
                // start and end base on current position,
                // so we need to calculate the adapter position
                var adapterPosition = i
                if (i >= itemCount) {
                    adapterPosition %= itemCount
                } else if (i < 0) {
                    var delta = (-adapterPosition) % itemCount
                    if (delta == 0) delta = itemCount
                    adapterPosition = itemCount - delta
                }
                val scrap = recycler.getViewForPosition(adapterPosition)
                measureChildWithMargins(scrap, 0, 0)
                resetViewProperty(scrap)
                // we need i to calculate the real offset of current view
                val targetOffset = getProperty(i) - mOffset
                layoutScrap(scrap, targetOffset)
                val orderWeight = if (mEnableBringCenterToFront) setViewElevation(scrap, targetOffset) else adapterPosition.toFloat()
                if (orderWeight > lastOrderWeight) {
                    addView(scrap)
                } else {
                    addView(scrap, 0)
                }
                if (i == currentPos) {
                    currentFocusView = scrap
                }
                lastOrderWeight = orderWeight
                positionCache.put(i, scrap)
            }
        }

        currentFocusView!!.requestFocus()
    }

    private fun useMaxVisibleCount(): Boolean {
        return mMaxVisibleItemCount != DETERMINE_BY_MAX_AND_MIN
    }

    private fun removeCondition(targetOffset: Float): Boolean {
        return targetOffset > maxRemoveOffset() || targetOffset < minRemoveOffset()
    }

    private fun resetViewProperty(v: View) {
        v.rotation = 0f
        v.rotationY = 0f
        v.rotationX = 0f
        v.scaleX = 1f
        v.scaleY = 1f
        v.alpha = 1f
    }

    val maxOffset: Float
        get() = if (!mShouldReverseLayout) (itemCount - 1) * mInterval else 0f

    val minOffset: Float
        get() = if (!mShouldReverseLayout) 0f else -(itemCount - 1) * mInterval

    private fun layoutScrap(scrap: View, targetOffset: Float) {
        val left = calItemLeft(scrap, targetOffset)
        val top = calItemTop(scrap, targetOffset)
        if (mOrientation == VERTICAL) {
            layoutDecorated(scrap, mSpaceInOther + left, mSpaceMain + top, mSpaceInOther + left + mDecoratedMeasurementInOther, mSpaceMain + top + mDecoratedMeasurement)
        } else {
            layoutDecorated(scrap, mSpaceMain + left, mSpaceInOther + top, mSpaceMain + left + mDecoratedMeasurement, mSpaceInOther + top + mDecoratedMeasurementInOther)
        }
        setItemViewProperty(scrap, targetOffset)
    }

    protected fun calItemLeft(itemView: View?, targetOffset: Float): Int {
        return if (mOrientation == VERTICAL) 0 else targetOffset.toInt()
    }

    protected fun calItemTop(itemView: View?, targetOffset: Float): Int {
        return if (mOrientation == VERTICAL) targetOffset.toInt() else 0
    }

    /**
     * when the target offset reach this,
     * the view will be removed and recycled in [.layoutItems]
     */
    protected fun maxRemoveOffset(): Float {
        return (mOrientationHelper!!.totalSpace - mSpaceMain).toFloat()
    }

    /**
     * when the target offset reach this,
     * the view will be removed and recycled in [.layoutItems]
     */
    private fun minRemoveOffset(): Float {
        return (-mDecoratedMeasurement - mOrientationHelper!!.startAfterPadding - mSpaceMain).toFloat()
    }


    val currentPosition: Int
        get() {
            if (itemCount == 0) {
                return 0
            }

            var position = currentPositionOffset
            if (!mInfinite) {
                return abs(position.toDouble()).toInt()
            }

            position = if (!mShouldReverseLayout) //take care of position = getItemCount()
                (if (position >= 0) position % itemCount else itemCount + position % itemCount) else (if (position > 0) itemCount - position % itemCount else -position % itemCount)
            return if (position == itemCount) 0 else position
        }

    override fun findViewByPosition(position: Int): View? {
        val itemCount = itemCount
        if (itemCount == 0) {
            return null
        }
        for (i in 0 until positionCache.size()) {
            val key = positionCache.keyAt(i)
            if (key >= 0) {
                if (position == key % itemCount) {
                    return positionCache.valueAt(i)
                }
            } else {
                var delta = key % itemCount
                if (delta == 0) {
                    delta = -itemCount
                }
                if (itemCount + delta == position) {
                    return positionCache.valueAt(i)
                }
            }
        }
        return null
    }

    private val currentPositionOffset: Int
        get() = Math.round(mOffset / mInterval)

    private val offsetOfRightAdapterPosition: Float
        /**
         * Sometimes we need to get the right offset of matching adapter position
         * cause when [.mInfinite] is set true, there will be no limitation of [.mOffset]
         */
        get() = if (mShouldReverseLayout) if (mInfinite) (if (mOffset <= 0) mOffset % (mInterval * itemCount) else itemCount * -mInterval + mOffset % (mInterval * itemCount)) else mOffset
        else if (mInfinite) (if (mOffset >= 0) mOffset % (mInterval * itemCount) else itemCount * mInterval + mOffset % (mInterval * itemCount)) else mOffset

    val offsetToCenter: Int
        /**
         * @return the dy between center and current position
         */
        get() {
            if (mInfinite) return ((currentPositionOffset * mInterval - mOffset) * this.distanceRatio).toInt()
            return ((currentPosition * (if (!mShouldReverseLayout) mInterval else -mInterval) - mOffset) * this.distanceRatio).toInt()
        }

    private fun getOffsetToPosition(position: Int): Int {
        if (mInfinite) return (((currentPositionOffset + (if (!mShouldReverseLayout) position - currentPosition else currentPosition - position)) * mInterval - mOffset) * this.distanceRatio).toInt()
        return ((position * (if (!mShouldReverseLayout) mInterval else -mInterval) - mOffset) * this.distanceRatio).toInt()
    }

    var infinite: Boolean
        get() = mInfinite
        set(enable) {
            assertNotInLayoutOrScroll(null)
            if (enable == mInfinite) {
                return
            }
            mInfinite = enable
            requestLayout()
        }

    var distanceToBottom: Int
        get() = if (mDistanceToBottom == INVALID_SIZE) (totalSpaceInOther - mDecoratedMeasurementInOther) / 2 else mDistanceToBottom
        set(mDistanceToBottom) {
            assertNotInLayoutOrScroll(null)
            if (this.mDistanceToBottom == mDistanceToBottom) return
            this.mDistanceToBottom = mDistanceToBottom
            removeAllViews()
        }

    var enableBringCenterToFront: Boolean
        get() = mEnableBringCenterToFront
        set(bringCenterToTop) {
            assertNotInLayoutOrScroll(null)
            if (mEnableBringCenterToFront == bringCenterToTop) {
                return
            }
            this.mEnableBringCenterToFront = bringCenterToTop
            requestLayout()
        }

    private class SavedState : Parcelable {
        var position: Int = 0
        var offset: Float = 0f
        var isReverseLayout: Boolean = false

        constructor(parcel: Parcel) : this() {
            position = parcel.readInt()
            offset = parcel.readFloat()
            isReverseLayout = parcel.readByte() != 0.toByte()
        }

        constructor()

        constructor(other: SavedState) {
            position = other.position
            offset = other.offset
            isReverseLayout = other.isReverseLayout
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(position)
            dest.writeFloat(offset)
            dest.writeInt(if (isReverseLayout) 1 else 0)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }

    }

    interface OnPageChangeListener {
        fun onPageSelected(position: Int)

        fun onPageScrollStateChanged(state: Int)
    }

    companion object {
        const val DETERMINE_BY_MAX_AND_MIN: Int = -1

        const val HORIZONTAL: Int = OrientationHelper.HORIZONTAL

        const val VERTICAL: Int = OrientationHelper.VERTICAL

        private const val DIRECTION_NO_WHERE = -1

        private const val DIRECTION_FORWARD = 0

        private const val DIRECTION_BACKWARD = 1

        private const val INVALID_SIZE: Int = Int.MAX_VALUE
    }
}
