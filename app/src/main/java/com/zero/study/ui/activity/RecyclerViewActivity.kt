package com.zero.study.ui.activity

import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.drake.net.Get
import com.drake.net.cache.CacheMode
import com.drake.net.utils.scopeNetLife
import com.zero.base.activity.BaseActivity
import com.zero.base.activity.WebActivity
import com.zero.base.net.bean.PageResponse
import com.zero.base.util.PingUtils
import com.zero.study.databinding.ActivityRecyclerViewBinding
import com.zero.study.net.entity.Article
import com.zero.study.ui.adapter.LineAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import java.net.URL
import java.util.concurrent.TimeUnit

class RecyclerViewActivity : BaseActivity<ActivityRecyclerViewBinding>(ActivityRecyclerViewBinding::inflate), OnRefreshListener {
    override val useRootWindowInsetsPadding: Boolean = false

    private var pageNo: Int = 0
    private val mAdapter: LineAdapter by lazy {
        LineAdapter { item ->
            item?.let {
                WebActivity.startWebView(this@RecyclerViewActivity, it.title, it.link)
            }
        }
    }

    override fun initView() {
        applyRecyclerViewInsets(binding.recyclerView, top = true, bottom = true)
        binding.recyclerView.adapter = mAdapter

    }

    override fun initData() {
        binding.swipeLayout.isRefreshing = true
        scopeNetLife {
            runCatching {
                val data = Get<PageResponse<Article>>("wenda/list/${pageNo}/json") {
                    setCacheControl(CacheControl.FORCE_CACHE)
                    setCacheValidTime(30, TimeUnit.DAYS)
                    setCacheMode(CacheMode.READ_THEN_REQUEST)
                }.await()
                mAdapter.submitList(data.datas)
                binding.swipeLayout.isRefreshing = false
                withContext(Dispatchers.IO) {
                    data.datas.forEachIndexed { index, article ->
                        article.delay = PingUtils.pingDelay(URL(article.link).host)
                        withContext(Dispatchers.Main) {
                            mAdapter.notifyItemChanged(index, "ping")
                        }
                    }
                }
            }.onFailure {
                binding.swipeLayout.isRefreshing = false
            }
        }

    }

    override fun addListener() {
        binding.swipeLayout.setOnRefreshListener(this)
    }


    override fun onRefresh() {
        initData()
    }


}
