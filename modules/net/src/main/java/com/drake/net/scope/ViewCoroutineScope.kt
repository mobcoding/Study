

package com.drake.net.scope

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 视图作用域
 * 会在视图销毁时自动取消作用域
 */
class ViewCoroutineScope(
    val view: View,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) : NetCoroutineScope(dispatcher = dispatcher) {

    init {
        view.findViewTreeLifecycleOwner()?.lifecycle?.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) cancel()
            }
        })
    }
}
