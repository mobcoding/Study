package com.zero.base.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

public class StateBarUtils {

    /**
     * 设置透明状态栏
     *
     * @param activity 目标界面
     */
    public static void setTransparentForWindow(@NonNull Activity activity) {
        WeakReference<Activity> activityWeakReference = new WeakReference<>(activity);
        Window window = activityWeakReference.get().getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    /**
     * 全屏沉浸式状态栏
     */
    public static void initStatusBar(Activity activity) {
        // 延伸显示区域到刘海
        WeakReference<Activity> activityWeakReference = new WeakReference<>(activity);
        Window window = activityWeakReference.get().getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            // 延伸显示区域到刘海
            activity.getWindow().setAttributes(layoutParams);
        }
    }

    public static void setFullScreen(Activity activity) {
        WeakReference<Activity> activityWeakReference = new WeakReference<>(activity);
        Window window = activityWeakReference.get().getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
}
