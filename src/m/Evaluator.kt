package m

/**
 * Created by Aedan Smith.
 */

sealed class MemoryLocation {
    abstract fun get(memory: Memory): Any
    abstract fun set(memory: Memory, any: Any)

    class HeapPointer(private val index: Int) : MemoryLocation() {
        @Suppress("HasPlatformType")
        override fun get(memory: Memory) = memory.heap[index]
        override fun set(memory: Memory, any: Any) = run {
            memory.heap.expand(index)
            memory.heap[index] = any
            Unit
        }
        override fun toString() = "*h$index"
    }

    class StackPointer(private val index: Int) : MemoryLocation() {
        override fun get(memory: Memory) = memory.stack[index]
        override fun set(memory: Memory, any: Any) = memory.stack.set(index, any)
        override fun toString() = "*s$index"
    }
}

class Stack {
    private val stack = ArrayList<Any>()
    operator fun get(location: Int) = stack[stack.size - 1 - location]
    operator fun set(location: Int, any: Any) {
        stack[location] = any
    }

    fun push(any: Any) = stack.add(any)
    fun pop() = stack.removeAt(stack.size - 1)
}

typealias Heap = ArrayList<Any>

tailrec fun Heap.expand(i: Int) {
    if (i >= size) {
        add(Nil)
        expand(i)
    }
}

data class Memory(val stack: Stack, val heap: Heap)

@Suppress("NOTHING_TO_INLINE", "HasPlatformType")
inline fun IdentifierIRExpression.evaluate(memory: Memory) = Intrinsics.evaluateIdentifier(this, memory)

fun DefIRExpression.evaluate(memory: Memory) = location.set(memory, expression.eval(memory))

fun LambdaIRExpression.evaluate(memory: Memory) = lambda(
        memory,
        closures.mapToArray { it.get(memory) },
        value,
        expressions
)

private inline fun <T> List<T>.mapToArray(func: (T) -> Any): Array<Any> {
    var index = 0
    return Array(size) { func(this[index++]) }
}

fun lambda(memory: Memory, closures: Array<Any>, value: IRExpression, expressions: List<IRExpression>) = { arg: Any ->
    closures.forEach { memory.stack.push(it) }
    memory.stack.push(arg)
    expressions.forEach { it.eval(memory) }
    val rValue = value.eval(memory)
    memory.stack.pop()
    closures.forEach { memory.stack.pop() }
    rValue
}

fun IfIRExpression.evaluate(memory: Memory) = if (condition.eval(memory) as Boolean)
    ifTrue.eval(memory)
else
    ifFalse.eval(memory)

@Suppress("NOTHING_TO_INLINE", "HasPlatformType")
inline fun InvokeIRExpression.evaluate(memory: Memory) = Intrinsics.evaluateInvoke(this, memory)

fun QuasiquoteIRExpression.evaluate(memory: Memory): Any {
    var cons: ConsList<*> = Nil
    irExpressions.forEach {
        when (it) {
            is UnquoteIRExpression -> {
                cons = ConsCell(it.eval(memory), cons)
            }
            is UnquoteSplicingIRExpression -> {
                it.eval(memory).let {
                    if (it is ConsList<*>) {
                        it.reversed().forEach {
                            cons = ConsCell(it, cons)
                        }
                    } else {
                        cons = ConsCell(it, cons)
                    }
                }
            }
            else -> {
                cons = ConsCell(it, cons)
            }
        }
    }
    return cons
}
