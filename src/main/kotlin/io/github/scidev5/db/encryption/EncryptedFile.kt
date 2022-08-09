package io.github.scidev5.db.encryption

import java.nio.file.Path
import kotlin.io.path.*


class EncryptedFile(
    private val fileList:EncryptedFileList,
    private val path: Path,
    defaultContent:()->ByteArray)
{
    private var cypher = fileList.cypher
    fun read():ByteArray = cypher.decrypt(path.readBytes())
    fun write(data:ByteArray) = path.writeBytes(cypher.encrypt(data))

    fun migrateCypher(newCypher: AESWithKey) {
        if (path.exists()) {
            val content = read()
            cypher = newCypher
            write(content)
        } else cypher = newCypher
    }

    fun delete() {
        path.deleteIfExists()
        fileList.files.remove(this)
    }

    init {
        if (path.exists()) {
            if (!path.isRegularFile())
                throw Exception("There is a non-file in the way of making this file.")
        } else {
            write(defaultContent())
        }
        fileList.files.add(this)
    }

    fun <T> getWriterReader(
        serialize: (T)->ByteArray,
        deserialize: (ByteArray)->T)
        = EncryptedFileSerializer(this,serialize,deserialize)

}

class EncryptedFileSerializer<T> internal constructor(
    private val file: EncryptedFile,
    private val serialize: (T)->ByteArray,
    private val deserialize: (ByteArray)->T
) {
    var content:T
        get() = deserialize(file.read())
        set(v) = file.write(serialize(v))
}