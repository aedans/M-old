package com.aedans.m

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
): RuntimeEnvironment {
    val env = RuntimeEnvironment(IRSymbolTable(), Memory(Heap(0) { Nil }, Stack()))

    env.setVar("true", true)
    env.setVar("false", false)
    env.setVar("nil", Nil)

    env.setVar("cons", mFunction<Any, ConsList<Any>, ConsList<Any>>(::ConsCell))
    env.setVar("car", mFunction(ConsList<Any>::car))
    env.setVar("cdr", mFunction(ConsList<Any>::cdr))

    env.setVar("!", mFunction(Boolean::not))
    env.setVar("|", mFunction(Boolean::or))
    env.setVar("&", mFunction(Boolean::and))
    env.setVar("=", mFunction(Any::equals))

    env.setVar("+i", mFunction<Int, Int, Int>(Int::plus))
    env.setVar("-i", mFunction<Int, Int, Int>(Int::minus))
    env.setVar("*i", mFunction<Int, Int, Int>(Int::times))
    env.setVar("/i", mFunction<Int, Int, Int>(Int::div))
    env.setVar("%i", mFunction<Int, Int, Int>(Int::rem))
    env.setVar("+l", mFunction<Long, Long, Long>(Long::plus))
    env.setVar("-l", mFunction<Long, Long, Long>(Long::minus))
    env.setVar("*l", mFunction<Long, Long, Long>(Long::times))
    env.setVar("/l", mFunction<Long, Long, Long>(Long::div))
    env.setVar("%l", mFunction<Long, Long, Long>(Long::rem))
    env.setVar("+f", mFunction<Float, Float, Float>(Float::plus))
    env.setVar("-f", mFunction<Float, Float, Float>(Float::minus))
    env.setVar("*f", mFunction<Float, Float, Float>(Float::times))
    env.setVar("/f", mFunction<Float, Float, Float>(Float::div))
    env.setVar("%f", mFunction<Float, Float, Float>(Float::rem))
    env.setVar("+d", mFunction<Double, Double, Double>(Double::plus))
    env.setVar("-d", mFunction<Double, Double, Double>(Double::minus))
    env.setVar("*d", mFunction<Double, Double, Double>(Double::times))
    env.setVar("/d", mFunction<Double, Double, Double>(Double::div))
    env.setVar("%d", mFunction<Double, Double, Double>(Double::rem))

    env.setVar("<i", mFunction { x: Int, y: Int -> x < y })
    env.setVar(">i", mFunction { x: Int, y: Int -> x > y })
    env.setVar("<l", mFunction { x: Long, y: Long -> x < y })
    env.setVar(">l", mFunction { x: Long, y: Long -> x > y })
    env.setVar("<f", mFunction { x: Float, y: Float -> x < y })
    env.setVar(">f", mFunction { x: Float, y: Float -> x > y })
    env.setVar("<d", mFunction { x: Double, y: Double -> x < y })
    env.setVar(">d", mFunction { x: Double, y: Double -> x > y })

    env.setVar("stdin", `in`)
    env.setVar("stdout", out)
    env.setVar("stderr", err)
    env.setVar("write", mFunction<OutputStream, Char, Unit> { p, c -> p.write(c.toInt()) })
    env.setVar("read", mFunction<InputStream, Char> { i -> i.read().toChar() })

    env.setVar("defmacro", Macro { it: Any ->
        val name = ((it as ConsList<*>).car as IdentifierExpression).name
        val lambda = it.cdr.car as ConsList<*>
        @Suppress("UNCHECKED_CAST")
        val macro = Macro(lambda.toIRExpression(env.symbolTable).toEvaluable().eval(env.memory) as MFunction)
        env.setVar(name, macro)
        Nil
    })

    env.setVar("include", Macro { it: Any ->
        val name = (it as ConsList<*>).car as String
        val file = File(name).absoluteFile
        if (file.isDirectory)
            file
                    .listFiles()
                    .map { listOf(IdentifierExpression("include"), "$file/${it.nameWithoutExtension}").toConsList() }
                    .let { ConsCell(IdentifierExpression("do"), it.toConsList()) }
        else
            File(file.absolutePath + ".m")
                    .reader()
                    .iterator()
                    .lookaheadIterator()
                    .parse()
                    .asSequence()
                    .toList()
                    .let { ConsCell(IdentifierExpression("do"), it.toConsList()) }
    })

    env.setVar("macroexpand", Macro { it: Any -> QuoteExpression((it as SExpression).car.expand(env)) })

    env.setVar("print", mFunction<OutputStream, Any, Unit> { p, o -> PrintStream(p).print(o) })
    env.setVar("println", mFunction<OutputStream, Any, Unit> { p, o -> PrintStream(p).println(o) })

    return env
}
