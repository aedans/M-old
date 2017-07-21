package m

/**
 * Created by Aedan Smith.
 */

interface LookaheadIterator<out T> : Iterable<T> {
    operator fun get(i: Int): T
    fun drop(i: Int)
    fun hasNext(): Boolean
}

class IteratorLookaheadIterator<out T>(private val iterator: Iterator<T>) : LookaheadIterator<T> {
    private var cache = ArrayList<T>()
    fun fillCache(i: Int) {
        while (cache.size <= i && iterator.hasNext())
            cache.add(iterator.next())
    }

    override fun get(i: Int): T {
        fillCache(i)
        return cache[i]
    }

    override fun hasNext() = cache.isNotEmpty() || iterator.hasNext()
    override tailrec fun drop(i: Int) {
        if (i != 0) {
            cache.removeAt(0)
            drop(i - 1)
        }
    }

    override fun iterator() = object : Iterator<T> {
        private var index = 0
        override fun hasNext() = cache.size > index || iterator.hasNext()
        override fun next() = get(index++)
    }
}

fun <T, R> LookaheadIterator<T>.collect(collector: (LookaheadIterator<T>) -> R): Iterator<R> = object : Iterator<R> {
    override fun hasNext() = this@collect.hasNext()
    override fun next(): R = collector(this@collect)
}

fun <T> Iterator<T>.lookaheadIterator() = IteratorLookaheadIterator(this)
fun <T> Iterable<T>.lookaheadIterator() = iterator().lookaheadIterator()
