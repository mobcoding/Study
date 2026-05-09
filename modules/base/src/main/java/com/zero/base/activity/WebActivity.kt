package com.zero.base.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import com.zero.library_base.databinding.ActivityWebBinding

class WebActivity : BaseActivity<ActivityWebBinding>(ActivityWebBinding::inflate) {


    @SuppressLint("SetJavaScriptEnabled")
    override fun initView() {
        val webTitle = intent.getStringExtra(EXTRA_WEB_TITLE)
        val webUrl = intent.getStringExtra(EXTRA_WEB_URL)
        binding.tvTitle.text = webTitle
        binding.tvTitle.isSelected = true
        val webSettings: WebSettings = binding.webView.settings.apply {
            setSupportZoom(true)
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            allowContentAccess = true
            allowFileAccess = true
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        binding.webView.clearCache(true)
        val defaultUserAgent = webSettings.userAgentString
        webSettings.userAgentString = defaultUserAgent

        if (webUrl != null) {
            binding.webView.loadUrl(webUrl)
        }
    }

    override fun initData() {
    }

    override fun addListener() {
        binding.tvTitle.setOnClickListener {
            val webSettings: WebSettings = binding.webView.settings
            val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36 Edg/139.0.0.0"
            webSettings.userAgentString = desktopUserAgent
            binding.webView.loadUrl("https://baidu.com")
        }
        binding.ivBack.setOnClickListener { v: View? -> finish() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finish()
                }
            }
        })
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                Log.d("zzz", "onProgressChanged:$newProgress->${view.contentHeight} ->${view.title}  ")
            }
        }
    }

    override fun onDestroy() {
        binding.webView.clearCache(true)
        binding.webView.removeAllViews()
        binding.webView.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_WEB_TITLE: String = "extra_web_title"

        const val EXTRA_WEB_URL: String = "extra_web_url"

        fun startWebView(context: Context, title: String? = "", url: String? = "") {
            context.startActivity(Intent(context, WebActivity::class.java).apply {
                putExtra(EXTRA_WEB_TITLE, title)
                putExtra(EXTRA_WEB_URL, url)
            })
        }
    }
}
