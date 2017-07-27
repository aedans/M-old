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

val LANGUAGE_INTERPRETED: Language = {
    val env = getDefaultEnvironment()

    File(it.source).inputStream()
            .iterator()
            .lookaheadIterator()
            .toIR(env)
            .forEach {
                it.eval(env.memory)
            }
}
