package m

import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Created by Aedan Smith.
 */

fun main(args: Array<String>) = measureTimeMillis { when (args.size) {
    0 -> Repl.run()
    1 -> File(args[0]).interpret(getDefaultEnvironment())
    else -> throw Exception("Invalid arguments")
} }.let { println(it) }
