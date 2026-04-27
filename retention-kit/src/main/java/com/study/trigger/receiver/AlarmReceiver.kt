package com.study.trigger.receiver

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.study.trigger.ReminderTrigger
import com.study.trigger.RetentionModule
import com.study.trigger.internal.RetentionDispatcher
import com.google.firebase.FirebaseApp

internal class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val application = context?.applicationContext as? Application ?: return
        FirebaseApp.initializeApp(application)
        RetentionModule.triggerReminder(ReminderTrigger.ALARM)
        RetentionDispatcher.scheduleNextAlarmIfNeeded(force = true)
    }
}
