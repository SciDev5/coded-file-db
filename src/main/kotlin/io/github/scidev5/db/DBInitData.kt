package io.github.scidev5.db

import io.github.scidev5.workingDir
import org.apache.commons.lang3.SystemUtils
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*

private val ALL_DOTFILES = arrayOf(
    "/scripts/open.sh",
    "/scripts/other command.sh"
)

object DBInitData {
    private val jv = CurrentDB::class.java
    private fun loadDotfile(pathName:String):String {
        val jarPath = Paths.get(jv.protectionDomain.codeSource.location.toURI()).absolutePathString().replace("\\","\\\\")
        return (
                jv.getResourceAsStream(pathName)
                    ?: throw Exception("'$pathName' template resource missing")
                ).bufferedReader().readText()
            .replace(Regex("\\{\\{JAR_PATH}}"),jarPath)
    }

    private fun writeDotfile(
        pathName: String,
        dir:Path,
        saveAsName:String = pathName.substringAfterLast("/").substringBeforeLast(".")
    ) {
        if (SystemUtils.IS_OS_UNIX) {
            // unix support
            val openShPath = dir / ("$saveAsName.sh")
            openShPath.writeText(loadDotfile(pathName))
            try {
                openShPath.setPosixFilePermissions(
                    setOf(PosixFilePermission.OWNER_EXECUTE) + openShPath.getPosixFilePermissions()
                )
            } catch (_: UnsupportedOperationException) {
            }
        } else if (SystemUtils.IS_OS_WINDOWS) {
            // windows support
            val openCmdPath = dir / ("$saveAsName.cmd")
            openCmdPath.writeText(loadDotfile("${pathName.substringBeforeLast(".")}.bat"))
        } else {
            throw Exception("unrecognized os, can't provide fitting helper files")
        }
    }
    private fun writeInfoFile(
        pathName: String,
        dir: Path,
        saveAsName: String = pathName.substringAfterLast("/")
    ) {
        val filePath = dir/saveAsName
        val streamIn = jv.getResourceAsStream(pathName)
            ?: throw Exception("'$pathName' info resource missing")
        filePath.writeBytes(streamIn.readBytes())
        streamIn.close()
    }

    fun writeAllDotfiles(dir: Path) {
        ALL_DOTFILES.forEach { writeDotfile(it,dir) }
    }
    fun writeRunToolDotfile() {
        writeDotfile(
            "/scripts/init.sh",
            workingDir,
            "init database here"
        )
        writeDotfile(
            "/scripts/other command.sh",
            workingDir
        )
        writeInfoFile(
            "/binary-texts/manual.pdf",
            workingDir,
            "manual.pdf",
        )
    }
}