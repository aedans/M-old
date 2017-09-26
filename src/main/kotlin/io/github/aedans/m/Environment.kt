package io.github.aedans.m

import io.github.aedans.cons.Cons
import io.github.aedans.cons.Nil
import io.github.aedans.cons.cons
import io.github.aedans.cons.consOf
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream

/**
 * Created by Aedan Smith.
 */

data class RuntimeEnvironment(val symbolTable: SymbolTable, val memory: Memory) {
    fun getVar(name: String) = symbolTable.getLocation(name)?.toAccessible()?.get(memory)
    fun setVar(name: String, obj: Any) {
        val location = symbolTable.allocateLocation(name)
        symbolTable.setLocation(name, location)
        location.toAccessible().set(memory, obj)
    }
}

fun getDefaultRuntimeEnvironment(
        `in`: InputStream = System.`in`,
        out: OutputStream = System.out,
        err: OutputStream = System.err
) = RuntimeEnvironment(IRSymbolTable(), Memory(Heap(0) { Nil }, Stack())).apply {
    setVar("true", true)
    setVar("false", false)
    setVar("nil", Nil)

    setVar("cons", mFunction<Any, Cons<Any>, Cons<Any>> { a, b -> a cons b })
    setVar("car", mFunction(Cons<Any>::car))
    setVar("cdr", mFunction(Cons<Any>::cdr))

    setVar("!", mFunction(Boolean::not))
    setVar("|", mFunction(Boolean::or))
    setVar("&", mFunction(Boolean::and))
    setVar("=", mFunction(Any::equals))

    setVar("+i", mFunction<Int, Int, Int>(Int::plus))
    setVar("-i", mFunction<Int, Int, Int>(Int::minus))
    setVar("*i", mFunction<Int, Int, Int>(Int::times))
    setVar("/i", mFunction<Int, Int, Int>(Int::div))
    setVar("%i", mFunction<Int, Int, Int>(Int::rem))
    setVar("+l", mFunction<Long, Long, Long>(Long::plus))
    setVar("-l", mFunction<Long, Long, Long>(Long::minus))
    setVar("*l", mFunction<Long, Long, Long>(Long::times))
    setVar("/l", mFunction<Long, Long, Long>(Long::div))
    setVar("%l", mFunction<Long, Long, Long>(Long::rem))
    setVar("+f", mFunction<Float, Float, Float>(Float::plus))
    setVar("-f", mFunction<Float, Float, Float>(Float::minus))
    setVar("*f", mFunction<Float, Float, Float>(Float::times))
    setVar("/f", mFunction<Float, Float, Float>(Float::div))
    setVar("%f", mFunction<Float, Float, Float>(Float::rem))
    setVar("+d", mFunction<Double, Double, Double>(Double::plus))
    setVar("-d", mFunction<Double, Double, Double>(Double::minus))
    setVar("*d", mFunction<Double, Double, Double>(Double::times))
    setVar("/d", mFunction<Double, Double, Double>(Double::div))
    setVar("%d", mFunction<Double, Double, Double>(Double::rem))

    setVar("<i", mFunction { x: Int, y: Int -> x < y })
    setVar(">i", mFunction { x: Int, y: Int -> x > y })
    setVar("<l", mFunction { x: Long, y: Long -> x < y })
    setVar(">l", mFunction { x: Long, y: Long -> x > y })
    setVar("<f", mFunction { x: Float, y: Float -> x < y })
    setVar(">f", mFunction { x: Float, y: Float -> x > y })
    setVar("<d", mFunction { x: Double, y: Double -> x < y })
    setVar(">d", mFunction { x: Double, y: Double -> x > y })

    setVar("stdin", `in`)
    setVar("stdout", out)
    setVar("stderr", err)
    setVar("write", mFunction<OutputStream, Char, Unit> { p, c -> p.write(c.toInt()) })
    setVar("read", mFunction<InputStream, Char> { i -> i.read().toChar() })

    setVar("defmacro", Macro { it: Any ->
        val name = ((it as Cons<*>).car as IdentifierExpression).name
        val lambda = it.cdr.car as Cons<*>
        @Suppress("UNCHECKED_CAST")
        val macro = Macro(lambda.toIRExpression(symbolTable).toEvaluable().eval(memory) as MFunction)
        setVar(name, macro)
        Nil
    })

    setVar("include", Macro { it: Any ->
        val name = (it as Cons<*>).car as String
        val file = File(name).absoluteFile
        if (file.isDirectory)
            file
                    .listFiles()
                    .map { consOf(IdentifierExpression("include"), "$file/${it.nameWithoutExtension}") }
                    .let { IdentifierExpression("do") cons it.toConsList() }
        else
            File(file.absolutePath + ".m")
                    .reader()
                    .iterator()
                    .lookaheadIterator()
                    .parse()
                    .asSequence()
                    .toList()
                    .let { IdentifierExpression("do") cons it.toConsList() }
    })

    setVar("macroexpand", Macro { it: Any ->
        @Suppress("UNCHECKED_CAST")
        QuoteExpression((it as SExpression).car.expand(this))
    })

    setVar("print", mFunction<OutputStream, Any, Unit> { p, o -> PrintStream(p).print(o) })
    setVar("println", mFunction<OutputStream, Any, Unit> { p, o -> PrintStream(p).println(o) })
}
