package com.zero.base.ext

import android.content.res.Resources
import android.util.TypedValue

val Int.dp: Int
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics).toInt()

val Float.dp: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)

val Float.sp: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, Resources.getSystem().displayMetrics)

val Float.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

