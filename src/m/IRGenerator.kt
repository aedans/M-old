package m

import java.util.HashMap

/**
 * Created by Aedan Smith.
 */

interface SymbolTable {
    fun getLocation(name: String): MemoryLocation?
    fun setLocation(name: String, location: MemoryLocation?)
    fun allocateLocation(name: String): MemoryLocation
}

class IRSymbolTable(val container: SymbolTable? = null) : SymbolTable {
    var allocationIndex = 0
    val vars = HashMap<String, MemoryLocation?>()
    override fun getLocation(name: String) = vars[name] ?: container?.getLocation(name)
    override fun setLocation(name: String, location: MemoryLocation?) = vars.set(name, location)
    override fun allocateLocation(name: String) = MemoryLocation.HeapPointer(allocationIndex++)
}

interface IRExpression {
    fun eval(memory: Memory): Any
}

object UnitIRExpression : LiteralIRExpression(Unit)
open class LiteralIRExpression(val literal: Any) : IRExpression {
    override fun eval(memory: Memory) = literal
    override fun toString() = "$literal"
}

fun Iterator<Expression>.generateIR(symbolTable: SymbolTable) = lookaheadIterator().generateIR(symbolTable)
fun LookaheadIterator<Expression>.generateIR(symbolTable: SymbolTable): Iterator<IRExpression> = collect {
    next().toIRExpression(symbolTable)
}

fun Expression.toIRExpression(symbolTable: SymbolTable): IRExpression = null ?:
        generateNilLiteralIR(this) ?:
        generateStringLiteralIR(this) ?:
        generateNumberLiteralIR(this) ?:
        generateIdentifierIR(symbolTable, this) ?:
        generateDefIR(symbolTable, this) ?:
        generateLambdaIR(symbolTable, this) ?:
        generateIfIR(symbolTable, this) ?:
        generateQuoteIR(this) ?:
        generateQuasiquoteIR(symbolTable, this) ?:
        generateInvokeIR(symbolTable, this) ?:
        throw Exception("Unexpected expression ${this}")

private inline fun <reified T : Expression> generateLiteralIR(
        expression: Expression, irExpression: LiteralIRExpression
) = expression
        .takeIfInstance<T>()
        ?.let { irExpression }

private inline fun <reified T : Expression> generateLiteralIR(expression: Expression) = expression
        .takeIfInstance<T>()
        ?.let { LiteralIRExpression(it) }

private inline fun generateUniqueSExpressionIR(
        symbolTable: SymbolTable,
        expression: Expression,
        name: String,
        crossinline func: (SymbolTable, SExpression) -> IRExpression
) = expression.takeIfInstance<SExpression>()
        ?.takeIf { it[0].let { it is IdentifierExpression && it.name == name } }
        ?.let { func(symbolTable, it.cdr as SExpression) }

object NilLiteralIRExpression : LiteralIRExpression(Nil)
fun generateNilLiteralIR(expression: Expression) = generateLiteralIR<Nil>(expression, NilLiteralIRExpression)
fun generateStringLiteralIR(expression: Expression) = generateLiteralIR<StringLiteralExpression>(expression)
fun generateNumberLiteralIR(expression: Expression) = generateLiteralIR<NumberLiteralExpression>(expression)

data class IdentifierIRExpression(val name: String, @JvmField val memoryLocation: MemoryLocation) : IRExpression {
    @Suppress("HasPlatformType")
    override fun eval(memory: Memory) = evaluate(memory)
    override fun toString() = "$name : $memoryLocation"
}

data class ConstIdentifierIRExpression(val name: String, val memoryLocation: MemoryLocation.HeapPointer) : IRExpression {
    var value: Any? = null
    override fun eval(memory: Memory): Any = value?.let { it } ?: {
        value = memoryLocation.get(memory)
        value!!
    }()

    override fun toString() = "CONST $name : $memoryLocation"
}

fun generateIdentifierIR(symbolTable: SymbolTable, expression: Expression) = expression
        .takeIfInstance<IdentifierExpression>()
        ?.let {
            val location = symbolTable.getLocation(it.name) ?: throw Exception("Could not find symbol ${it.name}")
            when (location) {
                is MemoryLocation.HeapPointer -> ConstIdentifierIRExpression(it.name, location)
                else -> IdentifierIRExpression(it.name, location)
            }
        }

data class DefIRExpression(val name: String, val expression: IRExpression, val location: MemoryLocation) : IRExpression {
    override fun eval(memory: Memory) = evaluate(memory)
    override fun toString() = "(def $name $expression)"
}

fun generateDefIR(
        symbolTable: SymbolTable, expr: Expression
) = generateUniqueSExpressionIR(symbolTable, expr, "def") { _, sExpression ->
    val name = (sExpression[0] as IdentifierExpression).name
    val expression = sExpression[1]
    val location = symbolTable.allocateLocation(name)
    symbolTable.setLocation(name, location)
    DefIRExpression(name, expression.toIRExpression(symbolTable), location)
}

