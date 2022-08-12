package io.github.scidev5.db

import io.github.scidev5.workingDir
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.nio.file.Paths
import java.security.SecureRandom
import java.util.Base64
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
    val path = workingDir/".config.json"

    val groupNameSalt: ByteArray
    var remoteDir: Path = Paths.get(".")/".@storage"
    var localDir: Path = Paths.get(".")
    var initialized = path.exists(); private set

    private object KEY {
        const val remoteDir = "remoteDir"
        const val groupNameSalt = "groupNameSalt"
    }

    init {
        if (initialized) {
            val json = Json.parseToJsonElement(path.readText())
            remoteDir = Paths.get(
                json.jsonObject[KEY.remoteDir]?.jsonPrimitive?.contentOrNull
                    ?: throw DBParseException(this,"remote database path missing or invalid",KEY.remoteDir,DBExpectedType.STRING)
            )
            groupNameSalt = Base64.getDecoder().decode(
                json.jsonObject[KEY.groupNameSalt]?.jsonPrimitive?.contentOrNull
                    ?: throw DBParseException(this,"groupNameSalt missing or invalid",KEY.groupNameSalt,DBExpectedType.STRING)
            )
        } else {
            groupNameSalt = ByteArray(16)
            SecureRandom.getInstanceStrong().nextBytes(groupNameSalt)
        }
    }

    fun save() {
        path.writeText(buildJsonObject {
            put(KEY.remoteDir,remoteDir.pathString)
            put(KEY.groupNameSalt,Base64.getEncoder().encodeToString(groupNameSalt))
        }.toString())
        initialized = true
    }

}