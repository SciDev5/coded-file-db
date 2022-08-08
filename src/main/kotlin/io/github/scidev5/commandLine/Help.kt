package io.github.scidev5.commandLine

object Help {
    private fun printHelpFile(which:String) {
        val helpReader = javaClass.getResourceAsStream("/help/$which")?.bufferedReader()
            ?: throw Exception("cli.txt resource was missing")
        val helpText = helpReader.readText()
        helpReader.close()
        println(helpText)
    }
    fun print() {
        printHelpFile("cli.txt")
    }
    fun printDBOpen() {
        printHelpFile("db-interface.txt")
    }
}