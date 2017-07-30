package m

import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import kotlin.reflect.KClass

/**
 * Created by Aedan Smith.
 */

object Repl : Runnable {
    override fun run() {
        val env = getDefaultEnvironment()
        run(env)
    }

    private tailrec fun run(environment: RuntimeEnvironment,
                            irExpressionIterator: Iterator<IRExpression> = ReplStream(System.`in`, System.out)
                                    .toIR(environment)) {
        val success = try {
            irExpressionIterator.next().eval(environment.memory).takeIf { it != Unit }?.also { println(it) }
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
