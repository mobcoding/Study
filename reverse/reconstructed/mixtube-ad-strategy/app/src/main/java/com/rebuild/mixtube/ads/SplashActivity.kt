package com.rebuild.mixtube.ads

import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ContentLoadingProgressBar

class SplashActivity : AppCompatActivity() {
    private lateinit var progressBar: ContentLoadingProgressBar
    private lateinit var progressText: TextView
    private var finished = false
    private var startupAdAttempted = false
    private var enterTempGate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        progressBar = findViewById(R.id.splash_progress)
        progressText = findViewById(R.id.tv_percent_name)

        val summary = ServiceLocator.adManager.bootstrapFromRemoteConfig()
        val duration = ServiceLocator.adManager.startupDurationMillis()
        enterTempGate = shouldEnterTempGate()
        Tracking.log(
            this,
            "splash_bootstrap",
            mapOf(
                "duration" to duration,
                "enterTempGate" to enterTempGate,
                "cloak_model" to ServiceLocator.remoteConfig.getLong("cloak_model", 0L),
                "organic_user_inter_time" to ServiceLocator.remoteConfig.getLong("organic_user_inter_time", 12L),
                "scenes" to ServiceLocator.adManager.startupScenes().joinToString(",")
            )
        )
        Log.d(TAG, "bootstrap=$summary")
        Log.d(TAG, "startupScenes=${ServiceLocator.adManager.startupScenes()}")
        Log.d(TAG, "enterTempGate=$enterTempGate")

        ValueAnimator.ofInt(0, 100).apply {
            this.duration = duration
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Int
                progressBar.progress = progress
                progressText.text = getString(R.string.resource_loading) + " " + progress + "%"
            }
            doOnEnd {
                navigateToBusiness()
            }
            start()
        }
    }

    private fun navigateToBusiness() {
        if (finished) return
        finished = true
        if (enterTempGate) {
            Tracking.log(this, "route_temp", mapOf("source" to "splash"))
            startActivity(
                Intent(this, TempActivity::class.java).apply {
                    putExtras(getIntent()?.extras ?: Bundle())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            finish()
            return
        }
        startupAdAttempted = ServiceLocator.adManager.hasStartupPresentation()
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtras(getIntent()?.extras ?: Bundle())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(MainActivity.EXTRA_SKIP_STARTUP_ENTRY_AD, startupAdAttempted)
        }
        Tracking.log(this, "route_main", mapOf("startupAdAttempted" to startupAdAttempted))
        PostAdNavigationManager.clear()
        PostAdNavigationManager.setPending(intent)
        if (startupAdAttempted) {
            ServiceLocator.adManager.presentStartupAd(this)
            Log.d(TAG, "startup ad is presenting before main")
            finish()
            return
        }
        PostAdNavigationManager.clear()
        startActivity(intent)
        finish()
    }

    private fun shouldEnterTempGate(): Boolean {
        val cloakModel = ServiceLocator.remoteConfig.getLong("cloak_model", 0L)
        if (cloakModel == 0L) return true
        if (UserStateStore.isOrganicPassed(this)) return false

        val install = packageManager.packageInfoSafe(packageName)
        val firstInstall = install?.firstInstallTime ?: 0L
        val lastUpdate = install?.lastUpdateTime ?: 0L
        val isFreshInstall = firstInstall > 0 && lastUpdate == firstInstall

        val windowHours = ServiceLocator.remoteConfig.getLong("organic_user_inter_time", 12L)
        val now = System.currentTimeMillis()
        val withinWindow = isFreshInstall && firstInstall > 0 && (now - firstInstall) < windowHours * 3_600_000L
        return withinWindow
    }

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

    private fun ValueAnimator.doOnEnd(block: () -> Unit) {
        addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) = Unit
            override fun onAnimationEnd(animation: android.animation.Animator) = block()
            override fun onAnimationCancel(animation: android.animation.Animator) = block()
            override fun onAnimationRepeat(animation: android.animation.Animator) = Unit
        })
    }

    private companion object {
        const val TAG = "SplashActivity"
    }
}
