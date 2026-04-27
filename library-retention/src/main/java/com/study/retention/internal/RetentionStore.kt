package com.study.retention.internal

import android.content.Context
import com.tencent.mmkv.MMKV
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class RetentionStore(context: Context) {

    private val storage by lazy { openStorage(context.applicationContext) }

    fun getLastShownAt(trigger: RetentionTriggerType): Long {
        return storage.decodeLong(lastShownKey(trigger), 0L)
    }

    fun getTodayCount(trigger: RetentionTriggerType): Int {
        resetCountersIfNeeded()
        return storage.decodeInt(todayCountKey(trigger), 0)
    }

    fun recordShown(trigger: RetentionTriggerType) {
        resetCountersIfNeeded()
        val countKey = todayCountKey(trigger)
        val now = System.currentTimeMillis()
        storage.encode(lastShownKey(trigger), now)
        storage.encode(countKey, storage.decodeInt(countKey, 0) + 1)
        storage.encode(KEY_GLOBAL_LAST_SHOWN_AT, now)
    }

    fun getGlobalLastShownAt(): Long = storage.decodeLong(KEY_GLOBAL_LAST_SHOWN_AT, 0L)

    fun getNextScheduledAlarmAt(): Long = storage.decodeLong(KEY_NEXT_ALARM_AT, 0L)

    fun setNextScheduledAlarmAt(value: Long) {
        storage.encode(KEY_NEXT_ALARM_AT, value)
    }

    fun advanceBucketCursor(size: Int): Int {
        if (size <= 0) {
            return 0
        }
        val current = storage.decodeInt(KEY_BUCKET_CURSOR, -1)
        val next = (current + 1) % size
        storage.encode(KEY_BUCKET_CURSOR, next)
        return next
    }

    fun setBucketCursor(index: Int) {
        storage.encode(KEY_BUCKET_CURSOR, index)
    }

    fun nextBucketRotationIndex(bucketId: Int, size: Int): Int {
        if (size <= 0) {
            return 0
        }
        val key = "${KEY_BUCKET_ROTATION_PREFIX}$bucketId"
        val current = storage.decodeInt(key, 0)
        storage.encode(key, (current + 1) % size)
        return current.coerceIn(0, size - 1)
    }

    fun nextNaturalReminderIndex(size: Int): Int {
        if (size <= 0) {
            return 0
        }
        val current = storage.decodeInt(KEY_NATURAL_INDEX, 0)
        storage.encode(KEY_NATURAL_INDEX, (current + 1) % size)
        return current.coerceIn(0, size - 1)
    }

    private fun resetCountersIfNeeded() {
        val today = dayKey(System.currentTimeMillis())
        val stored = storage.decodeString(KEY_DAY_MARKER, null)
        if (stored == today) {
            return
        }
        storage.encode(KEY_DAY_MARKER, today)
        RetentionTriggerType.entries.forEach { trigger ->
            storage.encode(todayCountKey(trigger), 0)
        }
    }

    private fun dayKey(timestamp: Long): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestamp))
    }

    private fun todayCountKey(trigger: RetentionTriggerType): String {
        return "${KEY_COUNT_PREFIX}_${trigger.policyKey}"
    }

    private fun lastShownKey(trigger: RetentionTriggerType): String {
        return "${KEY_LAST_PREFIX}_${trigger.policyKey}"
    }

    private companion object {
        private const val KEY_BUCKET_CURSOR = "bucket_cursor"
        private const val KEY_BUCKET_ROTATION_PREFIX = "bucket_rotation_"
        private const val KEY_COUNT_PREFIX = "count"
        private const val KEY_DAY_MARKER = "day_marker"
        private const val KEY_GLOBAL_LAST_SHOWN_AT = "global_last_shown_at"
        private const val KEY_LAST_PREFIX = "last"
        private const val KEY_NATURAL_INDEX = "natural_index"
        private const val KEY_NEXT_ALARM_AT = "next_alarm_at"
        private const val STORAGE_ID = "library_retention_state"

        private fun openStorage(context: Context): MMKV {
            MMKV.initialize(context)
            return requireNotNull(MMKV.mmkvWithID(STORAGE_ID, MMKV.SINGLE_PROCESS_MODE)) {
                "Failed to open MMKV storage for library-retention."
            }
        }
    }
}
