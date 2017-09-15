package com.aedans.m

/**
 * Created by Aedan Smith.
 */

interface IRExpression {
    fun eval(memory: Memory): Any
    fun evalT(memory: Memory): Trampoline = Trampoline.Just(eval(memory))
}

interface PureIRExpression : IRExpression
data class PureIRExpressionImpl(@JvmField val irExpression: IRExpression) : PureIRExpression {
    var value: Any? = null
    override fun eval(memory: Memory): Any = value?.let { it } ?: run {
        value = irExpression.eval(memory)
        value!!
    }

    override fun toString() = "PURE $irExpression"
}

open class LiteralIRExpression(@JvmField val literal: Any) : PureIRExpression {
    override fun eval(memory: Memory) = literal
    override fun toString() = "$literal"
}

object NilLiteralIRExpression : LiteralIRExpression(Nil)

data class IdentifierIRExpression(@JvmField val memoryLocation: MemoryLocation) : IRExpression {
    @Suppress("HasPlatformType")
    override fun eval(memory: Memory) = evaluate(memory)
    override fun toString() = "$memoryLocation"
}

data class DefIRExpression(
        val expression: IRExpression,
        val location: MemoryLocation
) : IRExpression {
    override fun eval(memory: Memory) = evaluate(memory)
    override fun toString() = "(def $location $expression)"
}

data class LambdaIRExpression(
        @JvmField val closures: List<MemoryLocation>,
        @JvmField val value: IRExpression
) : IRExpression {
    override fun eval(memory: Memory) = evaluate(memory)
    override fun toString() = "(lambda $closures $value)"
}

data class SimpleLambdaIRExpression(val value: IRExpression) : IRExpression {
    override fun eval(memory: Memory) = evaluate(memory)
    override fun toString() = "(lambda $value)"
}

data class IfIRExpression(
        @JvmField val condition: IRExpression,
        @JvmField val ifTrue: IRExpression,
        @JvmField val ifFalse: IRExpression
) : IRExpression {
    override fun eval(memory: Memory) = evaluate(memory)
    override fun evalT(memory: Memory) = evaluateT(memory)
    override fun toString() = "(if $condition $ifTrue $ifFalse)"
}

data class DoIRExpression(
        @JvmField val expressions: List<IRExpression>,
        @JvmField val value: IRExpression
) : IRExpression {
    override fun eval(memory: Memory) = evaluate(memory)
    override fun evalT(memory: Memory) = evaluateT(memory)
    override fun toString() = "(do " +
            expressions.joinToString(separator = " ") + (if (expressions.isEmpty()) "" else " ") +
            "$value)"
}

data class QuasiquoteIRExpression(val irExpressions: List<Any>) : IRExpression {
    override fun eval(memory: Memory) = evaluate(memory)
    override fun toString() = "`${irExpressions.reversed()}"
}

data class UnquoteIRExpression(val irExpression: IRExpression) : IRExpression {
    override fun eval(memory: Memory) = irExpression.eval(memory)
    override fun toString() = ",$irExpression"
}

data class UnquoteSplicingIRExpression(val irExpression: IRExpression) : IRExpression {
    override fun eval(memory: Memory) = irExpression.eval(memory)
    override fun toString() = "~$irExpression"
}

data class InvokeIRExpression(
        @JvmField val expression: IRExpression,
        @JvmField val arg: IRExpression
) : IRExpression {
    @Suppress("HasPlatformType")
    override fun eval(memory: Memory) = evaluate(memory)
    override fun evalT(memory: Memory) = evaluateT(memory)
    override fun toString() = "($expression $arg)"
}
