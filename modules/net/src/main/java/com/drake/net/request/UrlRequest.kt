

package com.drake.net.request

open class UrlRequest : BaseRequest() {

    override fun param(name: String, value: String?) {
        value ?: return
        httpUrl.setQueryParameter(name, value)
    }

    override fun param(name: String, value: String?, encoded: Boolean) {
        value ?: return
        if (encoded) {
            httpUrl.setEncodedQueryParameter(name, value)
        } else {
            httpUrl.setQueryParameter(name, value)
        }
    }

    override fun param(name: String, value: Number?) {
        value ?: return
        httpUrl.setQueryParameter(name, value.toString())
    }

    override fun param(name: String, value: Boolean?) {
        value ?: return
        httpUrl.setQueryParameter(name, value.toString())
    }
}