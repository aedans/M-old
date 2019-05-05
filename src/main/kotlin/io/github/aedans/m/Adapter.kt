package io.github.aedans.m

import io.github.aedans.kons.Cons
import io.github.aedans.kons.Nil
import io.github.aedans.kons.cons

/**
 * Created by Aedan Smith.
 */

typealias MFunction = (Any) -> Any

inline fun <reified I, reified O> mFunction(
        crossinline mFunction: (I) -> O
) = object : (I) -> O {
    override fun invoke(p1: I) = mFunction(p1)
    override fun toString() = "(${I::class.simpleName}) -> ${O::class.simpleName}"
}

inline fun <reified I1, reified I2, reified O> mFunction(
        crossinline mFunction: (I1, I2) -> O
) = object : (I1) -> (I2) -> O {
    override fun invoke(p1: I1): (I2) -> O = mFunction { i1: I2 -> mFunction(p1, i1) }
    override fun toString() = "(${I1::class.simpleName}) -> (${I2::class.simpleName}) -> ${O::class.simpleName}"
}

fun Any.toConsListOrSelf() = when (this) {
    is Iterator<*> -> @Suppress("UNCHECKED_CAST") (this as Iterator<Any>).toConsList()
    else -> this
}

fun <T : Any> Iterable<T>.toConsList() = iterator().toConsList()
fun <T : Any> Iterator<T>.toConsList(): Cons<T> = if (!hasNext()) Nil else next() cons toConsList()
