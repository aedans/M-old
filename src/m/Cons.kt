package m

/**
 * Created by Aedan Smith.
 */

object Nil {
    override fun toString() = "nil"
}

class ConsCell(@JvmField val car: Any, @JvmField val cdr: Any) {
    override fun toString() = "($car . $cdr)"
}

fun List<Any>.toConsList(): Any = if (size == 0) Nil else ConsCell(first(), drop(1).toConsList())
