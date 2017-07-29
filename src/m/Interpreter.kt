package m

import java.io.BufferedReader
import java.io.File
import java.io.InputStream

/**
 * Created by Aedan Smith.
 */

operator fun BufferedReader.iterator() = object : Iterator<Char> {
    override fun hasNext() = this@iterator.ready()
    override fun next() = this@iterator.read().toChar()
}

fun File.interpret(env: RuntimeEnvironment) = bufferedReader().iterator().interpret(env)
fun Iterator<Char>.interpret(env: RuntimeEnvironment) {
    this
            .lookaheadIterator()
            .toIR(env)
            .forEach { it.eval(env.memory) }
}
