package com.zero.widget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class WidgetGuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appwidget_guide)
        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
    }
}
