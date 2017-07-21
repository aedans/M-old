package m

import java.io.File

/**
 * Created by Aedan Smith.
 */

fun String.interpret(environment: Environment) = lookaheadIterator().interpret(environment)
fun File.interpret(environment: Environment) = inputStream().lookaheadIterator().interpret(environment)
fun LookaheadIterator<Char>.interpret(environment: Environment) {
    this
            .lookaheadIterator()
            .tokenize(environment)
            .noWhitespaceOrComments()
            .lookaheadIterator()
            .parse(environment)
            .lookaheadIterator()
            .generateIR(environment)
            .evaluate(environment)
}