package io.github.scidev5.commandLine

@Suppress("unused")
object AskForInput {
    fun <T> obj(message: String, optionsMessage: String, parseObj:(String)->T?):T? {
        return parseObj(line("$message [$optionsMessage]"))
    }
    fun <T> obj(message: String, parseObj:(String)->T?):T? {
        return parseObj(line(message))
    }
    fun line(message: String):String {
        print("> $message: ")
        return readln()
    }
    fun lineOrNevermind(message: String, optionsMessage: String, validate:(String)->Boolean):String? {
        while (true) {
            val reply = line("$message [$optionsMessage or 'nevermind']")
            if (reply == "nevermind")
                return null
            if (validate(reply))
                return reply
        }
    }
    fun int(message: String):Int? {
        return obj(message,"int") { it.toIntOrNull() }
    }
    fun long(message: String):Long? {
        return obj(message, "long int") { it.toLongOrNull() }
    }
    fun float(message: String):Float? {
        return obj(message, "decimal") { it.toFloatOrNull() }
    }
    fun double(message: String):Double? {
        return obj(message, "precise decimal") { it.toDoubleOrNull() }
    }

    fun password(message: String):String {
        print("> $message:")
        return System.console()?.readPassword()?.concatToString()
            ?: readln()
    }
}