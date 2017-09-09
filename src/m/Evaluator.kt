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
        override fun set(memory: Memory, any: Any) = let {
            memory.heap.expand(index)
            memory.heap[index] = any
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

interface StackSafeMFunction : MFunction {
    fun invokeStackSafe(arg: Any): Trampoline
}

sealed class Trampoline {
    abstract val it: Any
    data class Just(override val it: Any) : Trampoline()
    data class Deferred(private val func: () -> Trampoline) : Trampoline() {
        override val it get() = run {
            tailrec fun getIt(trampoline: Trampoline): Any = when (trampoline) {
                is Just -> trampoline.it
                is Deferred -> getIt(trampoline.func())
            }
            getIt(func())
        }
    }
}

@Suppress("NOTHING_TO_INLINE", "HasPlatformType")
inline fun IdentifierIRExpression.evaluate(memory: Memory) = Intrinsics.evaluateIdentifier(this, memory)

fun DefIRExpression.evaluate(memory: Memory) = location.set(memory, expression.eval(memory))

private inline fun <T> List<T>.mapToArray(func: (T) -> Any): Array<Any> {
    var index = 0
    return Array(size) { func(this[index++]) }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Memory.push(closures: Array<Any>, arg: Any) {
    for (it in closures) {
        stack.push(it)
    }
    stack.push(arg)
}

@Suppress("NOTHING_TO_INLINE")
inline fun Memory.pop(num: Int) {
    for (it in 0..num) {
        stack.pop()
    }
}

fun LambdaIRExpression.evaluate(memory: Memory): MFunction {
    val closures = closures.mapToArray { it.get(memory) }
    return if (isTailCall) {
        object : StackSafeMFunction {
            override fun invoke(arg: Any): Any {
                memory.push(closures, arg)
                expressions.forEach { it.eval(memory) }
                val ret = value.eval(memory)
                memory.pop(closures.size)
                return ret
            }

            override fun invokeStackSafe(arg: Any): Trampoline {
                memory.push(closures, arg)
                expressions.forEach { it.eval(memory) }
                val ret = value.evalT(memory)
                memory.pop(closures.size)
                return ret
            }
        }
    } else {
        { arg ->
            memory.push(closures, arg)
            expressions.forEach { it.eval(memory) }
            val rValue = value.eval(memory)
            memory.pop(closures.size)
            rValue
        }
    }
}

fun IfIRExpression.evaluate(memory: Memory) = if (condition.eval(memory) as Boolean)
    ifTrue.eval(memory)
else
    ifFalse.eval(memory)

fun IfIRExpression.evaluateT(memory: Memory) = if (condition.eval(memory) as Boolean)
    ifTrue.evalT(memory)
else
    ifFalse.evalT(memory)

@Suppress("NOTHING_TO_INLINE", "HasPlatformType")
inline fun InvokeIRExpression.evaluate(memory: Memory) = Intrinsics.evaluateInvoke(this, memory)

fun InvokeIRExpression.evaluateT(memory: Memory): Trampoline {
    val expression = expression.eval(memory)
    val arg = arg.eval(memory)
    return Trampoline.Deferred {
        when (expression) {
            is StackSafeMFunction -> Trampoline.Deferred { expression.invokeStackSafe(arg) }
            else -> Trampoline.Just(Intrinsics.evaluateCall(expression, arg))
        }
    }
}

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
