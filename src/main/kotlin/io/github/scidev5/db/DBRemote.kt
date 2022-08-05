package io.github.scidev5.db

import io.github.scidev5.commandLine.requestConfirmation
import io.github.scidev5.util.DefaultConfirmation
import java.nio.file.Path
import kotlin.io.path.*


class DBRemote(private val REMOTE_DIR: Path) {
    private val coder = Coder(REMOTE_DIR.absolutePathString().toByteArray(Charsets.UTF_8))

    private fun locate(folderName: String) = REMOTE_DIR/coder.hashStr(folderName)

    operator fun get(folderName: String): DBFolder
        = DBFolder(locate(folderName))
    fun hasFolder(folderName: String): Boolean
        = locate(folderName).isDirectory()

    init {
        if (!REMOTE_DIR.exists())
            if (requestConfirmation("create folder [${REMOTE_DIR.absolutePathString()}]?", DefaultConfirmation.YES))
                try {
                    REMOTE_DIR.createDirectories()
                } catch (e: FileAlreadyExistsException) {
                    println("[!!ERROR!!] There is a file in the way of creating the database folder.")
                    throw e
                }
    }
    fun destroy() {
        if (!REMOTE_DIR.exists() || !REMOTE_DIR.isDirectory()) return
        for (subfolder in REMOTE_DIR.toFile().listFiles() ?: emptyArray())

        REMOTE_DIR.deleteIfExists()
    }
}