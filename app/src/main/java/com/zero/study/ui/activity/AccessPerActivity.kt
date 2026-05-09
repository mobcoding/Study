package com.zero.study.ui.activity

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.zero.base.activity.BaseActivity
import com.zero.study.bean.ImageModel
import com.zero.study.databinding.ActivityAccessPerBinding
import com.zero.study.ui.adapter.AlbumAdapter
import kotlin.concurrent.thread
import android.view.ViewTreeObserver.OnPreDrawListener as OnPreDrawListener1

class AccessPerActivity : BaseActivity<ActivityAccessPerBinding>(ActivityAccessPerBinding::inflate) {

    private val adapter = AlbumAdapter()
    private val imageModelList = ArrayList<ImageModel>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            checkPermission()
            loadImages()
        }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionLauncher.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED))
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(READ_MEDIA_IMAGES))
        } else {
            permissionLauncher.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }
    }

    override fun initView() {
        applyRecyclerViewInsets(binding.recyclerView, bottom = true)

        binding.button.setOnClickListener {
            requestPermissions()
        }

        binding.recyclerView.viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener1 {
            override fun onPreDraw(): Boolean {
                binding.recyclerView.viewTreeObserver.removeOnPreDrawListener(this)
                binding.recyclerView.layoutManager = GridLayoutManager(this@AccessPerActivity, 3)
                binding.recyclerView.adapter = adapter
                loadImages()
                return false
            }
        })

        checkPermission()
    }

    override fun initData() = Unit

    override fun addListener() = Unit

    private fun checkPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (
                    ContextCompat.checkSelfPermission(this, READ_MEDIA_IMAGES) == PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, READ_MEDIA_VIDEO) == PERMISSION_GRANTED
                    ) -> {
                binding.cardLayout.visibility = View.GONE
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                ContextCompat.checkSelfPermission(this, READ_MEDIA_VISUAL_USER_SELECTED) == PERMISSION_GRANTED -> {
                binding.textView.text = "Partial photo access granted"
                binding.button.text = "Manage"
                binding.cardLayout.visibility = View.VISIBLE
            }

            ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED -> {
                binding.cardLayout.visibility = View.GONE
            }

            else -> {
                binding.textView.text = "Photo access is not granted"
                binding.button.text = "Request"
                binding.cardLayout.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadImages() {
        thread {
            imageModelList.clear()
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} desc"
            )
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }
                cursor.close()
            }
            runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        }
    }
}
