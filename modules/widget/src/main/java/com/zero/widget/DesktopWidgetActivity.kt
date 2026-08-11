package com.zero.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class DesktopWidgetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appwidget_widgets)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        findViewById<android.view.View>(R.id.tv_add_main).setOnClickListener { requestWidgetPin() }
        findViewById<android.view.View>(R.id.goWidgetDirections).setOnClickListener { openGuide() }
    }

    private fun requestWidgetPin() {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, StudyAppWidgetProvider::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(provider, null, null)
        } else {
            showManualAddDialog()
        }
    }

    private fun showManualAddDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.widget_add_failed)
            .setMessage(R.string.widget_add_failed_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.widget_learn_more) { _, _ -> openGuide() }
            .show()
    }

    private fun openGuide() {
        startActivity(Intent(this, WidgetGuideActivity::class.java))
    }

    companion object {
        const val EXTRA_SECTION = "com.zero.widget.extra.SECTION"
        const val SECTION_MEMORY = "memory"
        const val SECTION_STORAGE = "storage"
        const val SECTION_APPS = "apps"
        const val SECTION_CLOCK = "clock"
    }
}
