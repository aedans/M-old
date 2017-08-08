package m

/**
 * Created by Aedan Smith.
 */

typealias MFunction = (Any) -> Any

fun mMacro(mMacro: (Expression) -> Expression) = Macro(mMacro)

inline fun <reified I, reified O> mFunction(
        crossinline mFunction: (I) -> O
) = object : (I) -> O {
    override fun invoke(p1: I) = mFunction(p1)
    override fun toString() = "(${I::class.qualifiedName}) -> ${O::class.qualifiedName}"
}

inline fun <reified I1, reified I2, reified O> mFunction(
        crossinline mFunction: (I1, I2) -> O
) = object : (I1) -> (I2) -> O {
    override fun invoke(p1: I1): (I2) -> O = mFunction { i1: I2 -> mFunction(p1, i1) }
    override fun toString() = "(${I1::class.qualifiedName}) -> (${I2::class.qualifiedName}) -> ${O::class.simpleName}"
}

interface ConsList<out T : Any> : Iterable<T> {
    val size: Int
    val car: T
    val cdr: ConsList<T>
    operator fun get(i: Int): T
}

object Nil : ConsList<Nothing> {
    override val size = 0
    override fun iterator() = emptyList<Nothing>().iterator()
    override fun get(i: Int) = throw IndexOutOfBoundsException()
    override val car get() = throw IndexOutOfBoundsException()
    override val cdr get() = throw IndexOutOfBoundsException()
    override fun toString() = "nil"
}

class ConsCell<out T : Any>(override val car: T, override val cdr: ConsList<T>) : ConsList<T> {
    override val size get(): Int = (cdr as ConsList<*>).size + 1

    override fun iterator() = object : Iterator<T> {
        var it: Any = this@ConsCell
        override fun hasNext() = it !== Nil
        @Suppress("UNCHECKED_CAST")
        override fun next(): T {
            val it = it as ConsList<T>
            val next = it.car
            this.it = it.cdr
            return next
        }
    }

    override operator fun get(i: Int): T = when (i) {
        0 -> car
        else -> cdr[i - 1]
    }

    override fun toString() = toString(true)
    fun toString(b: Boolean): String = if (b) "(${this.toString(false)})" else when (cdr) {
        Nil -> "$car"
        is ConsCell<*> -> "$car ${cdr.toString(false)}"
        else -> "($car . $cdr)"
    }
}

@Suppress("UNCHECKED_CAST")
fun Any.toConsListOrSelf() = when (this) {
    is Iterator<*> -> (this as Iterator<Any>).toConsList()
    else -> this
}

fun <T : Any> Iterable<T>.toConsList() = iterator().toConsList()
fun <T : Any> Iterator<T>.toConsList(): ConsList<T> = if (!hasNext()) Nil else ConsCell(next(), toConsList())
