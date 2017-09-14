package com.aedans.m

import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Created by Aedan Smith.
 */

fun main(args: Array<String>) = measureTimeMillis {
    when (args.size) {
        0 -> Repl.run()
        1 -> File(args[0]).interpret(getDefaultRuntimeEnvironment())
        else -> throw Exception("Invalid arguments ${args.toList()}")
    }
}.let {
    println("${args[0]} ran in ${it}ms")
}
