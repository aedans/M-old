package m

import java.io.File
import java.io.InputStream

/**
 * Created by Aedan Smith.
 */

operator fun InputStream.iterator() = object : Iterator<Char> {
    override fun hasNext() = this@iterator.available() != 0
    override fun next() = this@iterator.read().toChar()
}

fun File.interpret(env: RuntimeEnvironment) = inputStream().iterator().interpret(env)
fun Iterator<Char>.interpret(env: RuntimeEnvironment) {
    this
            .lookaheadIterator()
            .toIR(env)
            .forEach { it.eval(env.memory) }
}
