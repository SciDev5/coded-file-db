package io.github.scidev5.db

import io.github.scidev5.commandLine.requestConfirmation
import io.github.scidev5.commandLine.DefaultConfirmation
import io.github.scidev5.db.encryption.Coder
import io.github.scidev5.db.encryption.DBEncryption
import io.github.scidev5.db.encryption.EncryptedFile
import io.github.scidev5.db.encryption.EncryptedFileSerializer
import io.github.scidev5.util.UnableToContinueException
import java.nio.file.Path
import kotlin.io.path.*


class DBRemote(private val REMOTE_DIR: Path, config:DBConfig, password:String) {
    private val coder = Coder(config.groupNameSalt)
    val encryption = DBEncryption(password,REMOTE_DIR)

    private val folders:MutableSet<String>
    private val folderListFile: EncryptedFile
    private val folderListFileData:EncryptedFileSerializer<MutableSet<String>>
    private fun saveFolderList() { folderListFileData.content = folders }

    private fun locate(folderName: String) = REMOTE_DIR/coder.hashStr(folderName)

    operator fun get(folderName: String) = DBFolder(
        locate(folderName),
        { folders.add   (folderName); saveFolderList() },
        { folders.remove(folderName); saveFolderList() }
    )
    fun hasFolder(folderName: String): Boolean
        = locate(folderName).isDirectory()

    fun allFolders():Set<String>
        = folders.toSet()

    init {
        if (!REMOTE_DIR.isDirectory()) {
            if (requestConfirmation("create folder [${REMOTE_DIR.absolutePathString()}]?", DefaultConfirmation.YES))
                try {
                    REMOTE_DIR.createDirectories()
                } catch (e: FileAlreadyExistsException) {
                    println("[!!ERROR!!] There is a file in the way of creating the database folder.")
                    throw e
                }
            else
                throw UnableToContinueException("did not create folder")
        }
        if (config.initialized)
            if (!encryption.checkPassword())
                throw UnableToContinueException("incorrect password")
        folderListFile = encryption.encryptedFiles
            .open(REMOTE_DIR / "folderList") { "".toByteArray(Charsets.UTF_8) }
        folderListFileData = folderListFile.getWriterReader(
            { it.joinToString("\n").toByteArray(Charsets.UTF_8) },
            { it.toString(Charsets.UTF_8).split(Regex.fromLiteral("\n")).toMutableSet() }
        )
        folders = folderListFileData.content
    }
    fun destroy() {
        folderListFile.delete()
        if (!REMOTE_DIR.exists() || !REMOTE_DIR.isDirectory()) return
        for (file in REMOTE_DIR.toFile().listFiles() ?: emptyArray())
            file.deleteRecursively()
    }
}