package io.github.scidev5.commandLine

object Help {
    private fun printHelpFile(which:String) {
        val helpReader = javaClass.getResourceAsStream("/help/$which.txt")?.bufferedReader()
            ?: throw Exception("main.txt resource was missing")
        val helpText = helpReader.readText()
        helpReader.close()
        println(helpText)
    }
    fun print() {
        printHelpFile("main")
    }
    fun printDBOpen() {
        printHelpFile("db-open")
    }
}