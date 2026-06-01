package com.rebuild.mixtube.ads

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ContentLoadingProgressBar

class SplashActivity : AppCompatActivity() {
    private lateinit var progressBar: ContentLoadingProgressBar
    private lateinit var progressText: TextView
    private var finished = false
    private var startupAdAttempted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        progressBar = findViewById(R.id.splash_progress)
        progressText = findViewById(R.id.tv_percent_name)

        val summary = ServiceLocator.adManager.bootstrapFromRemoteConfig()
        val duration = ServiceLocator.adManager.startupDurationMillis()
        Log.d(TAG, "bootstrap=$summary")
        Log.d(TAG, "startupScenes=${ServiceLocator.adManager.startupScenes()}")

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
        startupAdAttempted = ServiceLocator.adManager.hasStartupPresentation()
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtras(getIntent()?.extras ?: Bundle())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(MainActivity.EXTRA_SKIP_STARTUP_ENTRY_AD, startupAdAttempted)
        }
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
