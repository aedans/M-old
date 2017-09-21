package io.github.aedans.m

/**
 * Created by Aedan Smith.
 */

sealed class MemoryLocation {
    data class HeapPointer(val index: Int) : MemoryLocation() {
        override fun toString() = "*h$index"
    }

    data class StackPointer(val index: Int) : MemoryLocation() {
        override fun toString() = "*s$index"
    }
}

interface Accessible {
    fun get(memory: Memory): Any
    fun set(memory: Memory, any: Any)
}

fun MemoryLocation.toAccessible() = when (this) {
    is MemoryLocation.HeapPointer -> object : Accessible {
        @Suppress("HasPlatformType")
        override fun get(memory: Memory) = memory.heap[index]
        override fun set(memory: Memory, any: Any) = let {
            memory.heap = memory.heap.expand(index)
            memory.heap[index] = any
        }
    }
    is MemoryLocation.StackPointer -> object : Accessible {
        override fun get(memory: Memory) = memory.stack[index]
        override fun set(memory: Memory, any: Any) = memory.stack.set(index, any)
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
    tailrec fun pop(i: Int) {
        if (i > 0) {
            pop()
            pop(i - 1)
        }
    }
}

typealias Heap = Array<Any>

fun Heap.expand(i: Int) = if (i < size) {
    this
} else {
    val newArray = Array((i + 1) * 2) { Nil as Any }
    System.arraycopy(this, 0, newArray, 0, size)
    newArray
}

class Memory(@JvmField var heap: Heap, @JvmField val stack: Stack)

interface StackSafeMFunction : MFunction {
    fun invokeStackSafe(arg: Any): Trampoline
}

sealed class Trampoline {
    abstract val it: Any
    data class Just(override val it: Any) : Trampoline()
    data class Deferred(private val func: () -> Trampoline) : Trampoline() {
        private tailrec fun getIt(trampoline: Trampoline): Any = when (trampoline) {
            is Just -> trampoline.it
            is Deferred -> getIt(trampoline.func())
        }

        override val it get() = getIt(func())
    }
}

interface Evaluable {
    fun eval(memory: Memory): Any
    fun evalT(memory: Memory): Trampoline = Trampoline.Just(eval(memory))

    companion object {
        inline operator fun invoke(crossinline eval: (Memory) -> Any) = object : Evaluable {
            override fun eval(memory: Memory) = eval(memory)
        }
    }
}

fun Iterator<IRExpression>.toEvaluable() = asSequence()
        .map { it.toEvaluable() }
        .iterator()

fun IRExpression.toEvaluable(): Evaluable = when (this) {
    is PureIRExpression -> toEvaluable()
    is LiteralIRExpression -> toEvaluable()
    is IdentifierIRExpression -> toEvaluable()
    is DefIRExpression -> toEvaluable()
    is InvokeIRExpression -> toEvaluable()
    is LambdaIRExpression -> toEvaluable()
    is IfIRExpression -> toEvaluable()
    is DoIRExpression -> toEvaluable()
    is QuasiquoteIRExpression -> toEvaluable()
    is BinaryOperatorIRExpression -> toEvaluable()
    is UnquoteIRExpression -> throw Exception("Unquote cannot be evaluated in a non-quasiquoted context")
    is UnquoteSplicingIRExpression -> throw Exception("UnquoteSplicing cannot be evaluated in a non-quasiquoted context")
}

fun PureIRExpression.toEvaluable() = irExpression.toEvaluable().optimizePure()

fun Evaluable.optimizePure() = object : Evaluable {
    var value: Any? = null
    override fun eval(memory: Memory): Any = value?.let { it } ?: run {
        value = this@optimizePure.eval(memory)
        value!!
    }
}

fun LiteralIRExpression.toEvaluable() = object : Evaluable {
    override fun eval(memory: Memory) = literal
}

fun IdentifierIRExpression.toEvaluable() = object : Evaluable {
    @JvmField val accessibleMemoryLocation = memoryLocation.toAccessible()

    override fun eval(memory: Memory) = accessibleMemoryLocation.get(memory)
}

fun DefIRExpression.toEvaluable() = object : Evaluable {
    @JvmField val accessibleMemoryLocation = memoryLocation.toAccessible()
    @JvmField val evaluableExpression = expression.toEvaluable()

    override fun eval(memory: Memory) {
        accessibleMemoryLocation.set(memory, evaluableExpression.eval(memory))
    }
}

fun InvokeIRExpression.toEvaluable() = object : Evaluable {
    @JvmField val evaluableExpression = expression.toEvaluable()
    @JvmField val evaluableArg = arg.toEvaluable()

    override fun eval(memory: Memory) = Intrinsics.evaluateCall(
            evaluableExpression.eval(memory),
            evaluableArg.eval(memory)
    )

    override fun evalT(memory: Memory): Trampoline {
        val expression = evaluableExpression.eval(memory)
        val arg = evaluableArg.eval(memory)
        return if (expression is StackSafeMFunction)
            Trampoline.Deferred { expression.invokeStackSafe(arg) }
        else
            Trampoline.Just(Intrinsics.evaluateCall(expression, arg))
    }
}

fun LambdaIRExpression.toEvaluable() = if (closures.isEmpty())
    object : Evaluable {
        override fun eval(memory: Memory) = object : StackSafeMFunction {
            @JvmField val evaluableValue = value.toEvaluable()

            @Suppress("NOTHING_TO_INLINE")
            private inline fun i(arg: Any): Trampoline {
                memory.stack.push(arg)
                val ret = evaluableValue.evalT(memory)
                memory.stack.pop()
                return ret
            }

            override fun invoke(arg: Any) = i(arg).it
            override fun invokeStackSafe(arg: Any) = i(arg)
        }
    }.optimizePure()
else
    object : Evaluable {
        @JvmField val accessibleClosures = closures.map { it.toAccessible() }

        override fun eval(memory: Memory) = object : StackSafeMFunction {
            @JvmField val evaluableValue = value.toEvaluable()
            @JvmField val closures = accessibleClosures.mapToArray { it.get(memory) }

            @Suppress("NOTHING_TO_INLINE")
            private inline fun i(arg: Any): Trampoline {
                for (it in closures) {
                    memory.stack.push(it)
                }
                memory.stack.push(arg)
                val ret = evaluableValue.evalT(memory)
                memory.stack.pop(closures.size)
                memory.stack.pop()
                return ret
            }

            override fun invoke(arg: Any) = i(arg).it
            override fun invokeStackSafe(arg: Any) = i(arg)
        }
    }

fun IfIRExpression.toEvaluable() = object : Evaluable {
    @JvmField val evaluableCondition = condition.toEvaluable()
    @JvmField val evaluableIfTrue = ifTrue.toEvaluable()
    @JvmField val evaluableIfFalse = ifFalse.toEvaluable()

    override fun eval(memory: Memory) = if (evaluableCondition.eval(memory) as Boolean)
        evaluableIfTrue.eval(memory)
    else
        evaluableIfFalse.eval(memory)

    override fun evalT(memory: Memory) = if (evaluableCondition.eval(memory) as Boolean)
        evaluableIfTrue.evalT(memory)
    else
        evaluableIfFalse.evalT(memory)
}

fun DoIRExpression.toEvaluable() = object : Evaluable {
    @JvmField val evaluableExpressions = expressions.map { it.toEvaluable() }
    @JvmField val evaluableValue = value.toEvaluable()

    override fun eval(memory: Memory): Any {
        for (it in evaluableExpressions) { it.eval(memory) }
        return evaluableValue.eval(memory)
    }

    override fun evalT(memory: Memory): Trampoline {
        for (it in evaluableExpressions) { it.eval(memory) }
        return evaluableValue.evalT(memory)
    }
}

private class EvaluableUnquoteExpression(val evaluable: Evaluable)
private class EvaluableUnquoteSplicingExpression(val evaluable: Evaluable)

fun QuasiquoteIRExpression.toEvaluable() = object : Evaluable {
    @JvmField val evaluableIRExpressions = elements.map {
        when (it) {
            is UnquoteIRExpression -> EvaluableUnquoteExpression(it.irExpression.toEvaluable())
            is UnquoteSplicingIRExpression -> EvaluableUnquoteSplicingExpression(it.irExpression.toEvaluable())
            else -> it
        }
    }

    override fun eval(memory: Memory): Any {
        var cons: ConsList<*> = Nil
        evaluableIRExpressions.forEach {
            when (it) {
                is EvaluableUnquoteExpression -> {
                    cons = ConsCell(it.evaluable.eval(memory), cons)
                }
                is EvaluableUnquoteSplicingExpression -> {
                    it.evaluable.eval(memory).let {
                        if (it is ConsList<*>) {
                            for (e in it.reversed()) {
                                cons = ConsCell(e, cons)
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
}

private inline fun <reified T> binaryOperator(
        ea1: Evaluable,
        ea2: Evaluable,
        crossinline lambda: (T, T) -> Any
) = Evaluable { lambda(ea1.eval(it) as T, ea2.eval(it) as T) }

fun BinaryOperatorIRExpression.toEvaluable(): Evaluable {
    val ea1 = a1.toEvaluable()
    val ea2 = a2.toEvaluable()
    return when (operator) {
        "|" -> binaryOperator(ea1, ea2, Boolean::or)
        "&" -> binaryOperator(ea1, ea2, Boolean::and)
        "=" -> binaryOperator(ea1, ea2, Any::equals)
        "+i" -> binaryOperator<Int>(ea1, ea2, Int::plus)
        "-i" -> binaryOperator<Int>(ea1, ea2, Int::minus)
        "*i" -> binaryOperator<Int>(ea1, ea2, Int::times)
        "/i" -> binaryOperator<Int>(ea1, ea2, Int::div)
        "%i" -> binaryOperator<Int>(ea1, ea2, Int::rem)
        "+l" -> binaryOperator<Long>(ea1, ea2, Long::plus)
        "-l" -> binaryOperator<Long>(ea1, ea2, Long::minus)
        "*l" -> binaryOperator<Long>(ea1, ea2, Long::times)
        "/l" -> binaryOperator<Long>(ea1, ea2, Long::div)
        "%l" -> binaryOperator<Long>(ea1, ea2, Long::rem)
        "+f" -> binaryOperator<Float>(ea1, ea2, Float::plus)
        "-f" -> binaryOperator<Float>(ea1, ea2, Float::minus)
        "*f" -> binaryOperator<Float>(ea1, ea2, Float::times)
        "/f" -> binaryOperator<Float>(ea1, ea2, Float::div)
        "%f" -> binaryOperator<Float>(ea1, ea2, Float::rem)
        "+d" -> binaryOperator<Double>(ea1, ea2, Double::plus)
        "-d" -> binaryOperator<Double>(ea1, ea2, Double::minus)
        "*d" -> binaryOperator<Double>(ea1, ea2, Double::times)
        "/d" -> binaryOperator<Double>(ea1, ea2, Double::div)
        "%d" -> binaryOperator<Double>(ea1, ea2, Double::rem)
        "<i" -> binaryOperator<Int>(ea1, ea2) { a, b -> a < b }
        ">i" -> binaryOperator<Int>(ea1, ea2) { a, b -> a > b }
        "<l" -> binaryOperator<Long>(ea1, ea2) { a, b -> a < b }
        ">l" -> binaryOperator<Long>(ea1, ea2) { a, b -> a > b }
        "<f" -> binaryOperator<Float>(ea1, ea2) { a, b -> a < b }
        ">f" -> binaryOperator<Float>(ea1, ea2) { a, b -> a > b }
        "<d" -> binaryOperator<Double>(ea1, ea2) { a, b -> a < b }
        ">d" -> binaryOperator<Double>(ea1, ea2) { a, b -> a > b }
        else -> throw Exception("Unrecognized operator $operator")
    }
}
