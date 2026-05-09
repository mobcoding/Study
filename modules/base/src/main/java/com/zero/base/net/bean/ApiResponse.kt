package com.zero.base.net.bean

/**
 * 接口返回外层封装实体
 *
 * @author LTP  2022/3/22
 */
data class ApiResponse<T>(
    val data: T,
    val errorCode: Int,
    val errorMsg: String
)