package com.zero.study.ui.activity

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.zero.base.activity.BaseActivity
import com.zero.base.ext.toast
import com.zero.study.databinding.ActivityRoomBinding
import com.zero.study.db.DbManager
import com.zero.study.db.DogDao
import com.zero.study.db.UserDao
import com.zero.study.db.entity.Dog
import com.zero.study.db.entity.User
import com.zero.study.ui.adapter.UserAdapter
import com.zero.study.ui.dialog.Dialog
import com.zero.study.ui.model.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


class RoomActivity : BaseActivity<ActivityRoomBinding>(ActivityRoomBinding::inflate) {
    override val useRootWindowInsetsPadding: Boolean = false

    private val TAG = RoomActivity::class.java.simpleName
    private var list = mutableListOf<User>()
    private val userDao: UserDao by lazy { DbManager.db.userDao() }
    private val dogDao: DogDao by lazy { DbManager.db.dogDao() }
    private val viewModel by lazy {
        ViewModelProvider(this)[UserViewModel::class.java]
    }
    private val userAdapter by lazy {
        UserAdapter()
    }

    override fun initView() {
        applySystemBarPadding(binding.actionBarContainer, top = true)
        applyRecyclerViewInsets(binding.resultRecycle, bottom = true)
        binding.resultRecycle.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.resultRecycle.adapter = userAdapter
    }

    override fun initData() {
        insertAll()
        val userList: LiveData<MutableList<User>> = userDao.getAllUsers()
        userList.observe(this@RoomActivity) { users: MutableList<User> ->
            userAdapter.submitList(users)
        }
    }

    override fun addListener() {
        binding.query.setOnClickListener { query() }
        binding.insert.setOnClickListener { insertSingle() }
        binding.delete.setOnClickListener { delete() }
        binding.update.setOnClickListener { update() }
        userAdapter.onDeleteClickListener = { user: User ->
            singleDel(user)
        }
        binding.queryFuzzy.setOnClickListener {
            Dialog.Builder().setTitle("Fuzzy Query").setCancelText("Cancel").setConfirmText("OK").setCancelOnTouchOutSide(false).setOnClickListener { input ->
                query(keyWord = input)
            }.build().show(supportFragmentManager, "CreateFolderDialog")
        }
        binding.swipeLayout.setOnRefreshListener { query() }
        binding.queryPaging.setOnClickListener {

        }
    }

    /**
     * 查询
     */

    private fun query(keyWord: String = "") {
        lifecycleScope.launch(Dispatchers.IO) {
            list = if (keyWord.isEmpty()) {
                userDao.queryAllUser()
            } else {
                userDao.queryFuzzyByName("%$keyWord%")
            }
          val userDog =  userDao.queryUserDogs()
            Log.d(TAG, Gson().toJson(userDog))
            withContext(Dispatchers.Main) {
                userAdapter.submitList(list)
                binding.swipeLayout.isRefreshing = false
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
                for (a in 1..3) {
                    val user = User("张三$a", 20 + a, "深圳市龙华区$a", "")
                    mutableList.add(user)
                }
                for (a in 4..10) {
                    val user = User("王二$a", 20 + a, "深圳市宝安区$a", "")
                    mutableList.add(user)
                }
                val count = userDao.insertUserList(mutableList)
                withContext(Dispatchers.Main) {
                    this@RoomActivity.toast("批量新增${count}条数据")
                }
            }

            query()
        }
    }

    /**
     * 插入单条数据
     */
    private fun insertSingle() {
        val age = Random.nextInt(20, 99)
        val user = User("小米", age, "东莞", "")
        lifecycleScope.launch(Dispatchers.IO) {
            val count = userDao.addUser(user)

            val dog = Dog(ownId = 2, age = 50, name = "汪汪队")
            dogDao.insertDog(dog, dog)
            withContext(Dispatchers.Main) {
                this@RoomActivity.toast("新增${count}条数据成功")
            }
        }
    }

    /**
     * 删除表里的所有数据
     */
    private fun delete() {
        lifecycleScope.launch(Dispatchers.IO) {
            val count = userDao.deleteAllUser()
            withContext(Dispatchers.Main) {
                this@RoomActivity.toast("删除${count}条数据")
            }
            query()
        }
    }

    /**
     * 更新所有数据
     */
    private fun update() {
        lifecycleScope.launch(Dispatchers.IO) {
            val count = userDao.updateAll()
            withContext(Dispatchers.Main) {
                this@RoomActivity.toast("更新${count}条数据")
            }
            query()
        }
    }

    /**
     * 删除loginUser表里的指定删除某一个
     */
    private fun singleDel(singleUser: User) {
        lifecycleScope.launch(Dispatchers.IO) {
            userDao.deleteSingle(singleUser)
            withContext(Dispatchers.Main) {
                this@RoomActivity.toast("删除单条数据成功")
            }
            query()
        }


    }

    /**
     * 修改数据表里某一个对象的字段值
     */
    private fun singleModify(user: User) {
        lifecycleScope.launch(Dispatchers.IO) {
            user.aliasName = "修改的" + user.aliasName
            user.age = 100
            user.address = "修改的地址白云区"
            userDao.updateUser(user)
            withContext(Dispatchers.Main) {
                this@RoomActivity.toast("更新单条数据成功")
            }
            list.clear()
            query()
        }
    }

}
