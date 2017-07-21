package m

import java.io.OutputStream
import java.io.PrintStream

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
            defTokenizer,
            lambdaTokenizer,
            identifierTokenizer
    )

    env.virtualMemory[PARSER_INDEX] = mutableListOf(
            sExpressionParser,
            defParser,
            lambdaParser,
            identifierParser,
            stringLiteralParser
    )

    env.virtualMemory[IR_GENERATOR_INDEX] = mutableListOf(
            stringLiteralIRGenerator,
            identifierIRGenerator,
            defIRGenerator,
            lambdaIRGenerator,
            sExpressionIRGenerator
    )

    env.virtualMemory[EVALUATOR_INDEX] = mutableListOf(
            identifierEvaluator,
            defEvaluator,
            lambdaEvaluator,
            invokeEvaluator
    )

    env.virtualMemory[IDENTIFIER_IS_HEAD_INDEX] = mFunction<Char, Boolean> {
        it in 'a'..'z' || it in 'A'..'Z'
    }

    env.virtualMemory[IDENTIFIER_IS_TAIL_INDEX] = mFunction<Char, Boolean> {
        it in '0'..'9' || it == '-' || it == '_' || it == ':'
    }

    env["print"] = { x: Any -> PrintStream(out).print(x) }

    GlobalMemoryRegistry.addAllToTable(env.symbolTable)

    return env
}
