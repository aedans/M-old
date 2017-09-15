package com.aedans.m

import java.io.File
import java.io.Reader

/**
 * Created by Aedan Smith.
 */

operator fun Reader.iterator() = object : Iterator<Char> {
    override fun hasNext() = this@iterator.ready()
    override fun next() = this@iterator.read().toChar()
}

fun File.interpret(env: RuntimeEnvironment) = bufferedReader().iterator().interpret(env)
fun Iterator<Char>.interpret(env: RuntimeEnvironment) = this
        .toEvaluable(env)
        .forEach { it.eval(env.memory) }
