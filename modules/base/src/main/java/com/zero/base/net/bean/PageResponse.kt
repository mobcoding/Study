package com.zero.base.net.bean

/**
 * 分页实体
 *
 */
data class PageResponse<T>(
    val curPage: Int,
    val datas: List<T>,
    val offset: Int,
    val over: Boolean,
    val pageCount: Int,
    val size: Int,
    val total: Int
)