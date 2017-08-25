package m

import java.io.InputStream
import java.io.OutputStream
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

fun getDefaultRuntimeEnvironment(
        `in`: InputStream = System.`in`,
        out: OutputStream = System.out,
        err: OutputStream = System.err
): RuntimeEnvironment {
    val env = RuntimeEnvironment(IRSymbolTable(), Memory(Stack(), Heap()))

    env.setVar("true", true)
    env.setVar("false", false)
    env.setVar("nil", Nil)

    env.setVar("macro", mFunction(::mMacro))
    env.setVar("macroexpand", mMacro { QuoteExpression((it as SExpression).car.expand(env)) })

    env.setVar("cons", mFunction<Any, ConsList<Any>, ConsList<Any>>(::ConsCell))
    env.setVar("car", mFunction(ConsList<Any>::car))
    env.setVar("cdr", mFunction(ConsList<Any>::cdr))

    env.setVar("!", mFunction(Boolean::not))
    env.setVar("|", mFunction(Boolean::or))
    env.setVar("&", mFunction(Boolean::and))
    env.setVar("=", mFunction(Any::equals))
    env.setVar("<", mFunction { x: Int, y: Int -> x < y })
    env.setVar(">", mFunction { x: Int, y: Int -> x > y })

    env.setVar("+i", mFunction<Int, Int, Int>(Int::plus))
    env.setVar("-i", mFunction<Int, Int, Int>(Int::minus))
    env.setVar("*i", mFunction<Int, Int, Int>(Int::times))
    env.setVar("/i", mFunction<Int, Int, Int>(Int::div))
    env.setVar("+l", mFunction<Long, Long, Long>(Long::plus))
    env.setVar("-l", mFunction<Long, Long, Long>(Long::minus))
    env.setVar("*l", mFunction<Long, Long, Long>(Long::times))
    env.setVar("/l", mFunction<Long, Long, Long>(Long::div))
    env.setVar("+f", mFunction<Float, Float, Float>(Float::plus))
    env.setVar("-f", mFunction<Float, Float, Float>(Float::minus))
    env.setVar("*f", mFunction<Float, Float, Float>(Float::times))
    env.setVar("/f", mFunction<Float, Float, Float>(Float::div))
    env.setVar("+d", mFunction<Double, Double, Double>(Double::plus))
    env.setVar("-d", mFunction<Double, Double, Double>(Double::minus))
    env.setVar("*d", mFunction<Double, Double, Double>(Double::times))
    env.setVar("/d", mFunction<Double, Double, Double>(Double::div))

    listOf("+", "-", "*", "/").forEach {
        env.setVar(it, env.getVar(it + "i")!!)
    }

    env.setVar("stdin", `in`)
    env.setVar("stdout", out)
    env.setVar("stderr", err)
    env.setVar("write", mFunction<OutputStream, Char, Unit> { p, c -> p.write(c.toInt()) })
    env.setVar("read", mFunction<InputStream, Char> { i -> i.read().toChar() })

    env.setVar("print", mFunction<OutputStream, Any, Unit> { p, o -> PrintStream(p).print(o) })
    env.setVar("println", mFunction<OutputStream, Any, Unit> { p, o -> PrintStream(p).println(o) })
    env.setVar("class-of", mFunction<Any, KClass<*>> { it::class })

    return env
}
