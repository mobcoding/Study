package com.zero.base.data

import com.tencent.mmkv.MMKV

object IpManager {

    /** MMKV独有的mmapId */
    private const val MMKV_MAP_ID = "ip"

    /** 常用的IP */
    private const val DEFAULT_IP_ADDRESS_REMOTE = "https://www.wanandroid.com/"     // 外网

    private const val KEY_DEFAULT_IP_AND_PORT = "data_default_ip_and_port"

    private val mmkv by lazy { MMKV.mmkvWithID(MMKV_MAP_ID) }

    /**
     * 保存默认IP
     *
     * @param ip 要保存为默认的IP
     */
    fun saveDefaultIP(ip: String) {
        mmkv.encode(KEY_DEFAULT_IP_AND_PORT, ip)
    }

    /** 获取默认IP */
    fun getDefaultIP(): String {
        return mmkv.decodeString(KEY_DEFAULT_IP_AND_PORT, DEFAULT_IP_ADDRESS_REMOTE)!!
    }
}