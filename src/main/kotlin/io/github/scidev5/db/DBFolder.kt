package io.github.scidev5.db

import java.io.BufferedInputStream
import java.io.File
import java.io.FileFilter
import java.nio.file.Path
import kotlin.io.path.*

object AllFolderContents

class DBFolder(
    private val location: Path,
    onCreate: ()->Unit,
    private val onDelete: ()->Unit)
{
    constructor(location: Path) : this(location, {}, {})
    private val isInitialized:Boolean get() = location.isDirectory()
    private fun childFile(fileName: String) = location/fileName

    init {
        if (!isInitialized) { // don't re-initialize
            location.deleteIfExists()
            location.createDirectory()
            onCreate()
        }
    }

    fun delete() {
        clear()
        location.deleteIfExists()
        onDelete()
    }

    operator fun get(fileName:String):BufferedInputStream? {
        val ref = childFile(fileName)
        if (!ref.exists()) return null
        return ref.inputStream().buffered()
    }
    operator fun iterator():Iterator<String> {
        return (listFiles() ?: emptyArray())
            .map { f -> f.name }.iterator()
    }
    operator fun set(fileName:String, inputStream: BufferedInputStream) {
        val outputStream = childFile(fileName).outputStream().buffered()
        val chunk = ByteArray(0x1000000) // copy in 16MiB chunks
        var readLen: Int
        while (true) {
            readLen = inputStream.read(chunk)
            if (readLen == -1) break
            outputStream.write(chunk,0,readLen)
        }
        inputStream.close()
        outputStream.close()
    }
    operator fun set(fileName: String, data: ByteArray) {
        childFile(fileName).writeBytes(data)
    }
    operator fun set(fileName: String, data: File) {
        this[fileName] = data.inputStream().buffered()
    }
    operator fun set(fileName: String, @Suppress("UNUSED_PARAMETER") data: Nothing?) {
        childFile(fileName).deleteIfExists()
    }

    operator fun get(@Suppress("UNUSED_PARAMETER") index:AllFolderContents):HashMap<String,BufferedInputStream> {
        val outputMap = HashMap<String,BufferedInputStream>()
        for (file in listFiles { it.isFile } ?: emptyArray()) {
            outputMap[file.name] = file.inputStream().buffered()
        }
        return outputMap
    }
    operator fun set(@Suppress("UNUSED_PARAMETER") index:AllFolderContents, otherFolder: HashMap<String,BufferedInputStream>) {
        clear { !otherFolder.containsKey(it.name) }
        for (key in otherFolder.keys)
            this[key] = otherFolder[key]
                ?: throw AssertionError("otherFolder[key in otherFolder.keys] returned null, impossible")
    }

    private fun listFiles() = listFiles(null)
    private fun listFiles(filter: FileFilter?) = location.toFile().listFiles(filter)
    private fun clear() = clear(null)
    private fun clear(filter: FileFilter?) {
        for (file in listFiles(filter) ?: emptyArray())
            file.delete()
    }
}