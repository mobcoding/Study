package com.zero.base.ext

/**
 * @date:2026/2/12 21:10
 * @path:com.zero.base.ext._String
 */
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AESConfig {
    const val ALGORITHM = "AES/GCM/NoPadding"
    const val IV_LENGTH_BYTE = 12
    const val TAG_LENGTH_BIT = 128
    const val BASE64_FLAG = Base64.NO_WRAP

    /**
     * 将自定义密码字符串转换为标准 SecretKey
     */
    fun String.toAESKey(): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(this.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }
}

/**
 * 加密扩展函数
 * 用法: "hello".encrypt(myKey)
 */
fun String.encrypt(secretKey: SecretKey): String {
    if (this.isEmpty()) return ""
    return try {
        val cipher = Cipher.getInstance(AESConfig.ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val encryptedBytes = cipher.doFinal(this.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        val combined = iv + encryptedBytes
        Base64.encodeToString(combined, AESConfig.BASE64_FLAG)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

/**
 * 解密扩展函数
 * 用法: "base64EncodedString".decrypt(myKey)
 */
fun String?.decrypt(secretKey: SecretKey): String {
    if (this.isNullOrBlank()) return ""
    return try {
        val combined = Base64.decode(this, AESConfig.BASE64_FLAG)

        if (combined.size < AESConfig.IV_LENGTH_BYTE + 16) return ""

        val iv = combined.sliceArray(0 until AESConfig.IV_LENGTH_BYTE)
        val ciphertext = combined.sliceArray(AESConfig.IV_LENGTH_BYTE until combined.size)

        val cipher = Cipher.getInstance(AESConfig.ALGORITHM)
        val spec = GCMParameterSpec(AESConfig.TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}