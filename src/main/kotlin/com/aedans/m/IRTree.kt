package com.aedans.m

/**
 * Created by Aedan Smith.
 */

sealed class IRExpression {
    open val isPure: Boolean get() = false
}

class PureIRExpression(val irExpression: IRExpression) : IRExpression() {
    override val isPure get() = true
}

open class LiteralIRExpression(val literal: Any) : IRExpression() {
    override val isPure get() = true
    override fun toString() = "$literal"
}

object NilLiteralIRExpression : LiteralIRExpression(Nil)

data class IdentifierIRExpression(val memoryLocation: MemoryLocation) : IRExpression() {
    @Suppress("HasPlatformType")
    override fun toString() = "$memoryLocation"
}

data class DefIRExpression(
        val expression: IRExpression,
        val memoryLocation: MemoryLocation
) : IRExpression() {
    override fun toString() = "(def $memoryLocation $expression)"
}

data class InvokeIRExpression(
        val expression: IRExpression,
        val arg: IRExpression
) : IRExpression() {
    @Suppress("HasPlatformType")
    override fun toString() = "($expression $arg)"
}

data class LambdaIRExpression(
        val closures: List<MemoryLocation>,
        val value: IRExpression
) : IRExpression() {
    override fun toString() = "(lambda $closures $value)"
}

data class IfIRExpression(
        val condition: IRExpression,
        val ifTrue: IRExpression,
        val ifFalse: IRExpression
) : IRExpression() {
    override fun toString() = "(if $condition $ifTrue $ifFalse)"
}

data class DoIRExpression(
        val expressions: List<IRExpression>,
        val value: IRExpression
) : IRExpression() {
    override fun toString() = "(do " +
            expressions.joinToString(separator = " ") + (if (expressions.isEmpty()) "" else " ") +
            "$value)"
}

data class QuasiquoteIRExpression(val elements: List<Any>) : IRExpression() {
    override fun toString() = "`${elements.reversed()}"
}

data class UnquoteIRExpression(val irExpression: IRExpression) : IRExpression() {
    override fun toString() = ",$irExpression"
}

data class UnquoteSplicingIRExpression(val irExpression: IRExpression) : IRExpression() {
    override fun toString() = "~$irExpression"
}

data class BinaryOperatorIRExpression(
        val operator: String,
        val a1: IRExpression,
        val a2: IRExpression
) : IRExpression() {
    override fun toString() = "(OP $operator $a1 $a2)"
}
