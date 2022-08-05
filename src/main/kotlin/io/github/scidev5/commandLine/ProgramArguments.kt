package io.github.scidev5.commandLine

import io.github.scidev5.util.RegexMany

object ProgramArguments {
    var mode:ProgramMode? = null; private set
    var autoConfirmFlag = false; private set

    var TEMP_rootFolder = ""; private set

    fun parse(args: Array<String>):Boolean {
        val error = run error@{
            if (args.isEmpty()) return@error "no arguments were passed"
            mode = ProgramMode.fromString(args[0]) ?: return@error "invalid mode [${args[0]}]"

            val argMatch = RegexMany()
            argMatch.add("-y",1) {
                autoConfirmFlag = true
            }
            argMatch.add("-r=(.*)",1) {
                TEMP_rootFolder = it.groups[1]?.value ?: throw Exception("<impossible> rootFolder match group 1 missing")
            }

            for (arg in args.slice(1 until args.size)) {
                val matched = argMatch(arg)
                if (!matched) return@error "invalid argument [$arg]"
            }
            return true
        }
        println("error! $error")
        Help.print()
        return false
    }
}