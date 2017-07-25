package m

/**
 * Created by Aedan Smith.
 */

sealed class MemoryLocation {
    abstract fun get(memory: Memory): Any
    abstract fun set(memory: Memory, any: Any)

    class HeapPointer(val index: Int) : MemoryLocation() {
        override fun get(memory: Memory) = memory.heap[index]
        override fun set(memory: Memory, any: Any) = memory.heap.set(index, any)
        override fun toString() = "*h$index"
    }

    class StackPointer(val index: Int) : MemoryLocation() {
        override fun get(memory: Memory) = memory.stack[index]
        override fun set(memory: Memory, any: Any) = memory.stack.set(index, any)
        override fun toString() = "*s$index"
    }
}

class Stack {
    val stack = ArrayList<Any>()
    operator fun get(location: Int) = stack[stack.size - 1 - location]
    operator fun set(location: Int, any: Any) {
        stack[location] = any
    }

    fun push(any: Any){
        stack.add(any)
    }

    fun pop() {
        stack.removeAt(stack.size - 1)
    }
}

class Heap {
    val heap = ArrayList<Any>()
    private fun expand(i: Int) {
        while (heap.size <= i)
            heap.add(Nil)
    }

    operator fun get(location: Int): Any = heap[location]
    operator fun set(location: Int, obj: Any) {
        expand(location)
        heap[location] = obj
    }
}

data class Memory(val stack: Stack, val heap: Heap)

fun IdentifierIRExpression.evaluate(memory: Memory) = memoryLocation.get(memory)

fun DefIRExpression.evaluate(memory: Memory) {
    location.set(memory, expression.eval(memory))
}

fun LambdaIRExpression.evaluate(memory: Memory) = lambda(
        memory,
        closures.mapToArray { it.get(memory) },
        value,
        expressions
)

private inline fun <T> List<T>.mapToArray(func: (T) -> Any): Array<Any> {
    var index = 0
    val array = Array(size) { func(this[index++]) }
    return array
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
