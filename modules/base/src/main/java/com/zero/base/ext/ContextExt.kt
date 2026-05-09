package com.zero.base.ext

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.ContextWrapper
import android.util.Log
import android.view.Gravity
import android.widget.Toast

fun Context?.toast(message: String?) {
    message?.let {
        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
    }
}

@SuppressLint("LogNotTimber")
fun String.log(tag: String) {
    Log.e(tag, this)
}


fun Context.toastLong(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Context.toastCenter(message: String) {
    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).apply {
        setGravity(Gravity.CENTER, 0, 0)
        show()
    }
}

fun Context.getActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun <T> Context.isServiceRunning(serviceClass: Class<T>): Boolean {
    val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}

