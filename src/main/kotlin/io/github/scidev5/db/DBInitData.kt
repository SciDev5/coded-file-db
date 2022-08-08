package io.github.scidev5.db

import java.nio.file.Paths
import kotlin.io.path.absolutePathString

object DBInitData {
    val openSHContent:String get() {
        val jv = CurrentDB::class.java
        val jarPath = Paths.get(jv.protectionDomain.codeSource.location.toURI()).absolutePathString().replace("\\","\\\\")
        return (
                jv.getResourceAsStream("/autos/open.sh")
                    ?: throw Exception("'/autos/open.sh' template resource missing")
                ).bufferedReader().readText()
            .replace(Regex("\\{\\{JAR_PATH}}"),jarPath)
    }
}