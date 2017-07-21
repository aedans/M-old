import m.getDefaultEnvironment
import m.interpret
import java.io.File

/**
 * Created by Aedan Smith.
 */

fun main(args: Array<String>) {
    File("main.m").interpret(getDefaultEnvironment(System.out))
}
