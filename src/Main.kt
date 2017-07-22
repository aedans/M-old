import m.getDefaultEnvironment
import m.interpret
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Created by Aedan Smith.
 */

fun main(args: Array<String>) {
    println(measureTimeMillis {
        File("main.m").interpret(getDefaultEnvironment(System.out))
    })
}
