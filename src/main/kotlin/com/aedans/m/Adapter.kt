package com.aedans.m

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

interface ConsList<out T : Any> : Iterable<T> {
    val car: T
    val cdr: ConsList<T>

    val size get(): Int = cdr.size + 1

    override fun iterator() = object : Iterator<T> {
        private var it: ConsList<T> = this@ConsList
        override fun hasNext() = it !== Nil
        override fun next(): T {
            @Suppress("UNCHECKED_CAST")
            val it = it
            val next = it.car
            this.it = it.cdr
            return next
        }
    }

    fun toString(b: Boolean): String = if (b) "(${this.toString(false)})" else when (cdr) {
        Nil -> "$car"
        else -> "$car ${cdr.toString(false)}"
    }
}

object Nil : ConsList<Nothing> {
    override val car get() = throw IndexOutOfBoundsException()
    override val cdr get() = throw IndexOutOfBoundsException()
    override val size = 0
    override fun iterator() = emptyList<Nothing>().iterator()
    override fun toString() = "nil"
    override fun toString(b: Boolean) = toString()
}

data class ConsCell<out T : Any>(override val car: T, override val cdr: ConsList<T>) : ConsList<T> {
    override fun toString() = toString(true)
}

fun Any.toConsListOrSelf() = when (this) {
    is Iterator<*> -> @Suppress("UNCHECKED_CAST") (this as Iterator<Any>).toConsList()
    else -> this
}

operator fun <T : Any> ConsList<T>.get(i: Int): T = when (i) {
    0 -> car
    else -> cdr[i - 1]
}

fun <T : Any> Iterable<T>.toConsList() = iterator().toConsList()
fun <T : Any> Iterator<T>.toConsList(): ConsList<T> = if (!hasNext()) Nil else ConsCell(next(), toConsList())
