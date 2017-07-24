package m

import java.io.InputStream

/**
 * Created by Aedan Smith.
 */

object Repl : Runnable {
    override fun run() {
        val env = getDefaultEnvironment(System.out)
        run(env)
    }

    private tailrec fun run(environment: Environment,
                    irExpressionIterator: Iterator<IRExpression> = ReplStream(System.`in`)
                            .lookaheadIterator()
                            .toIR(environment)) {
        val success = try {
            irExpressionIterator.next().eval(environment).takeIf { it != Unit }?.also { println(it) }
            true
        } catch (e: Exception) {
            e.printStackTrace(System.out)
            false
        }
        if (success) {
            run(environment, irExpressionIterator)
        } else {
            run(environment)
        }
    }
}

class ReplStream(val inputStream: InputStream) : Iterator<Char> {
    override fun hasNext() = true
    override fun next(): Char {
        if (inputStream.available() == 0)
            print(">")
        return inputStream.read().toChar()
    }
}

fun LookaheadIterator<Char>.toIR(environment: Environment) = this
        .tokenize(environment)
        .parse(environment)
        .generateIR(environment)

class CharSequenceLookaheadIterator(var charSequence: CharSequence) : LookaheadIterator<Char> {
    override fun get(i: Int) = charSequence[i]
    override fun hasNext() = charSequence.isNotEmpty()
    override fun iterator() = charSequence.iterator()
    override fun drop(i: Int) {
        charSequence = charSequence.subSequence(i, charSequence.length)
    }
}

fun CharSequence.lookaheadIterator() = CharSequenceLookaheadIterator(this)
fun InputStream.lookaheadIterator() = iterator().lookaheadIterator()

operator fun InputStream.iterator() = object : Iterator<Char> {
    override fun hasNext() = this@iterator.available() != 0
    override fun next() = this@iterator.read().toChar()
}
