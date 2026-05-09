package com.zero.base.ext

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * @date:2025/8/8 15:02
 * @path:com.zero.base.ext.Permission
 */
/**
 * 一键打开文件管理页面
 *  需要添加MANAGE_EXTERNAL_STORAGE 特殊权限和传入当前应用包名
 */
fun Context.appFileManager(): Boolean {
    return try {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
        intent.data = Uri.parse("package:${packageName}")
        ContextCompat.startActivity(this, intent, null)
        true
    } catch (e: Exception) {
        false
    }
}

fun Context.hasOverlayPermission(): Boolean {
    var result = true
    try {
        val clazz: Class<*> = Settings::class.java
        val canDrawOverlays = clazz.getDeclaredMethod("canDrawOverlays", Context::class.java)
        result = canDrawOverlays.invoke(null, this) as Boolean
    } catch (e: Exception) {
        Log.e("PermissionUtils", Log.getStackTraceString(e))
    }
    return result
}

fun Context.applyOverlay(): Boolean {
    return try {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.data = Uri.parse("package:${packageName}")
        ContextCompat.startActivity(this, intent, null)
        true
    } catch (e: Exception) {
        e.printStackTrace();
        false
    }
}

fun Context.hasExactAlarmPermission(): Boolean? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.USE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.getSystemService(this, AlarmManager::class.java)?.canScheduleExactAlarms()
    } else {
        true
    }
}

fun Context.startAlarm() {
    val intent = when {
        Build.VERSION.SDK_INT in 31..33 -> {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        }

        Build.VERSION.SDK_INT >= 34 -> {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        }

        else -> {
            Intent(Settings.ACTION_DATE_SETTINGS)
        }
    }
    startActivity(intent)
}

/**
 * 封装：注册通知权限回调
 */
fun AppCompatActivity.registerNotificationLauncher(onGranted: () -> Unit, onDenied: () -> Unit): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) onGranted() else onDenied()
    }
}

/**
 * 封装：执行申请逻辑
 */
fun AppCompatActivity.checkAndRequestNotification(launcher: ActivityResultLauncher<String>, onAlreadyGranted: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        when {
            // 已授权
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                onAlreadyGranted()
            }
            // 用户拒绝过一次，显示解释弹窗（Rationale）
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
                AlertDialog.Builder(this).setTitle("需要通知权限").setMessage("开启通知权限，确保您能及时收到订单和消息提醒。").setPositiveButton("去开启") { _, _ ->
                    launcher.launch(permission)
                }.setNegativeButton("取消", null).show()
            }
            // 直接申请
            else -> launcher.launch(permission)
        }
    } else {
        // API 33 以下默认有权
        onAlreadyGranted()
    }
}