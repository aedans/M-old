package m

/**
 * Created by Aedan Smith.
 */

class LookaheadIterator<out T>(private val iterator: Iterator<T>) : Iterator<T> {
    private var cache = ArrayList<T>()
    private fun fillCache(i: Int) {
        while (cache.size <= i)
            cache.add(iterator.next())
    }

    operator fun get(i: Int): T {
        fillCache(i)
        return cache[i]
    }

    override fun hasNext() = cache.isNotEmpty() || iterator.hasNext()
    override fun next() = this[0].also { drop(1) }

    tailrec fun drop(i: Int) {
        if (i != 0) {
            cache.removeAt(0)
            drop(i - 1)
        }
    }
}

inline fun <T, R> LookaheadIterator<T>.collect(
        crossinline collector: (LookaheadIterator<T>) -> R
) = object : Iterator<R> {
    override fun hasNext() = this@collect.hasNext()
    override fun next(): R = collector(this@collect)
}

fun <T> Iterator<T>.lookaheadIterator() = LookaheadIterator(this)
