package com.study.retention

import android.content.Context
import androidx.startup.Initializer
import com.study.retention.internal.RetentionEngine

class RetentionStartupInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        RetentionEngine.initialize(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
