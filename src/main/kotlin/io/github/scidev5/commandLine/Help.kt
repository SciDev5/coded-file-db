package io.github.scidev5.commandLine

object Help {
    private fun printHelpFile(which:String) {
        val helpReader = javaClass.getResourceAsStream(which)?.bufferedReader()
            ?: throw Exception("$which was missing")
        val helpText = helpReader.readText()
        helpReader.close()
        println(helpText)
    }
    fun print() {
        printHelpFile("/texts/cli.txt")
    }
    fun printDBOpen() {
        printHelpFile("/texts/db-interface.txt")
    }
}