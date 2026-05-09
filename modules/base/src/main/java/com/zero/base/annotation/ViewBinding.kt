package com.zero.base.annotation

import androidx.appcompat.app.AppCompatActivity

/**
 * @date:2024/12/12 11:51
 * @path:com.zero.base.annotation.ViewBind
 */
object ViewBinding {
    fun bind(activity: AppCompatActivity) {
        // 获取该 Activity 的全部成员变量
        for (field in activity.javaClass.declaredFields) {
            // 判断该成员变量是否被 MyBindView 注解
            val myBindView = field.getAnnotation(BindView::class.java)
            if (myBindView != null) {
                try {
                    // 注解符合的情况下，对该成员变量进行 findViewById 赋值
                    // 相当于 field = activity.findViewById(myBindView.value())
                    field.isAccessible = true // 确保可以访问私有字段
                    field.set(activity, activity.findViewById(myBindView.value))
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }
    }
}