package com.aedans.m

/**
 * Created by Aedan Smith.
 */

inline fun <reified T : Any> Any.takeIfInstance(): T? = takeIf { it is T } as T?

fun Iterator<Char>.toIR(env: RuntimeEnvironment) = this
        .tokenize()
        .parse()
        .expandMacros(env)
        .generateIR(env.symbolTable)