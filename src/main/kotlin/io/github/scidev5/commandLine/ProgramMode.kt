package io.github.scidev5.commandLine

enum class ProgramMode {
    OPEN_DB,
    RUN_DB,
    INIT_DB,
    VERSION,
    HELP;
    companion object {
        fun fromString(name: String): ProgramMode? = when (name) {
            "--open", "-o" -> OPEN_DB
            "--init", "-i" -> INIT_DB
            "--help", "?", "-h" -> HELP
            "--version", "-v" -> VERSION
            "--run", "-r" -> RUN_DB
            else -> null
        }
    }
}