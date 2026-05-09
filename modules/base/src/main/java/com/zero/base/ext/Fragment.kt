package com.zero.base.ext

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/**
 * 多个Fragment的切换
 */
fun AppCompatActivity.switchFragment(containerId: Int, fragmentToShow: Fragment) {
    supportFragmentManager.beginTransaction().apply {
        supportFragmentManager.fragments.forEach {
            if (it.isVisible) {
                hide(it)
            }
        }
        if (fragmentToShow.isAdded) {
            show(fragmentToShow)
        } else {
            add(containerId, fragmentToShow, fragmentToShow.javaClass.simpleName)
        }
        commit()
    }
}

/**
 * 单个Fragment的显示和隐藏
 */
fun AppCompatActivity.toggleFragment(containerId: Int, fragment: Fragment, tag: String, show: Boolean) {
    supportFragmentManager.beginTransaction().apply {
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)
        if (show) {
            if (existingFragment == null) {
                add(containerId, fragment, tag)
            } else {
                show(existingFragment)
            }
        } else {
            existingFragment?.let { hide(it) }
        }
        commit()
    }
}