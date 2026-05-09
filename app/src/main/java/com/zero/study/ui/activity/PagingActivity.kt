package com.zero.study.ui.activity

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zero.base.activity.BaseActivity
import com.zero.base.ext.toast
import com.zero.study.databinding.ActivityRoomBinding
import com.zero.study.db.DbManager
import com.zero.study.db.UserDao
import com.zero.study.db.entity.User
import com.zero.study.ui.adapter.PagingAdapter
import com.zero.study.ui.adapter.PagingStateAdapter
import com.zero.study.ui.model.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PagingActivity : BaseActivity<ActivityRoomBinding>(ActivityRoomBinding::inflate) {
    override val useRootWindowInsetsPadding: Boolean = false

    private val userDao: UserDao by lazy { DbManager.db.userDao() }
    private val viewModel by lazy {
        ViewModelProvider(this)[UserViewModel::class.java]
    }
    private val userAdapter by lazy {
        PagingAdapter()
    }

    override fun initView() {
        applySystemBarPadding(binding.actionBarContainer, top = true)
        applyRecyclerViewInsets(binding.resultRecycle, bottom = true)
        val stateAdapter = userAdapter.withLoadStateFooter(PagingStateAdapter(userAdapter::retry))
        binding.resultRecycle.let {
            it.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
            it.adapter = stateAdapter
        }
    }

    override fun initData() {

        lifecycleScope.launch {
            userAdapter.loadStateFlow.collectLatest { loadStates ->
                Log.d("ccc", "loadStates:${loadStates.source}")
            }
        }
        userAdapter.addLoadStateListener {
            when (it.prepend) {
                is LoadState.Loading -> Log.d("zzz", "Loading")

                is LoadState.Error -> Log.d("zzz", "Error")

                is LoadState.NotLoading -> {
                    binding.swipeLayout.isRefreshing = false
                    Log.d("zzz", "NotLoading")
                }
            }
        }
        lifecycleScope.launch {
            viewModel.userFlow.collectLatest {
                userAdapter.submitData(it)
            }
        }
    }

    override fun addListener() {
        binding.swipeLayout.setOnRefreshListener { userAdapter.refresh() }
        binding.insert.setOnClickListener { insertAll() }
        binding.queryPaging.setOnClickListener {
            userAdapter.refresh()
        }
        userAdapter.onDeleteClickListener = { user: User ->
            lifecycleScope.launch(Dispatchers.IO) {
                userDao.deleteSingle(user)
                withContext(Dispatchers.Main) {
                    this@PagingActivity.toast("删除单条数据成功")
                }
            }
        }
    }


    /**
     * 默认插入批量数据
     */
    private fun insertAll() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (userDao.queryAllUser().size == 0) {
                val mutableList: MutableList<User> = mutableListOf()
                for (a in 1..100) {
                    val user = User("分页数据$a", a, "碧桂园${a}栋", "")
                    mutableList.add(user)
                }
                val count = userDao.insertUserList(mutableList)
                withContext(Dispatchers.Main) {
                    this@PagingActivity.toast("批量新增${count}条数据")
                }
            }
        }
    }


}
