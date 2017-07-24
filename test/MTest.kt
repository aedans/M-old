@file:Suppress("unused")

import m.*
import java.io.OutputStream
import javax.xml.stream.events.Characters
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
                            .toIR(env)
                            .forEach { it.eval(env) }

                    val oString = output.string.filter { it != '\r' && it != '\n' }
                    val eString = type.expected.filter { it != '\r' && it != '\n' }
                    if (oString != eString) {
                        System.err.println("Incorrect output for $name : $oString (expected $eString)")
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

val helloWorld6 by TestType.SuccessTest("Hello, world!") src """
(print (if true "Hello, world!" "error"))
"""

val helloWorld7 by TestType.SuccessTest("Hello, world!") src """
(print (if (if false true false) "error" "Hello, world!"))
"""

val helloWorld8 by TestType.SuccessTest("Hello, world!") src """
(print (if true "Hello, world!"))
"""

val helloWorld9 by TestType.SuccessTest("Hello, world!") src """
(def x "Hello")
(print x)
(def x ", ")
(print x)
(def x "world")
(print x)
(def x "!")
(print x)
"""

val quote by TestType.SuccessTest("(1 2 3)(1 2 3)") src """
(print (quote (1 2 3)))
(print '(1 2 3))
"""

val numberTokenizer by TestType.SuccessTest("""
class kotlin.Byte
class kotlin.Short
class kotlin.Int
class kotlin.Long
class kotlin.Float
class kotlin.Double
"""
) src """
(println (class-of 0B))
(println (class-of 0S))
(println (class-of 0I))
(println (class-of 0L))
(println (class-of 0F))
(println (class-of 0D))
"""

val fibonacci by TestType.SuccessTest("""
0 : 0
1 : 1
2 : 1
3 : 2
4 : 3
5 : 5
6 : 8
7 : 13
8 : 21
9 : 34
10 : 55
""") src """
(def fib (lambda (x) (if (= x 0) 0 (if (= x 1) 1 (+ (fib (- x 1)) (fib (- x 2)))))))
(def loop (lambda (x f) (if (= x 0) f (loop (- x 1) f)) (f x)))
(loop 10 (lambda (x) (print x) (print " : ") (println (fib x))))
"""
