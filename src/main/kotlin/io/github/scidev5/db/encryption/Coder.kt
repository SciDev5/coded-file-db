package io.github.scidev5.db.encryption

import io.github.scidev5.util.Hex
import java.security.MessageDigest

class Coder(private val dbId: ByteArray) {
    private val sha256: MessageDigest = MessageDigest.getInstance("SHA-256")

    fun hashStr(str: String): String {
        sha256.reset()
        sha256.update(dbId)
        sha256.update(str.toByteArray(Charsets.UTF_8))
        return Hex.encode(sha256.digest())
    }
}