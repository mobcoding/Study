package com.rebuild.mixtube.ads

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rebuild.mixtube.ads.R
import com.rebuild.mixtube.ads.service.LocalService

class TempActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startLocalService()
        Tracking.log(this, "temp_enter")
        setContentView(buildContent())
        maybeExitToMain()
    }

    override fun onResume() {
        super.onResume()
        maybeExitToMain()
    }

    private fun maybeExitToMain() {
        if (UserStateStore.isOrganicPassed(this)) {
            goMain("organic_passed")
            return
        }

        val install = packageManager.packageInfoSafe(packageName)
        val firstInstall = install?.firstInstallTime ?: 0L
        val lastUpdate = install?.lastUpdateTime ?: 0L
        val isFreshInstall = firstInstall > 0 && lastUpdate == firstInstall

        val windowHours = ServiceLocator.remoteConfig.getLong("organic_user_inter_time", 12L)
        val now = System.currentTimeMillis()
        val withinWindow = isFreshInstall && firstInstall > 0 && (now - firstInstall) < windowHours * 3_600_000L

        if (!withinWindow) {
            goMain("window_expired fresh=$isFreshInstall hours=$windowHours")
        } else {
            Log.d(TAG, "stay on Temp: withinWindow=$withinWindow hours=$windowHours")
        }
    }

    private fun goMain(reason: String) {
        Log.d(TAG, "Temp -> Main: $reason")
        Tracking.log(this, "temp_exit_main", mapOf("reason" to reason))
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtras(getIntent()?.extras ?: Bundle())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    private fun buildContent(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(48), dp(20), dp(24))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val title = TextView(this).apply {
            text = "Temp (A-side) Gate"
            textSize = 22f
            setTextColor(getColorCompat(R.color.white))
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "cloak_model=${ServiceLocator.remoteConfig.getLong("cloak_model", 0L)} · organic_user_inter_time=${ServiceLocator.remoteConfig.getLong("organic_user_inter_time", 12L)}h"
            textSize = 14f
            setTextColor(getColorCompat(R.color.main_small_text_color))
            setPadding(0, dp(12), 0, dp(24))
        }
        root.addView(subtitle)

        val continueBtn = Button(this).apply {
            text = "Continue to Main"
            setOnClickListener {
                UserStateStore.setOrganicPassed(this@TempActivity, true)
                Tracking.log(this@TempActivity, "temp_continue")
                goMain("user_continue")
            }
        }
        root.addView(continueBtn, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        return root
    }

    private fun startLocalService() {
        val intent = Intent(this, LocalService::class.java).setAction(LocalService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun getColorCompat(colorRes: Int): Int = resources.getColor(colorRes, theme)

    private fun PackageManager.packageInfoSafe(pkg: String) = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(pkg, 0)
        }
    } catch (_: Exception) {
        null
    }

    private companion object {
        private const val TAG = "TempActivity"
    }
}
