@file:Suppress("unused")

import m.*
import java.io.OutputStream
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Created by Aedan Smith.
 */

class StringOutputStream : OutputStream() {
    var string = ""
    override fun toString() = string
    override fun write(b: Int) {
        string += b.toChar()
    }
}

sealed class TestType {
    class SuccessTest(val expected: String) : TestType() {
        infix fun src(src: String) = TestRegistry.Test(this, src)
    }
}

object TestRegistry {
    val tests = mutableListOf<Triple<TestType, String, String>>()

    class Test(val testType: TestType, val src: String) {
        operator fun provideDelegate(nothing: Nothing?, property: KProperty<*>) =
                object : ReadOnlyProperty<Nothing?, Unit> {
            init {
                tests.add(Triple(testType, property.name, src))
            }

            @Suppress("NAME_SHADOWING")
            override operator fun getValue(thisRef: Nothing?, property: KProperty<*>) = Unit
        }
    }
}

fun main(args: Array<String>) {
    TestRegistry.tests.forEach { (type, name, src) ->
        try {
            when (type) {
                is TestType.SuccessTest -> {
                    val output = StringOutputStream()
                    val env = getDefaultEnvironment(output)

                    src
                            .lookaheadIterator()
                            .tokenize(env)
                            .noWhitespaceOrComments()
                            .lookaheadIterator()
                            .parse(env)
                            .lookaheadIterator()
                            .generateIR(env)
                            .evaluate(env)

                    if (output.string != type.expected) {
                        System.err.println("Incorrect output for $name : $output (expected ${type.expected})")
                    } else {
                        println("Passed test $name")
                    }
                }
            }
        } catch (t: Throwable) {
            System.err.println("Exception in test $name")
            t.printStackTrace()
        }
    }
}

val helloWorld1 by TestType.SuccessTest("Hello, world!") src """
(print "Hello, world!")
"""

val helloWorld2 by TestType.SuccessTest("Hello, world!") src """
(def hello "Hello, world!")
(print hello)
"""
