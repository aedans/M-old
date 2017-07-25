package m

import java.io.OutputStream
import java.io.PrintStream
import kotlin.reflect.KClass

/**
 * Created by Aedan Smith.
 */

fun getDefaultEnvironment(out: OutputStream): RuntimeEnvironment {
    val env = RuntimeEnvironment(IRSymbolTable(), Memory())

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
