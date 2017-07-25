package m

import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream

/**
 * Created by Aedan Smith.
 */

object Repl : Runnable {
    override fun run() {
        val env = getDefaultEnvironment(System.out)
        run(env)
    }

    private tailrec fun run(environment: Environment,
                    irExpressionIterator: Iterator<IRExpression> = ReplStream(System.`in`, System.out)
                            .lookaheadIterator()
                            .toIR(environment)) {
        val success = try {
            irExpressionIterator.next().eval(environment).takeIf { it != Unit }?.also { println(it) }
            true
        } catch (t: Throwable) {
            t.printStackTrace(System.out)
            false
        }
        if (success) {
            run(environment, irExpressionIterator)
        } else {
            run(environment)
        }
    }
}

class ReplStream(val inputStream: InputStream, val printStream: PrintStream) : Iterator<Char> {
    override fun hasNext() = true
    override fun next(): Char {
        if (inputStream.available() == 0)
            printStream.print(">")
        return inputStream.read().toChar()
    }
}

fun LookaheadIterator<Char>.toIR(environment: Environment) = this
        .tokenize()
        .parse()
        .generateIR(environment)

operator fun InputStream.iterator() = object : Iterator<Char> {
    override fun hasNext() = this@iterator.available() != 0
    override fun next() = this@iterator.read().toChar()
}
