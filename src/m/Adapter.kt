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

object Nil {
    override fun toString() = "nil"
}

class ConsCell(@JvmField val car: Any, @JvmField val cdr: Any) : Iterable<Any> {
    val size get(): Int = if (cdr === Nil) 1 else (cdr as ConsCell).size + 1

    override fun iterator() = object : Iterator<Any> {
        var it: Any = this@ConsCell
        override fun hasNext() = it !== Nil
        override fun next(): Any {
            val it = it as ConsCell
            val next = it.car
            this.it = it.cdr
            return next
        }
    }

    operator fun get(i: Int): Any = when (i) {
        0 -> car
        else -> (cdr as ConsCell)[i - 1]
    }

    override fun toString() = toString(true)
    fun toString(b: Boolean): String = if (b) "(${this.toString(false)})" else when (cdr) {
        Nil -> "$car"
        is ConsCell -> "$car ${cdr.toString(false)}"
        else -> "($car . $cdr)"
    }
}

fun Any.toConsTree(): Any = when (this) {
    is Iterator<*> -> this.toConsTree()
    else -> this
}
fun Iterator<*>.toConsTree(): Any = if (!hasNext()) Nil else ConsCell(next()!!.toConsTree(), toConsTree())
