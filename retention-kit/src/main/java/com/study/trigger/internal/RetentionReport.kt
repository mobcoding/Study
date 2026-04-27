package com.study.trigger.internal

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

object RetentionReport {

    private const val TAG = "retention_report"

    fun report(name: String, extras: Bundle? = null) {
        val bundle = bundleOf("name" to name)
        if (extras != null) {
            bundle.putAll(extras)
        }
        Firebase.analytics.logEvent(TAG, bundle)
        Log.d(TAG, "report: $bundle")
    }
}