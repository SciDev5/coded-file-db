package io.github.scidev5.commandLine

import io.github.scidev5.util.RegexMany

object ProgramArguments {
    var mode:ProgramMode? = null; private set
    var autoConfirmFlag = false; private set

    var runMode_commands = ""; private set

    fun parse(args: Array<String>):Boolean {
        val error = run error@{
            if (args.isEmpty()) return@error "no arguments were passed"
            mode = ProgramMode.fromString(args[0]) ?: return@error "invalid mode [${args[0]}]"

            val argMatch = RegexMany()
            argMatch.add("-y",1) {
                autoConfirmFlag = true
            }
            when (mode) {
                ProgramMode.RUN_DB -> {
                    argMatch.add("-(?:commands|C)=(.*)", 1) {
                        runMode_commands = it.groupValues[1]
                    }
                }
                else -> {}
            }

            for (arg in args.slice(1 until args.size)) {
                val matched = argMatch(arg)
                if (!matched) return@error "invalid argument [$arg]"
            }
            return true
        }
        println("Error reading arguments: $error\nSee `--help` for usage instructions.")
        return false
    }
}