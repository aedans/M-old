package m

import java.io.OutputStream
import java.io.PrintStream
import kotlin.reflect.KClass

/**
 * Created by Aedan Smith.
 */

fun getDefaultEnvironment(out: OutputStream): Environment {
    val env = Environment(VirtualMemory(), SymbolTable())

    env.virtualMemory[TOKENIZER_INDEX] = mutableListOf(
            oParenTokenizer,
            cParenTokenizer,
            whitespaceTokenizer,
            stringLiteralTokenizer,
            numberLiteralTokenizer,
            defTokenizer,
            lambdaTokenizer,
            ifTokenizer,
            trueTokenizer,
            falseTokenizer,
            identifierTokenizer
    )

    env.virtualMemory[PARSER_INDEX] = mutableListOf(
            sExpressionParser,
            stringLiteralParser,
            numberLiteralParser,
            defParser,
            lambdaParser,
            ifParser,
            trueParser,
            falseParser,
            identifierParser
    )

    env.virtualMemory[IR_GENERATOR_INDEX] = mutableListOf(
            stringLiteralIRGenerator,
            numberLiteralIRGenerator,
            identifierIRGenerator,
            defIRGenerator,
            lambdaIRGenerator,
            ifIRGenerator,
            trueIRGenerator,
            falseIRGenerator,
            sExpressionIRGenerator
    )

    env.virtualMemory[EVALUATOR_INDEX] = mutableListOf(
            identifierEvaluator,
            defEvaluator,
            lambdaEvaluator,
            ifEvaluator,
            invokeEvaluator
    )

    env.virtualMemory[IDENTIFIER_IS_HEAD_INDEX] = mFunction<Char, Boolean> {
        it in 'a'..'z' || it in 'A'..'Z' || it in "+-*/=<>!"
    }

    env.virtualMemory[IDENTIFIER_IS_TAIL_INDEX] = mFunction<Char, Boolean> {
        it in '0'..'9' || it == '-' || it == '_' || it == ':'
    }

    env["print"] = mFunction<Any, Unit> { PrintStream(out).print(it) }
    env["println"] = mFunction<Any, Unit> { PrintStream(out).println(it) }
    env["class-of"] = mFunction<Any, KClass<*>> { it::class }
    env["!"] = mFunction<Boolean, Boolean> { !it }
    env["="] = mFunction<Any, Any, Any> { x, y -> x == y }
//    env["<"] = mFunction<Int, Int, Boolean> { x, y -> x < y }
//    env[">"] = mFunction<Int, Int, Boolean> { x, y -> x > y }
//    env["+"] = mFunction<Int, Int, Int> { x, y -> x + y }
//    env["-"] = mFunction<Int, Int, Int> { x, y -> x - y }
//    env["*"] = mFunction<Int, Int, Int> { x, y -> x * y }
//    env["/"] = mFunction<Int, Int, Int> { x, y -> x / y }

    GlobalMemoryRegistry.addAllToTable(env.symbolTable)

    return env
}
