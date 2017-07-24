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
            // Chars
            oParenTokenizer,
            cParenTokenizer,
            apostropheTokenizer,
            // Whitespace
            whitespaceTokenizer,
            // Literals
            stringLiteralTokenizer,
            numberLiteralTokenizer,
            // Identifiers
            identifierTokenizer
    ))

    env.setHeapValue(PARSER_INDEX, mutableListOf(
            // Tokens
            identifierParser,
            stringLiteralParser,
            numberLiteralParser,
            // Sugar
            apostropheParser,
            // SExpressions
            sExpressionParser
    ))

    env.setHeapValue(IR_GENERATOR_INDEX, mutableListOf(
            // Literals
            stringLiteralIRGenerator,
            numberLiteralIRGenerator,
            // Identifier
            identifierIRGenerator,
            // Fundamentals
            defIRGenerator,
            lambdaIRGenerator,
            ifIRGenerator,
            quoteIRGenerator,
            // Invoke
            invokeIRGenerator
    ))

    env.setHeapValue(IDENTIFIER_IS_HEAD_INDEX, mFunction<Char, Boolean> {
        it in 'a'..'z' || it in 'A'..'Z' || it in "+-*/=<>!?#"
    })

    env.setHeapValue(IDENTIFIER_IS_TAIL_INDEX, mFunction<Char, Boolean> {
        it in '0'..'9' || it == '-' || it == '_' || it == ':'
    })

    env.setVar("#t", true)
    env.setVar("#f", false)
    env.setVar("nil", Nil)

    env.setVar("cons", mFunction<Any, Any, ConsCell> { car, cdr -> ConsCell(car, cdr) })
    env.setVar("car", mFunction<ConsCell, Any> { it.car })
    env.setVar("cdr", mFunction<ConsCell, Any> { it.cdr })

    env.setVar("!", mFunction<Boolean, Boolean> { !it })
    env.setVar("=", mFunction<Any, Any, Any> { x, y -> x == y })
    env.setVar("<", mFunction<Int, Int, Boolean> { x, y -> x < y })
    env.setVar(">", mFunction<Int, Int, Boolean> { x, y -> x > y })
    env.setVar("+", mFunction<Int, Int, Int> { x, y -> x + y })
    env.setVar("-", mFunction<Int, Int, Int> { x, y -> x - y })
    env.setVar("*", mFunction<Int, Int, Int> { x, y -> x * y })
    env.setVar("/", mFunction<Int, Int, Int> { x, y -> x / y })

    env.setVar("print", mFunction<Any, Unit> { PrintStream(out).print(it) })
    env.setVar("println", mFunction<Any, Unit> { PrintStream(out).println(it) })
    env.setVar("class-of", mFunction<Any, KClass<*>> { it::class })

    return env
}
