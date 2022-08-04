package io.github.scidev5

import org.mindrot.jbcrypt.*
import java.io.File

fun main(args: Array<String>) {


    println(File("test.txt").absolutePath)


    println(Coder.hashStr("helloworld"))
    println(Coder.hashStr("helloworld2"))
    println(Coder.hashStr("helloworld"))

    val z = BCrypt.hashpw("helloworld",BCrypt.gensalt())
    println(z)
    println(BCrypt.checkpw("hewwoworld",z))
    println(BCrypt.checkpw("helloworld",z))
}