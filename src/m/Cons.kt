package m

/**
 * Created by Aedan Smith.
 */

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
