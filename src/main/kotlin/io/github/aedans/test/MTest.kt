@file:Suppress("unused")

package io.github.aedans.test

import io.github.aedans.m.getDefaultRuntimeEnvironment
import io.github.aedans.m.interpret
import io.github.aedans.m.mFunction
import java.io.OutputStream
import java.io.PrintStream
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
                    val env = getDefaultRuntimeEnvironment(out = PrintStream(output)).apply {
                        setVar("class-of", mFunction<Any, Class<*>> { it::class.java })
                    }

                    ("(include std)\n" + src).iterator().interpret(env)

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
(def test
  (lambda (x)
    (print stdout x)))

(test hello)
"""

val helloWorld4 by TestType.SuccessTest("Hello, world!") src """
(def hello "Hello")
(def comma ", ")
(def world "world")
(def exclamation "!")
(def test
  (lambda (x)
    (print stdout x)))

(def print4
  (lambda (w x y z)
    (test w)
    (test x)
    (test y)
    (test z)))

(print4 hello comma world exclamation)
"""

val helloWorld5 by TestType.SuccessTest("Hello, world!") src """
(def hello "Hello")
(def comma ", ")
(def world "world")
(def exclamation "!")
(def test
  (lambda (x)
    (print stdout x)))

(def print4
  (lambda (w)
    (lambda (x)
      (lambda (y)
        (lambda (z)
          (test w)
          (test x)
          (test y)
          (test z))))))

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
(def get
  (lambda (list x)
    (if (= x 0)
      (car list)
      (get (cdr list) (-i x 1)))))

(def drop
  (lambda (list x)
    (if (= x 0)
      list
      (drop (cdr list) (-i x 1)))))

(defmacro let
  (lambda (x)
    `((lambda (,(get x 0)) ~(drop x 2)) ,(get x 1))))

(let hello "Hello, world!" (print stdout hello))
"""

val helloWorld10 by TestType.SuccessTest("Hello, world!") src """
(defmacro proc
  (lambda (x)
    `(lambda (_) ~x)))

((proc (println stdout "Hello, world!"))
"""

val helloWorld11 by TestType.SuccessTest("Hello, world!") src """
(def a
  (lambda (x)
    (write stdout x)))

(a \H)
(a \e)
(a \l)
(a \l)
(a \o)
(a \,)
(a \ )
(a \w)
(a \o)
(a \r)
(a \l)
(a \d)
(a \!)
"""

val helloWorld12 by TestType.SuccessTest("Hello, world!") src """
(print stdout
  (do
    (print stdout "Hello, ")
    (print stdout "world")
    "!"))
"""

val helloWorld13 by TestType.SuccessTest("Hello, world!") src """
(do
  (def hello "Hello")
  (def comma ", ")
  (def world "world")
  (def exclamation "!"))

(def test
  (lambda (x)
    (print stdout x)))

(def print4
  (lambda (w x y z)
    (test w)
    (test x)
    (test y)
    (test z)))

(print4 hello comma world exclamation)
"""

val tailCall1 by TestType.SuccessTest("0") src """
(def recursion-test
  (lambda (x)
    (do
      (if (<i x 100000)
        (recursion-test (+i x 1))
        0))))

(println stdout (string (recursion-test 0)))
"""

val tailCall2 by TestType.SuccessTest("0") src """
(def recursion-test2)
(def recursion-test1
  (lambda (x)
    (if (<i x 100000)
      (do (recursion-test2 (+i x 1)))
      0)))

(def recursion-test2
  (lambda (x)
    (do
      (if (<i x 100000)
        (recursion-test1 (+i x 1))
        0))))

(println stdout (string (recursion-test1 0)))
"""

val tailCall3 by TestType.SuccessTest("1") src """
(def recursion-test2)
(def recursion-test1
  (lambda (x)
    (if (<i x 100000)
      (lambda (y) (recursion-test2 (+i x y) 1))
      (lambda (x) x))))

(def recursion-test2
  (lambda (x)
    (if (<i x 100000)
      (lambda (y) (recursion-test1 (+i x y) 1))
      (lambda (x) x))))

(println stdout (string (recursion-test1 0 1)))
"""

val quote by TestType.SuccessTest("[nil, 1, 2, 3, [4, 5, 6]]") src """
(print stdout (string '(() 1 2 3 (4 5 6))))
"""

val list by TestType.SuccessTest("[1, 2, 3]") src """
(defmacro list
  (lambda (x)
    (if (nil? x)
      nil
      `(cons ,(car x) (list ~(cdr x))))))

(print stdout (string (list 1 2 3)))
"""

val numberTokenizer by TestType.SuccessTest("""
class java.lang.Byte
class java.lang.Short
class java.lang.Integer
class java.lang.Long
class java.lang.Float
class java.lang.Double
"""
) src """
(println stdout (string (class-of 0b)))
(println stdout (string (class-of 0s)))
(println stdout (string (class-of 0i)))
(println stdout (string (class-of 0l)))
(println stdout (string (class-of 0f)))
(println stdout (string (class-of 0d)))
"""

val plusString by TestType.SuccessTest("(Int) -> (Int) -> Int") src """
(println stdout (string +i))
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
(def fib
  (lambda (x)
    (if (= x 0)
      0
    (if (= x 1)
      1
    (+i (fib (-i x 1)) (fib (-i x 2)))))))

(def loop
  (lambda (x f)
    (if (= x 0) f (loop (-i x 1) f)) (f x)))

(loop 10
  (lambda (x)
    (print stdout (string x))
    (print stdout " : ")
    (println stdout (string (fib x)))))
"""
