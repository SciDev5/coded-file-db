package io.github.scidev5.commandLine

import io.github.scidev5.util.DefaultConfirmation
import io.github.scidev5.util.DefaultConfirmation.*
import kotlin.random.Random


fun requestConfirmation(request: String, default:DefaultConfirmation):Boolean {
    if (ProgramArguments.autoConfirmFlag) return true

    print("$request [${ if (default == YES) "Y/n" else "y/N" }]: ")

    val response = readln().lowercase()

    return if (default == YES)
        !(response == "n" || response == "no") // default is yes, so it's a yes if it's not no
    else
        response == "y" || response == "yes"  // default is no, so it's a yes only if it's a yes
}
fun confirm(request: String, default: DefaultConfirmation):Unit?
    = if (requestConfirmation(request,default)) Unit else null

private fun genConfirmText(nLetters:Int=6):String {
    var str = "confirm_"
    val letters = "abcdefghijklmnopqrstuvwxyz".toCharArray()
    for (i in 0 until nLetters)
        str += letters[Random.nextInt(letters.size)]
    return str
}
fun requestStrongConfirmation(request: String):Boolean {
    if (ProgramArguments.autoConfirmFlag) return true

    val confirmationText = genConfirmText()
    print("$request [retype '$confirmationText' to confirm]: ")
    return readln().lowercase() == confirmationText
}
fun strongConfirm(request: String):Unit?
        = if (requestStrongConfirmation(request)) Unit else null