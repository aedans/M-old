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
        val env = getDefaultEnvironment(System.out)
        run(env)
    }

    private tailrec fun run(environment: RuntimeEnvironment,
                            irExpressionIterator: Iterator<IRExpression> = ReplStream(System.`in`, System.out)
                                    .lookaheadIterator()
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

fun LookaheadIterator<Char>.toIR(env: RuntimeEnvironment) = this
        .tokenize()
        .parse()
        .expandMacros(env)
        .generateIR(env.symbolTable)

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

fun getDefaultEnvironment(out: OutputStream): RuntimeEnvironment {
    val env = RuntimeEnvironment(IRSymbolTable(), Memory(Stack(), Heap()))

    env.setVar("true", true)
    env.setVar("false", false)
    env.setVar("nil", Nil)

    env.setVar("cons", mFunction<Any, Any, ConsCell> { car, cdr -> ConsCell(car, cdr) })
    env.setVar("car", mFunction<ConsCell, Any> { it.car })
    env.setVar("cdr", mFunction<ConsCell, Any> { it.cdr })

    env.setVar("!", mFunction<Boolean, Boolean> { !it })
    env.setVar("|", mFunction<Boolean, Boolean, Boolean> { x, y -> x || y })
    env.setVar("&", mFunction<Boolean, Boolean, Boolean> { x, y -> x && y })

    env.setVar("=", mFunction<Any, Any, Boolean> { x, y -> x == y })
    env.setVar("<", mFunction<Int, Int, Boolean> { x, y -> x < y })
    env.setVar(">", mFunction<Int, Int, Boolean> { x, y -> x > y })
    env.setVar("+", mFunction<Int, Int, Int> { x, y -> x + y })
    env.setVar("-", mFunction<Int, Int, Int> { x, y -> x - y })
    env.setVar("*", mFunction<Int, Int, Int> { x, y -> x * y })
    env.setVar("/", mFunction<Int, Int, Int> { x, y -> x / y })

    env.setVar("print", mFunction<Any, Unit> { PrintStream(out).print(it) })
    env.setVar("println", mFunction<Any, Unit> { PrintStream(out).println(it) })
    env.setVar("class-of", mFunction<Any, KClass<*>> { it::class })

    return env
}
