package com.zero.base.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.zero.base.net.bean.ApiResponse

/**
 * ViewModel基类
 * @author LTP  2021/11/23
 */
abstract class BaseViewModel : ViewModel() {


    val loading = MutableLiveData<Boolean>()

    val error = MutableLiveData<String>()

    /** 请求异常（服务器请求失败，譬如：服务器连接超时等） */
    val exception = MutableLiveData<Exception>()

    /** 请求服务器返回错误（服务器请求成功但status错误，譬如：登录过期等） */
    val errorResponse = MutableLiveData<ApiResponse<*>?>()
}