package m

/**
 * Created by Aedan Smith.
 */

inline fun <T, R : Any> List<T>.firstNonNull(function: (T) -> R?): R? {
    forEach {
        function(it)?.let {
            return it
        }
    }
    return null
}

inline fun <reified T : Any> Any.takeIfInstance(): T? = if (this is T) this else null
