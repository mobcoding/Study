

package com.drake.net.convert

import com.drake.net.exception.ConvertException
import com.drake.net.response.file
import okhttp3.Response
import okio.ByteString
import java.io.File
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
interface NetConverter {

    @Throws(Throwable::class)
    fun <R> onConvert(succeed: Type, response: Response): R?

    companion object DEFAULT : NetConverter {
        /**
         * 返回结果应当等于泛型对象, 可空
         * @param succeed 请求要求返回的泛型类型
         * @param response 请求响应对象
         */
        override fun <R> onConvert(succeed: Type, response: Response): R? {
            return when {
                succeed === String::class.java && response.isSuccessful -> response.body?.string() as R
                succeed === ByteString::class.java && response.isSuccessful -> response.body?.byteString() as R
                succeed is GenericArrayType && succeed.genericComponentType === Byte::class.java && response.isSuccessful -> response.body?.bytes() as R
                succeed === File::class.java && response.isSuccessful -> response.file() as R
                succeed === Response::class.java -> response as R
                else -> throw ConvertException(response, "An exception occurred while converting the NetConverter.DEFAULT")
            }
        }
    }
}