data class LambdaIRExpression(
        val closures: List<MemoryLocation>,
        val expressions: List<IRExpression>,
        val value: IRExpression
) : IRExpression {
    override fun eval(memory: Memory) = evaluate(memory)
    override fun toString() = "(lambda $closures " +
            expressions.joinToString(separator = " ") + (if (expressions.isEmpty()) "" else " ") +
            "$value)"
}

class ClosedSymbolTable(val symbolTable: SymbolTable) : SymbolTable {
    val vars = mutableMapOf<String, MemoryLocation.StackPointer>()
    val closures = mutableListOf<Pair<String, MemoryLocation>>()

    override fun getLocation(name: String): MemoryLocation? {
        return vars[name] ?: symbolTable.getLocation(name)?.let {
            if (it is MemoryLocation.HeapPointer) it else {
                closures.add(name to it)
                val ptr = MemoryLocation.StackPointer(closures.size)
                vars.put(name, ptr)
                ptr
            }
        }
    }

    override fun setLocation(name: String, location: MemoryLocation?) {
        vars.put(name, location as MemoryLocation.StackPointer)
    }

    override fun allocateLocation(name: String) = symbolTable.allocateLocation(name)
}

fun generateLambdaIR(
        symbolTable: SymbolTable, expr: Expression
) = generateUniqueSExpressionIR(symbolTable, expr, "lambda") { environment, sExpression ->
    @Suppress("UNCHECKED_CAST")
    val argNames = (sExpression[0] as ConsCell).map { (it as IdentifierExpression).name }
    val expressions = sExpression.drop(1)
    generateLambdaIRExpression(environment, argNames, expressions)
}

private fun generateLambdaIRExpression(
        symbolTable: SymbolTable,
        argNames: List<String>,
        expressions: List<Expression>
): LambdaIRExpression {
    val argName = argNames[0]
    val env = ClosedSymbolTable(symbolTable)
    env.setLocation(argName, MemoryLocation.StackPointer(0))
    val irExpressions = when (argNames.size) {
        1 -> expressions.map { it.toIRExpression(env) }
        else -> listOf(generateLambdaIRExpression(env, argNames.drop(1), expressions))
    }
    return LambdaIRExpression(
            env.closures.map { it.second }.reversed(),
            irExpressions.dropLast(1),
            irExpressions.last()
    )
}

data class IfIRExpression(
        val condition: IRExpression,
        val ifTrue: IRExpression,
        val ifFalse: IRExpression
) : IRExpression {
    override fun eval(memory: Memory) = evaluate(memory)
    override fun toString() = "(if $condition $ifTrue $ifFalse)"
}

fun generateIfIR(
        memory: SymbolTable, expr: Expression
) = generateUniqueSExpressionIR(memory, expr, "if") { environment, sExpression ->
    val condition = sExpression[0]
    val ifTrue = sExpression[1]
    val ifFalse = if (sExpression.size > 2) sExpression[2] else null
    IfIRExpression(
            condition.toIRExpression(environment),
            ifTrue.toIRExpression(environment),
            ifFalse?.toIRExpression(environment) ?: NilLiteralIRExpression
    )
}

fun generateQuoteIR(expr: Expression) = expr.takeIfInstance<QuoteExpression>()?.let { LiteralIRExpression(it.cons) }

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

fun generateQuasiquoteIR(table: SymbolTable, expr: Expression) = expr.takeIfInstance<QuasiquoteExpression>()?.let {
    when (it.cons) {
        Nil -> NilLiteralIRExpression
        is ConsCell -> QuasiquoteIRExpression(it.cons.map {
            when (it) {
                is UnquoteExpression -> UnquoteIRExpression(it.cons.toIRExpression(table))
                is UnquoteSplicingExpression -> UnquoteSplicingIRExpression(it.cons.toIRExpression(table))
                is ConsCell -> UnquoteIRExpression(QuasiquoteExpression(it).toIRExpression(table))
                else -> it
            }
        }.reversed())
        else -> throw Exception("Cannot quasiquote ${it.cons}")
    }
}

data class InvokeIRExpression(@JvmField val expression: IRExpression, @JvmField val arg: IRExpression) : IRExpression {
    @Suppress("HasPlatformType")
    override fun eval(memory: Memory) = evaluate(memory)
    override fun toString() = "($expression $arg)"
}

fun generateInvokeIR(symbolTable: SymbolTable, sExpression: Expression) = sExpression
        .takeIfInstance<SExpression>()
        ?.let {
            val expression = when (it.size) {
                1, 2 -> it[0].toIRExpression(symbolTable)
                else -> it.take(it.size - 1).iterator().toConsTree().toIRExpression(symbolTable)
            }
            val arg = when (it.size) {
                1 -> NilLiteralIRExpression
                else -> it.last().toIRExpression(symbolTable)
            }
            InvokeIRExpression(expression, arg)
        }
