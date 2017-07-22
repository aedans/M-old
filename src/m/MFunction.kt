package m

/**
 * Created by Aedan Smith.
 */

fun <I, O> mFunction(mFunction: (I) -> O) = mFunction
inline fun <I1, I2, O> mFunction(crossinline mFunction: (I1, I2) -> O): (I1) -> (I2) -> O = { i1 ->
    { i2 ->
        mFunction(i1, i2)
    }
}
