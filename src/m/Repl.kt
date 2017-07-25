package m

import java.io.InputStream
import java.io.PrintStream

/**
 * Created by Aedan Smith.
 */

object Repl : Runnable {
    override fun run() {
        val env = getDefaultEnvironment(System.out)
        run(env)
    }

    private tailrec fun run(environment: RuntimeEnvironment,
                            irExpressionIterator: Iterator<IRExpression> = ReplStream(System.`in`, System.out)
                                    .lookaheadIterator()
                                    .toIR(environment.symbolTable)) {
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

fun LookaheadIterator<Char>.toIR(symbolTable: SymbolTable) = this
        .tokenize()
        .parse()
        .generateIR(symbolTable)

operator fun InputStream.iterator() = object : Iterator<Char> {
    override fun hasNext() = this@iterator.available() != 0
    override fun next() = this@iterator.read().toChar()
}

data class RuntimeEnvironment(val symbolTable: SymbolTable, val memory: Memory) {
    fun setVar(name: String, obj: Any) {
        val location = symbolTable.allocateLocation(name)
        symbolTable.setLocation(name, location)
        location.set(memory, obj)
    }
}
