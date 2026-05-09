package com.toolkit.admob.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.toolkit.admob.listener.OpenAdStatusListener
import com.toolkit.admob.manager.AppOpenAdManager

/**
 * @date:2024/7/23 15:14
 * @path:com.toolkit.admob.activity.OpenActivity
 */
class OpenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppOpenAdManager.showAdIfAvailable(this@OpenActivity, object : OpenAdStatusListener {
            override fun onNotReady(loadFailed: Boolean) {
                this@OpenActivity.finish()
            }

            override fun onComplete() {
                this@OpenActivity.finish()
            }

        })
    }
}
