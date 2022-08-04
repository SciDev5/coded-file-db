package io.github.scidev5

import java.security.MessageDigest

object Coder {
    private var digestSHA256: MessageDigest = MessageDigest.getInstance("SHA-256")

    fun hashStr(str: String): String {
        digestSHA256.reset()

        digestSHA256.update(str.toByteArray(Charsets.UTF_8))
        return hexEncode(digestSHA256.digest())
    }
}