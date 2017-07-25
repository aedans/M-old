package m

/**
 * Created by Aedan Smith.
 */

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

class ConsCell(@JvmField val car: Any, @JvmField val cdr: Any) {
    override fun toString() = toString(true)
    fun toString(b: Boolean): String = if (b) "(${this.toString(false)})" else when (cdr) {
        Nil -> "$car"
        is ConsCell -> "$car ${cdr.toString(false)}"
        else -> "($car . $cdr)"
    }
}

fun Any.toConsTree(): Any = when (this) {
    is List<*> -> this.toConsTree()
    else -> this
}
fun List<*>.toConsTree(): Any = if (size == 0) Nil else ConsCell(first()!!.toConsTree(), drop(1).toConsTree())
