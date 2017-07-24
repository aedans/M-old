package m

/**
 * Created by Aedan Smith.
 */

inline fun <reified I, reified O> mFunction(
        noinline mFunction: (I) -> O
) = MFunctionWrapper0(
        mFunction,
        I::class.qualifiedName!!,
        O::class.qualifiedName!!
)

inline fun <reified I1, reified I2, reified O> mFunction(
        noinline mFunction: (I1, I2) -> O
) = MFunctionWrapper1(
        mFunction,
        I1::class.qualifiedName!!,
        I2::class.qualifiedName!!,
        O::class.qualifiedName!!
)

class MFunctionWrapper0<in I, out O>(
        val mFunction: (I) -> O,
        val i: String,
        val o: String
) : (I) -> O {
    override fun invoke(p1: I) = mFunction(p1)
    override fun toString() = "($i) -> $o"
}

class MFunctionWrapper1<in I1, in I2, out O>(
        val mFunction: (I1, I2) -> O,
        val i1: String,
        val i2: String,
        val o: String
) : (I1) -> (I2) -> O {
    override fun invoke(p1: I1): (I2) -> O = MFunctionWrapper0({ mFunction(p1, it) }, i2, o)
    override fun toString() = "($i1) -> ($i2) -> $o"
}
