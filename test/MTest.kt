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

                    src.interpret(env)

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

val helloWorld3 by TestType.SuccessTest("Hello, world!") src """
(def hello "Hello, world!")
(def test (lambda (x) (print x)))
(test hello)
"""

val helloWorld4 by TestType.SuccessTest("Hello, world!") src """
(def hello "Hello")
(def comma ", ")
(def world "world")
(def exclamation "!")
(def print4 (lambda (w x y z) (print w) (print x) (print y) (print z)))
(print4 hello comma world exclamation)
"""

// Not strictly necessary, since helloWorld4 should expand to this
val helloWorld5 by TestType.SuccessTest("Hello, world!") src """
(def hello "Hello")
(def comma ", ")
(def world "world")
(def exclamation "!")
(def print4 (lambda (w) (lambda (x) (lambda (y) (lambda (z) (print w) (print x) (print y) (print z))))))
(print4 hello comma world exclamation)
"""
