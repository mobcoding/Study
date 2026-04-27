package com.study.trigger.internal

import android.app.Application
import android.content.Context
import com.study.trigger.ReminderTrigger
import com.tencent.mmkv.MMKV
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class RetentionStateStore(context: Context) {

    private val storage by lazy { openStorage(context.applicationContext) }

    fun getLastShownAt(trigger: ReminderTrigger): Long = storage.decodeLong(lastShownKey(trigger), 0L)

    fun getTodayCount(trigger: ReminderTrigger): Int {
        resetCountersIfNeeded()
        return storage.decodeInt(todayCountKey(trigger), 0)
    }

    fun recordShown(trigger: ReminderTrigger) {
        resetCountersIfNeeded()
        val countKey = todayCountKey(trigger)
        storage.encode(lastShownKey(trigger), System.currentTimeMillis())
        storage.encode(countKey, storage.decodeInt(countKey, 0) + 1)
    }

    fun getNextScheduledAlarmAt(): Long = storage.decodeLong(KEY_NEXT_ALARM_AT, 0L)

    fun setNextScheduledAlarmAt(value: Long) {
        storage.encode(KEY_NEXT_ALARM_AT, value)
    }

    fun getDeviceCountryCode(defaultCountryCode: String): String {
        val cached = storage.decodeString(KEY_DEVICE_COUNTRY_CODE, null)
        if (!cached.isNullOrBlank()) {
            return cached
        }
        storage.encode(KEY_DEVICE_COUNTRY_CODE, defaultCountryCode)
        return defaultCountryCode
    }

    fun resolvePreferredLocale(application: Application): Locale {
        val languageCode = storage.decodeString(KEY_FILE_SAVE_LANGUAGE, null).orEmpty()
        if (languageCode.isBlank()) {
            val locales = application.resources.configuration.locales
            return if (locales.isEmpty) Locale.getDefault() else locales[0]
        }
        return when (languageCode) {
            "zh_CN" -> Locale.SIMPLIFIED_CHINESE
            "zh_TW" -> Locale.TRADITIONAL_CHINESE
            "zh_HK" -> Locale("zh", "HK")
            else -> Locale.forLanguageTag(languageCode.replace('_', '-'))
        }
    }

    fun nextBucketId(bucketOrder: List<Int>): Int? {
        if (bucketOrder.isEmpty()) {
            return null
        }
        val currentIndex = storage.decodeInt(KEY_BUCKET_INDEX, -1)
        val nextIndex = (currentIndex + 1) % bucketOrder.size
        storage.encode(KEY_BUCKET_INDEX, nextIndex)
        return bucketOrder[nextIndex]
    }

    fun nextRotationIndex(bucketId: Int, size: Int): Int {
        if (size <= 0) {
            return 0
        }
        val key = KEY_ROTATION_INDEX_PREFIX + bucketId
        val current = storage.decodeInt(key, 0)
        storage.encode(key, (current + 1) % size)
        return current.coerceIn(0, size - 1)
    }

    private fun resetCountersIfNeeded() {
        val today = dayKey(System.currentTimeMillis())
        val stored = storage.decodeString(KEY_DAY_MARKER, null)
        if (stored == today) {
            return
        }
        storage.encode(KEY_DAY_MARKER, today)
        ReminderTrigger.entries.forEach { trigger ->
            storage.encode(todayCountKey(trigger), 0)
        }
    }

    private fun dayKey(timestamp: Long): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestamp))

    private fun todayCountKey(trigger: ReminderTrigger): String = "${KEY_COUNT_PREFIX}_${trigger.name.lowercase(Locale.US)}"

    private fun lastShownKey(trigger: ReminderTrigger): String = "${KEY_LAST_PREFIX}_${trigger.name.lowercase(Locale.US)}"

    private companion object {
        private const val KEY_BUCKET_INDEX = "bucket_index"
        private const val KEY_COUNT_PREFIX = "count"
        private const val KEY_DAY_MARKER = "day_marker"
        private const val KEY_DEVICE_COUNTRY_CODE = "device_country_code"
        private const val KEY_FILE_SAVE_LANGUAGE = "flutter.fileSaveLanguage"
        private const val KEY_LAST_PREFIX = "last"
        private const val KEY_NEXT_ALARM_AT = "next_alarm_at"
        private const val KEY_ROTATION_INDEX_PREFIX = "rotation_"
        private const val STORAGE_ID = "retention_module_state"

        private fun openStorage(context: Context): MMKV {
            MMKV.initialize(context)
            return requireNotNull(MMKV.mmkvWithID(STORAGE_ID, MMKV.SINGLE_PROCESS_MODE)) {
                "Failed to open MMKV storage for retention module."
            }
        }

    }
}
