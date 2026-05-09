package com.zero.study.ui.activity

import android.app.Activity
import android.content.Intent
import com.zero.base.activity.BaseActivity
import com.zero.study.databinding.ActivityHelpOverlayBinding

/**
 * @author Admin
 */
class PermissionOverlayHelpActivity :
    BaseActivity<ActivityHelpOverlayBinding>(ActivityHelpOverlayBinding::inflate) {

    override fun initView() {
        binding.root.setOnClickListener {
            finish()
        }
    }

    override fun initData() = Unit

    override fun addListener() = Unit

    companion object {
        @JvmStatic
        fun start(activity: Activity) {
            val intent = Intent()
            intent.setClass(activity, PermissionOverlayHelpActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
