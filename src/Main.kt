import m.*
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Created by Aedan Smith.
 */

fun main(args: Array<String>) {
    val env = getDefaultEnvironment(System.out)

    println(measureTimeMillis {
        File("main.m").inputStream()
                .lookaheadIterator()
                .tokenize(env)
                .noWhitespaceOrComments()
                .lookaheadIterator()
                .parse(env)
                .lookaheadIterator()
                .generateIR(env)
                .evaluate(env)
    })
}
