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
