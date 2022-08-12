package io.github.scidev5.db

import io.github.scidev5.workingDir
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
        val openShPath = dir / ("$saveAsName.sh")
        val openCmdPath = dir / ("$saveAsName[windows].cmd")
        openShPath.writeText(loadDotfile(pathName))
        try { // *nix support
            openShPath.setPosixFilePermissions(
                setOf(PosixFilePermission.OWNER_EXECUTE) + openShPath.getPosixFilePermissions()
            )
        } catch (_: UnsupportedOperationException) {}
        // windows support
        openCmdPath.writeText("""
            powershell "./\"$saveAsName.sh\""
        """.trim())
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
            "/scripts/other command.sh",
            workingDir,
            "run-CodedFileDB"
        )
        writeInfoFile(
            "/binary-texts/usage-instructions.pdf",
            workingDir,
            "usage-instructions.pdf",
        )
    }
}