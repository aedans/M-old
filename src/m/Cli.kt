package m

import java.io.File

/**
 * Created by Aedan Smith.
 */

fun main(args: Array<String>) {
    when (args.size) {
        0 -> Repl.run()
        1 -> File(args[0]).interpret(getDefaultEnvironment())
        else -> throw Exception("Invalid arguments")
    }
}
