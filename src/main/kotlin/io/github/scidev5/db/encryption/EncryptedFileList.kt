package io.github.scidev5.db.encryption

import java.nio.file.Path

class EncryptedFileList(cypherIn:AESWithKey) {
    val files = mutableSetOf<EncryptedFile>()
    var cypher = cypherIn
        set(value) {
            field = value
            files.forEach {
                try {
                    it.migrateCypher(value)
                } catch (e: Exception) {
                    // Prevent errors here from causing partial cypher migrations
                    System.err.println(e)
                }
            }
        }

    fun open(path: Path,defaultContent:()->ByteArray)
        = EncryptedFile(this,path,defaultContent)
}