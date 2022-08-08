package io.github.scidev5.util

object SplashReader {
    fun print() {
        println((
                javaClass.getResourceAsStream("/splash.txt")
                    ?: throw java.lang.Exception("could not read splash page file")
                ).bufferedReader().readText()
        )
    }
}