package m

import java.io.InputStream
import java.io.PrintStream
import kotlin.reflect.KClass

/**
 * Created by Aedan Smith.
 */

data class RuntimeEnvironment(val symbolTable: SymbolTable, val memory: Memory) {
    fun getVar(name: String) = symbolTable.getLocation(name)?.get(memory)
    fun setVar(name: String, obj: Any) {
        val location = symbolTable.allocateLocation(name)
        symbolTable.setLocation(name, location)
        location.set(memory, obj)
    }
}

fun getDefaultEnvironment(
        `in`: InputStream = System.`in`,
        out: PrintStream = System.out,
        err: PrintStream = System.err
): RuntimeEnvironment {
    val env = RuntimeEnvironment(IRSymbolTable(), Memory(Stack(), Heap()))

    env.setVar("macro", mFunction<MFunction, Macro> { mMacro(it) })
    env.setVar("macroexpand", mMacro { QuoteExpression((it as SExpression).car.expand(env)) })

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

    env.setVar("stdin", `in`)
    env.setVar("stdout", out)
    env.setVar("stderr", err)
    env.setVar("print", mFunction<PrintStream, Any, Unit> { p, o -> p.print(o) })
    env.setVar("println", mFunction<PrintStream, Any, Unit> { p, o -> p.println(o) })

    env.setVar("class-of", mFunction<Any, KClass<*>> { it::class })

    return env
}
