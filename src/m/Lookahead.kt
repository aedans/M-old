package m

/**
 * Created by Aedan Smith.
 */

interface LookaheadIterator<out T> : Iterator<T> {
    operator fun get(i: Int): T
    fun drop(i: Int)
    override fun hasNext(): Boolean
}

class IteratorLookaheadIterator<out T>(private val iterator: Iterator<T>) : LookaheadIterator<T> {
    private var cache = ArrayList<T>()
    private fun fillCache(i: Int) {
        while (cache.size <= i && iterator.hasNext())
            cache.add(iterator.next())
    }

    override fun get(i: Int): T {
        fillCache(i)
        return cache[i]
    }

    override fun hasNext() = cache.isNotEmpty() || iterator.hasNext()
    override fun next() = this[0].also { drop(1) }

    override tailrec fun drop(i: Int) {
        if (i != 0) {
            cache.removeAt(0)
            drop(i - 1)
        }
    }
}

fun <T, R> LookaheadIterator<T>.collect(collector: (LookaheadIterator<T>) -> R) = object : Iterator<R> {
    override fun hasNext() = this@collect.hasNext()
    override fun next(): R = collector(this@collect)
}

fun <T> Iterator<T>.lookaheadIterator() = IteratorLookaheadIterator(this)
