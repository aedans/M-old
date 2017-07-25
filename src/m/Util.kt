package m

/**
 * Created by Aedan Smith.
 */

inline fun <reified T : Any> Any.takeIfInstance(): T? = if (this is T) this else null
