package com.zero.study.ui.activity

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.zero.base.activity.BaseActivity
import com.zero.study.databinding.ActivitySecondBinding
import com.zero.study.db.BookDao
import com.zero.study.db.CarDao
import com.zero.study.db.DbManager
import com.zero.study.ipc.Book
import com.zero.study.ipc.IBookManager
import com.zero.study.ipc.IOnNewBookCallback
import com.zero.study.service.BookManagerService
import com.zero.study.ui.adapter.BookAdapter
import kotlinx.coroutines.launch
import java.util.Random

/**
 * @author Admin
 */
class SecondActivity : BaseActivity<ActivitySecondBinding>(ActivitySecondBinding::inflate) {
    override val useRootWindowInsetsPadding: Boolean = false

    private var bookManager: IBookManager? = null
    private var mediaProjectionManager: MediaProjectionManager? = null

    private var bookList = mutableListOf<Book>()

    private val bookAdapter by lazy {
        BookAdapter()
    }

    private fun checkNotificationAndLaunchIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!notificationPermission) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                mediaProjectionManager?.let { startMediaProjection.launch(it.createScreenCaptureIntent()) }
            }
        }
    }

    private fun checkNotificationAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!notificationPermission) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ContextCompat.startForegroundService(this@SecondActivity, Intent(this@SecondActivity, BookManagerService::class.java))
            }
        }
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.i("zzz", "service connected")
            bookManager = IBookManager.Stub.asInterface(iBinder)
            try {
                bookManager?.registerListener(listener)
                Toast.makeText(this@SecondActivity, "服务已启动", Toast.LENGTH_SHORT).show()
                binding.group.visibility = View.VISIBLE
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.i("zzz", "service connected")
            Toast.makeText(this@SecondActivity, "服务已停止", Toast.LENGTH_SHORT).show()
            binding.group.visibility = View.GONE
            bookManager = null
        }
    }
    private val listener: IOnNewBookCallback = object : IOnNewBookCallback.Stub() {
        override fun callback(newBook: Book) {
            Log.i("zzz", "NewBookArrived--->" + newBook.name)
            notifyDataChange(newBook)
        }
    }


    private var startMediaProjection: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(this@SecondActivity, BookManagerService::class.java)
                intent.putExtra("resultParcel", result)
                ContextCompat.startForegroundService(this@SecondActivity, intent)
            }
        }
    }
    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _: Boolean? ->
        ContextCompat.startForegroundService(this@SecondActivity, Intent(this@SecondActivity, BookManagerService::class.java))
    }

    override fun initView() {
        applySystemBarPadding(binding.toolbar, top = true)
        applyRecyclerViewInsets(binding.recyclerView, bottom = true)
        binding.recyclerView.adapter = bookAdapter
        binding.recyclerView.layoutManager = GridLayoutManager(this@SecondActivity, 4)
        bookAdapter.submitList(bookList)
    }

    override fun initData() {
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private fun notifyDataChange(msg: Book) {
        lifecycleScope.launch {
            bookList.add(msg)
            bookAdapter.notifyItemInserted(bookList.size - 1)
            binding.recyclerView.smoothScrollToPosition(bookAdapter.itemCount - 1)
        }
    }

    override fun addListener() {
        binding.start.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
//                checkNotificationAndStartService();
//                checkNotificationAndLaunchIntent();
                bindService(Intent(this@SecondActivity, BookManagerService::class.java), mConnection, BIND_AUTO_CREATE)
            } else {
                try {
                    unbindService(mConnection)
                    bookManager?.unregisterListener(listener)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        binding.auto.setOnClickListener {
            try {
                bookManager?.autoAdd()
            } catch (e: RemoteException) {
                throw RuntimeException(e)
            }
        }
        binding.add.setOnClickListener {
            try {
                bookManager?.addBook(Book(101, "Java 设计模式： " + Random().nextInt(50), "阿奇"))
            } catch (e: RemoteException) {
                throw RuntimeException(e)
            }
        }
        binding.get.setOnClickListener {
            try {
                val bookList = bookManager?.list
                Log.d("zzz", "addListener: ${bookList?.size}")
            } catch (e: RemoteException) {
                throw RuntimeException(e)
            }
        }
    }
}
