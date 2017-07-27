@file:Suppress("unused")

import m.*
import java.io.OutputStream
import java.io.PrintStream
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
                    val env = getDefaultEnvironment(out = PrintStream(output))

                    src
                            .iterator()
                            .lookaheadIterator()
                            .toIR(env)
                            .forEach { it.eval(env.memory) }

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
(print stdout "Hello, world!")
"""

val helloWorld2 by TestType.SuccessTest("Hello, world!") src """
(def hello "Hello, world!")
(print stdout hello)
"""

val helloWorld3 by TestType.SuccessTest("Hello, world!") src """
(def hello "Hello, world!")
(def test (lambda (x) (print stdout x)))
(test hello)
"""

val helloWorld4 by TestType.SuccessTest("Hello, world!") src """
(def hello "Hello")
(def comma ", ")
(def world "world")
(def exclamation "!")
(def test (lambda (x) (print stdout x)))
(def print4 (lambda (w x y z) (test w) (test x) (test y) (test z)))
(print4 hello comma world exclamation)
"""

// Not strictly necessary, since helloWorld4 should expand to this
val helloWorld5 by TestType.SuccessTest("Hello, world!") src """
(def hello "Hello")
(def comma ", ")
(def world "world")
(def exclamation "!")
(def test (lambda (x) (print stdout x)))
(def print4 (lambda (w) (lambda (x) (lambda (y) (lambda (z) (test w) (test x) (test y) (test z))))))
(print4 hello comma world exclamation)
"""

val helloWorld6 by TestType.SuccessTest("Hello, world!") src """
(print stdout (if true "Hello, world!" "error"))
"""

val helloWorld7 by TestType.SuccessTest("Hello, world!") src """
(print stdout (if (if false true false) "error" "Hello, world!"))
"""

val helloWorld8 by TestType.SuccessTest("Hello, world!") src """
(print stdout (if true "Hello, world!"))
"""

val helloWorld9 by TestType.SuccessTest("Hello, world!") src """
(def x "Hello")
(print stdout x)
(def x ", ")
(print stdout x)
(def x "world")
(print stdout x)
(def x "!")
(print stdout x)
"""

val helloWorld10 by TestType.SuccessTest("Hello, world!") src """
(def get
  (lambda (list x)
    (if (= x 0)
      (car list)
      (get (cdr list) (- x 1)))))

(def drop
  (lambda (list x)
    (if (= x 0)
      list
      (drop (cdr list) (- x 1)))))

(def let
  (macro
    (lambda (x)
      `((lambda (,(get x 0)) ~(drop x 2)) ,(get x 1)))))

(let hello "Hello, world!" (print stdout hello))
"""

val quote by TestType.SuccessTest("(nil 1 2 3 (4 5 6))") src """
(print stdout '(() 1 2 3 (4 5 6)))
"""

val list by TestType.SuccessTest("(1 2 3)") src """
(def nil?
  (lambda (object)
    (= object nil)))

(def list
  (macro
    (lambda (x)
      (if (nil? x)
        nil
        `(cons ,(car x) ,(list (cdr x)))))))

(print stdout (list 1 2 3))
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
(println stdout (class-of 0B))
(println stdout (class-of 0S))
(println stdout (class-of 0I))
(println stdout (class-of 0L))
(println stdout (class-of 0F))
(println stdout (class-of 0D))
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
(loop 10 (lambda (x) (print stdout x) (print stdout " : ") (println stdout (fib x))))
"""
