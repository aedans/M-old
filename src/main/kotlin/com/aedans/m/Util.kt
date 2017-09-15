package com.aedans.m

/**
 * Created by Aedan Smith.
 */

inline fun <reified T : Any> Any.takeIfInstance(): T? = takeIf { it is T } as T?

inline fun <T> List<T>.mapToArray(func: (T) -> Any) = Array(size) { func(this[it]) }

fun Iterator<Char>.toEvaluable(env: RuntimeEnvironment) = toIR(env).toEvaluable()
fun Iterator<Char>.toIR(env: RuntimeEnvironment) = this
        .tokenize()
        .parse()
        .expandMacros(env)
        .generateIR(env.symbolTable)
