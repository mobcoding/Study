package com.zero.base.ext

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @date:2025/8/7 15:36
 * @path:com.zero.base.ext.Time
 */
val Date.formatHourMinute: String
    get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(this)

val Date.formatYearMouthDayHourMinute: String
    get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(this)
