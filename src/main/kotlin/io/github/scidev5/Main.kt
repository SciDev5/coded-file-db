package io.github.scidev5

import io.github.scidev5.commandLine.Help
import io.github.scidev5.commandLine.ProgramArguments
import io.github.scidev5.commandLine.ProgramMode.*
import io.github.scidev5.db.CurrentDB
import io.github.scidev5.util.SplashReader
import io.github.scidev5.util.UnableToContinueException
import java.nio.file.Path
import java.nio.file.Paths

val workingDir: Path = Paths.get(".")


fun main(args: Array<String>) {
    if (!ProgramArguments.parse(args)) return

    try {
        when (ProgramArguments.mode) {
            HELP -> Help.print()
            OPEN_DB -> CurrentDB.open()
            RUN_DB -> CurrentDB.run(ProgramArguments.runMode_commands)
            INIT_DB -> CurrentDB.init()
            RESET_PASSWORD -> CurrentDB.resetPassword()
            VERSION -> SplashReader.print()

            else -> TODO("implement other program modes")
        }
    } catch (e: UnableToContinueException) {
        println("!!!! unable to continue: ${e.reason} !!!!\n\n:: Aborting ::")
    }

}