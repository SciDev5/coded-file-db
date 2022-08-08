package io.github.scidev5.db

import io.github.scidev5.workingDir
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

private enum class DBExpectedType(val text: String) {
//    NUMBER("number"),
//    BOOL("boolean"),
    STRING("string");

    override fun toString(): String = text
}

private class DBParseException(dbConfig: DBConfig,msg:String,key:String,expectedType:DBExpectedType)
    : Exception("[DBConfig@'${dbConfig.path.absolutePathString()}'] $msg\n>> value for '$key' should be $expectedType!")


class DBConfig {
    val path = workingDir/"codedDbConfig.json"

    var remoteDir: Path = Paths.get(".")
    var localDir: Path = Paths.get(".")
    var initialized = path.exists(); private set

    private object KEY {
        const val remoteDir = "remoteDir"
    }

    init {
        if (initialized) {
            val json = Json.parseToJsonElement(path.readText())
            remoteDir = Paths.get(
                json.jsonObject[KEY.remoteDir]?.jsonPrimitive?.contentOrNull
                ?: throw DBParseException(this,"remote database path missing or invalid",KEY.remoteDir,DBExpectedType.STRING)
            )
        }
    }

    fun save() {
        path.writeText(buildJsonObject {
            this.put(KEY.remoteDir,remoteDir.absolutePathString())
        }.toString())
        initialized = true
    }

}