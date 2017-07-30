package m

/**
 * Created by Aedan Smith.
 */

inline fun <reified T : Any> Any.takeIfInstance(): T? = if (this is T) this else null

fun Iterator<Char>.toIR(env: RuntimeEnvironment) = this
        .tokenize()
        .parse()
        .expandMacros(env)
        .generateIR(env.symbolTable)
