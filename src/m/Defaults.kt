package m

import java.io.OutputStream
import java.io.PrintStream
import kotlin.reflect.KClass

/**
 * Created by Aedan Smith.
 */

fun getDefaultEnvironment(out: OutputStream): Environment {
    val env = GlobalEnvironment()

    env.setHeapValue(TOKENIZER_INDEX, mutableListOf(
            oParenTokenizer,
            cParenTokenizer,
            defTokenizer,
            lambdaTokenizer,
            ifTokenizer,
            trueTokenizer,
            falseTokenizer,
            whitespaceTokenizer,
            stringLiteralTokenizer,
            numberLiteralTokenizer,
            identifierTokenizer
    ))

    env.setHeapValue(PARSER_INDEX, mutableListOf(
            trueParser,
            falseParser,
            identifierParser,
            stringLiteralParser,
            numberLiteralParser,
            defParser,
            lambdaParser,
            ifParser,
            sExpressionParser
    ))

    env.setHeapValue(IR_GENERATOR_INDEX, mutableListOf(
            stringLiteralIRGenerator,
            numberLiteralIRGenerator,
            trueIRGenerator,
            falseIRGenerator,
            identifierIRGenerator,
            defIRGenerator,
            lambdaIRGenerator,
            ifIRGenerator,
            sExpressionIRGenerator
    ))

    env.setHeapValue(IDENTIFIER_IS_HEAD_INDEX, mFunction<Char, Boolean> {
        it in 'a'..'z' || it in 'A'..'Z' || it in "+-*/=<>!"
    })

    env.setHeapValue(IDENTIFIER_IS_TAIL_INDEX, mFunction<Char, Boolean> {
        it in '0'..'9' || it == '-' || it == '_' || it == ':'
    })

    env.setVar("print", mFunction<Any, Unit> { PrintStream(out).print(it) })
    env.setVar("println", mFunction<Any, Unit> { PrintStream(out).println(it) })
    env.setVar("class-of", mFunction<Any, KClass<*>> { it::class })
    env.setVar("!", mFunction<Boolean, Boolean> { !it })
    env.setVar("=", mFunction<Any, Any, Any> { x, y -> x == y })
    env.setVar("<", mFunction<Int, Int, Boolean> { x, y -> x < y })
    env.setVar(">", mFunction<Int, Int, Boolean> { x, y -> x > y })
    env.setVar("+", mFunction<Int, Int, Int> { x, y -> x + y })
    env.setVar("-", mFunction<Int, Int, Int> { x, y -> x - y })
    env.setVar("*", mFunction<Int, Int, Int> { x, y -> x * y })
    env.setVar("/", mFunction<Int, Int, Int> { x, y -> x / y })

    GlobalMemoryRegistry.addAllToTable(env)

    return env
}
