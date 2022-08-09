package io.github.scidev5.db.encryption

import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
class AESWithKey(password:String,salt:ByteArray) {
    private val secret: SecretKey
    init {
        val spec: KeySpec = PBEKeySpec(
            password.toCharArray(),
            salt,
            65536,
            256
        )
        secret = SecretKeySpec(secretKeyFactory.generateSecret(spec).encoded, "AES")
    }

    private fun getCipher(mode: Int): Cipher {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(mode, secret, IvParameterSpec(ByteArray(16)))
        return cipher
    }
    fun encrypt(data:ByteArray):ByteArray = getCipher(Cipher.ENCRYPT_MODE).doFinal(data)
    fun decrypt(data:ByteArray):ByteArray = getCipher(Cipher.DECRYPT_MODE).doFinal(data)
}