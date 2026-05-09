package com.zero.base.ext

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @date:2025/7/2 15:02
 * @path:com.zero.base.ext.BitmapExt
 */

private const val TAG = "zzz"
private const val REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1001

// 保存图片到相册
fun Context.saveImageToGallery(bitmap: Bitmap): Boolean {
    // 检查权限
    if (!checkPermission(this)) {
        Log.e(TAG, "没有保存图片的权限")
        return false
    }

    // 根据不同版本执行保存操作
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveImageWithMediaStore(this, bitmap, "${generateUniqueFileName()}.jpg")
    } else {
        saveImageToPublicDirectory(this, bitmap, "${generateUniqueFileName()}.jpg")
    }
}

/**
 * 保存到外部私有目录
 */
fun Context.saveImageToPrivateDir(bitmap: Bitmap): Boolean {

    val targetDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10+ 使用 getExternalFilesDir
        File(getExternalFilesDir(null), "zzz")
    } else {
        // Android 9- 使用传统路径
        getExternalFilesDir(null)
    }
    if (targetDir?.exists() == false && !targetDir.mkdirs()) {
        Log.e(TAG, "无法创建文件夹: ${targetDir.absolutePath}")
        return false
    }
    // 创建图片文件
    val imageFile = File(targetDir, "${generateUniqueFileName()}.jpg")

    return try {
        FileOutputStream(imageFile).use { fos ->
            // 将Bitmap写入文件
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)) {
                fos.flush()
                Log.d(TAG, "图片保存成功: ${imageFile.absolutePath}")
                true
            } else {
                false
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "保存图片失败", e)
        false
    }
}

// 检查是否有保存图片的权限
fun checkPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 11及以上无需WRITE_EXTERNAL_STORAGE权限
        true
    } else {
        ContextCompat.checkSelfPermission(context,
            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

// 请求保存图片的权限
fun requestPermission(activity: FragmentActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 11及以上无需WRITE_EXTERNAL_STORAGE权限
        Log.d(TAG, "Android 11+ 无需存储权限")
    } else {
        ActivityCompat.requestPermissions(activity,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_CODE_WRITE_EXTERNAL_STORAGE)
    }
}

// Android 10及以上版本使用MediaStore保存图片
private fun saveImageWithMediaStore(context: Context, bitmap: Bitmap,
                                    displayName: String): Boolean {
    val contentResolver: ContentResolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    // 创建图片URI
    val imageUri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values)
    if (imageUri == null) {
        Log.e(TAG, "无法创建图片URI")
        return false
    }

    // 将Bitmap写入到输出流
    return try {
        contentResolver.openOutputStream(imageUri).use { outputStream ->
            if (outputStream == null) {
                Log.e(TAG, "无法打开输出流")
                return false
            }

            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                // 更新图片状态
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(imageUri, values, null, null)
                Log.d(TAG, "图片保存成功: $imageUri")
                true
            } else {
                false
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "保存图片失败", e)
        // 删除失败的图片
        contentResolver.delete(imageUri, null, null)
        false
    }
}

// Android 9及以下版本保存图片到公共目录
private fun saveImageToPublicDirectory(context: Context, bitmap: Bitmap,
                                       displayName: String): Boolean {
    // 创建保存图片的目录
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

    // 构建 ZERO 文件夹对象
    val zeroDir = File(picturesDir, "ZERO")

    // 新建文件夹（若不存在）
    val isCreated = zeroDir.mkdirs()

    val imageFile = File(picturesDir, displayName)

    return try {
        FileOutputStream(imageFile).use { fos ->
            // 将Bitmap写入文件
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)) {
                fos.flush()
                Log.d(TAG, "图片保存成功: ${imageFile.absolutePath}")
                true
            } else {
                false
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "保存图片失败", e)
        false
    }
}

private fun generateUniqueFileName(): String {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "IMG_$timeStamp"
}