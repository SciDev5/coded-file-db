package io.github.scidev5.util

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class PossibleThing<T>(
    private val v: T?,
    private val cleanupThing: T.() -> Unit
) {
    infix fun ifExists(handle:(T)->Unit):PossibleThingElse<T,Unit> {
        return ifExistsReturn(handle)
    }
    infix fun <R> ifExistsReturn(handle:(T)->R):PossibleThingElse<T,R> {
        return PossibleThingElse(
            v,
            if(v !== null) {
                val ret = handle(v)
                cleanupThing(v)
                ret
            } else null
        )
    }
}

class PossibleThingElse<T,R>(
    private val v:T?,
    private var ret:R?)
{
    infix fun otherwise(handle:()->R):R {
        if (v === null) {
            ret = handle()
        }
        return ret!!
    }
}

object With {
    inline fun <T : Closeable, R> autoClose(closeable: T, doSomething: (T) -> R):R {
        val r = doSomething(closeable)
        closeable.close()
        return r
    }
    fun fileIn(path: Path):PossibleThing<InputStream> {
        return PossibleThing(
            if (path.exists())
                path.inputStream()
            else null,
            InputStream::close
        )
    }
    inline fun <R> fileOut(path: Path, handle:(OutputStream)->R):R
        = autoClose(path.outputStream(),handle)
    inline fun <R> fileOutWriter(path: Path, handle:(PrintWriter)->R):R
        = autoClose(PrintWriter(path.outputStream().bufferedWriter()),handle)
}