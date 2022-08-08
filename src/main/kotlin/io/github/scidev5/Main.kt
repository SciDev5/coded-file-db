package io.github.scidev5

import io.github.scidev5.commandLine.Help
import io.github.scidev5.commandLine.ProgramArguments
import io.github.scidev5.commandLine.ProgramMode.*
import io.github.scidev5.db.CurrentDB
import io.github.scidev5.util.SplashReader
import java.nio.file.Path
import java.nio.file.Paths

val workingDir: Path = Paths.get(".")


fun main(args: Array<String>) {
    if (!ProgramArguments.parse(args)) return

    when(ProgramArguments.mode) {
        HELP -> Help.print()
        OPEN_DB -> CurrentDB.open()
        RUN_DB -> CurrentDB.run(ProgramArguments.runMode_commands)
        INIT_DB -> CurrentDB.init()
        VERSION -> SplashReader.print()

        else -> TODO("implement other program modes")
    }

//    val z = BCrypt.hashpw("hello world",BCrypt.gensalt())
//    println(z)
//    println(BCrypt.checkpw("yeet",z))
//    println(BCrypt.checkpw("hello world",z))
}