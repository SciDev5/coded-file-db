package io.github.scidev5.db

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*

private val ALL_DOTFILES = arrayOf(
    "/autos/open.sh",
    "/autos/other command.sh"
)

object DBInitData {
    private fun loadDotfile(pathName:String):String {
        val jv = CurrentDB::class.java
        val jarPath = Paths.get(jv.protectionDomain.codeSource.location.toURI()).absolutePathString().replace("\\","\\\\")
        return (
                jv.getResourceAsStream(pathName)
                    ?: throw Exception("'$pathName' template resource missing")
                ).bufferedReader().readText()
            .replace(Regex("\\{\\{JAR_PATH}}"),jarPath)
    }

    private fun writeDotfile(pathName: String, dir:Path) {
        val openShPath = dir / (pathName.substringAfterLast("/"))
        openShPath.writeText(loadDotfile(pathName))
        try {
            openShPath.setPosixFilePermissions(
                setOf(PosixFilePermission.OWNER_EXECUTE) + openShPath.getPosixFilePermissions()
            )
        } catch (_: UnsupportedOperationException) {}
    }

    fun writeAllDotfiles(dir: Path) {
        ALL_DOTFILES.forEach { writeDotfile(it,dir) }
    }
}