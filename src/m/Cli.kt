package m

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

/**
 * Created by Aedan Smith.
 */

typealias Language = (MArgs) -> Unit

private val languages = mapOf(
        "i" to LANGUAGE_INTERPRETED
)

class MArgs(parser: ArgParser) {
    val source by parser.positional("SOURCE", help = "Source filename")
    val output by parser.storing("-o", "--output", help = "Output filename")
            .default("a")
    val language by parser.storing("-x", help = "Language target; i = interpreted")
            .default("i")
}

fun main(args: Array<String>) {
    if (args.isEmpty()) Repl.run() else {
        val mArgs = MArgs(ArgParser(args, helpFormatter = null))
        languages[mArgs.language]?.invoke(mArgs)
                ?: throw Exception("${mArgs.language} is not a valid language target")
    }
}
