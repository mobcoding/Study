package com.zero.base.annotation


/**
 * @date:2024/12/12 11:48
 * @path:com.zero.base.annotation.BindView
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
 annotation class BindView(val value: Int = 0)
