package io.github.scidev5.db.encryption

import io.github.scidev5.commandLine.AskForInput
import io.github.scidev5.util.With
import org.mindrot.jbcrypt.BCrypt
import java.nio.file.Path
import java.security.SecureRandom
import kotlin.io.path.*

private val random = SecureRandom.getInstanceStrong()


private const val SALT_LEN = 8
class DBEncryption(
    private var password:String,
    ROOT_DIR: Path)
{
    private val passwordSaltFile = ROOT_DIR/"pwd.salt"
    private val passwordBCryptCiphertextFile = ROOT_DIR/"pwd.txt"

    private var cypher: AESWithKey
    private val salt: ByteArray = ByteArray(SALT_LEN) { 0 }
    private var bcryptCipherText:String? = null
    private fun regenBCryptCipherText() {
        bcryptCipherText = BCrypt.hashpw(password,BCrypt.gensalt())
    }
    private fun randomizeSalt():ByteArray {
        random.nextBytes(salt)
        return salt
    }

    val encryptedFiles:EncryptedFileList



    init {
        loadCredentials()

        cypher = AESWithKey(password, salt)
        encryptedFiles = EncryptedFileList(cypher)
    }


    fun initPassword() {
        if (bcryptCipherText !== null)
            throw Exception("Cannot re-initialize password.")
        regenBCryptCipherText()
        saveCredentials()
    }
    fun setPassword(newPassword:String):Boolean {
        if (!checkPassword())
            return false
        password = newPassword
        randomizeSalt()
        regenBCryptCipherText()
        saveCredentials()
        cypher = AESWithKey(password,salt)
        encryptedFiles.cypher = cypher
        return true
    }
    fun checkPassword():Boolean
        = bcryptCipherText?.let { BCrypt.checkpw(password,it) } ?:
            throw Exception("password check file was missing")
    fun logIncorrectPassword() {
        println(
            if (checkPassword()) "[password was correct]"
            else                 "incorrect password was: '$password'"
        )
    }

    fun saveCredentials() {
        With.fileOut(passwordSaltFile) {
            it.write(salt)
        }
        if (bcryptCipherText !== null)
            With.fileOutWriter(passwordBCryptCiphertextFile) {
                it.println(bcryptCipherText!!)
            }
    }

    fun loadCredentials() {
        With.fileIn(passwordSaltFile) ifExists {
            it.read(salt)
        } otherwise {
            randomizeSalt()
        }
        With.fileIn(passwordBCryptCiphertextFile) ifExists {
            bcryptCipherText = it.bufferedReader().readLine()
        } otherwise {
            bcryptCipherText = null
        }
    }

    companion object {
        fun standardizePassword(password: String):String? {
            val trimmed = password.trim()
            return trimmed.ifEmpty { null }
        }
        fun enterNewPassword():String {
            while (true) {
                val newPassword = standardizePassword(
                    AskForInput.password("new database password")
                ) ?: continue // re-prompt if password is invalid.
                if (AskForInput.password("confirm password") != newPassword) {
                    println("passwords did not match, try agan.")
                    continue
                }
                return newPassword
            }
        }
    }
}