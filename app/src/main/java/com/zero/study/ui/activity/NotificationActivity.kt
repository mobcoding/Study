package com.zero.study.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.zero.base.activity.BaseActivity
import com.zero.health.bean.RemindType
import com.zero.health.helper.NotifyHelper
import com.zero.health.service.HealthService
import com.zero.study.NotificationManager
import com.zero.study.databinding.ActivityNotificationBinding

/**
 * @date:2024/9/19 18:17
 * @path:com.zero.study.ui.activity.NotificationActivity
 */
class NotificationActivity : BaseActivity<ActivityNotificationBinding>(ActivityNotificationBinding::inflate) {

    override fun initView() {
    }

    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean? ->
        if (granted == true) {
            startHealthService()
        }
    }

    private fun startHealthService() {
        ContextCompat.startForegroundService(this, Intent(this, HealthService::class.java))
    }

    private fun checkNotificationAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!notificationPermission) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startHealthService()
            }
        } else {
            startHealthService()
        }
    }

    override fun initData() {
        checkNotificationAndStartService()
    }

    override fun addListener() {
        binding.btnShow.setOnClickListener { NotificationManager.showNotification(this@NotificationActivity, "10086", "中国移动温馨提示：您的话费余额已不足，请及时充值！") }
        binding.btnShowReply.setOnClickListener { NotificationManager.showReplyNotification(this@NotificationActivity, "微信消息", "您有一条新的朋友圈动态，速去查看！") }
        binding.btnShowBanner.setOnClickListener { NotificationManager.showBannerNotification(this@NotificationActivity, "今日头条", "外交部：中方对美方安排赖清德“过境”予以严厉谴责；重要通知！今起全国推行；月内两度上调房贷利率，银行等不及了") }
        binding.btnShowCustom.setOnClickListener { NotificationManager.showMainNotification(this@NotificationActivity) }
        binding.btnBloodPressure.setOnClickListener { NotifyHelper.showAlarmNotification(this@NotificationActivity, RemindType.BLOOD_PRESSURE) }
        binding.btnHeartRate.setOnClickListener { NotifyHelper.showAlarmNotification(this@NotificationActivity, RemindType.HEART_RATE) }
        binding.btnBloodGlucose.setOnClickListener { NotifyHelper.showAlarmNotification(this@NotificationActivity, RemindType.BLOOD_GLUCOSE) }
    }

}
