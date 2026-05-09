package com.zero.health.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.zero.health.bean.RemindType
import com.zero.health.helper.AlarmHelper
import com.zero.health.helper.AlarmSetter
import com.zero.health.helper.NotifyHelper

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("zzz", "onReceive: ${intent.action}")
        if (intent.action == AlarmHelper.ACTION_FIRED) {
            val id = intent.getIntExtra(AlarmHelper.EXTRA_ID, -1)
            val type = intent.getStringExtra(AlarmHelper.EXTRA_TYPE)
            NotifyHelper.showAlarmNotification(context, RemindType.BLOOD_PRESSURE)
        }
    }
}
