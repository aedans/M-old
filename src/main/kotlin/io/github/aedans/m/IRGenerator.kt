package io.github.aedans.m

import io.github.aedans.cons.Cons
import io.github.aedans.cons.Nil
import io.github.aedans.cons.get
import io.github.aedans.cons.size
import java.util.HashMap

/**
 * Created by Aedan Smith.
 */

interface SymbolTable {
    fun getLocation(name: String): MemoryLocation?
    fun setLocation(name: String, location: MemoryLocation?)
    fun allocateNewLocation(name: String): MemoryLocation
    fun allocateLocation(name: String) = getLocation(name) ?: allocateNewLocation(name)
}

class IRSymbolTable(private val container: SymbolTable? = null) : SymbolTable {
    var allocationIndex = 0
    private val vars = HashMap<String, MemoryLocation?>()
    override fun getLocation(name: String) = vars[name] ?: container?.getLocation(name)
    override fun setLocation(name: String, location: MemoryLocation?) = vars.set(name, location)
    override fun allocateNewLocation(name: String) = MemoryLocation.HeapPointer(allocationIndex++)
}

fun Iterator<Expression>.generateIR(symbolTable: SymbolTable) = asSequence()
        .map { it.toIRExpression(symbolTable) }
        .iterator()

fun Expression.toIRExpression(symbolTable: SymbolTable): IRExpression = null ?:
        generateNilLiteralIR(this) ?:
        generateCharLiteralIR(this) ?:
        generateStringLiteralIR(this) ?:
        generateNumberLiteralIR(this) ?:
        generateIdentifierIR(symbolTable, this) ?:
        generateDefIR(symbolTable, this) ?:
        generateLambdaIR(symbolTable, this) ?:
        generateIfIR(symbolTable, this) ?:
        generateDoIR(symbolTable, this) ?:
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
        ?.let { func(symbolTable, it.cdr) }

fun generateNilLiteralIR(expression: Expression) = generateLiteralIR<Nil>(expression, NilLiteralIRExpression)
fun generateCharLiteralIR(expression: Expression) = generateLiteralIR<CharLiteralExpression>(expression)
fun generateStringLiteralIR(expression: Expression) = generateLiteralIR<StringLiteralExpression>(expression)
fun generateNumberLiteralIR(expression: Expression) = generateLiteralIR<NumberLiteralExpression>(expression)

fun generateIdentifierIR(symbolTable: SymbolTable, expression: Expression) = expression
        .takeIfInstance<IdentifierExpression>()
        ?.let {
            val location = symbolTable.getLocation(it.name) ?: throw Exception("Could not find symbol ${it.name}")
            if (location is MemoryLocation.HeapPointer)
                PureIRExpression(IdentifierIRExpression(location))
            else
                IdentifierIRExpression(location)
        }

fun generateDefIR(
        symbolTable: SymbolTable, expr: Expression
) = generateUniqueSExpressionIR(symbolTable, expr, "def") { _, sExpression ->
    val name = (sExpression[0] as IdentifierExpression).name
    val expression = sExpression[1]
    val location = symbolTable.allocateLocation(name)
    symbolTable.setLocation(name, location)
    DefIRExpression(expression.toIRExpression(symbolTable), location)
}

class ClosedSymbolTable(private val symbolTable: SymbolTable) : SymbolTable {
    private val vars = mutableMapOf<String, MemoryLocation.StackPointer>()
    val closures = mutableListOf<Pair<String, MemoryLocation>>()

    override fun getLocation(name: String): MemoryLocation? {
        return vars[name] ?: symbolTable.getLocation(name)?.let {
            if (it is MemoryLocation.StackPointer) {
                closures.add(name to it)
                val ptr = MemoryLocation.StackPointer(closures.size)
                vars.put(name, ptr)
                ptr
            } else it
        }
    }

    override fun setLocation(name: String, location: MemoryLocation?) {
        vars.put(name, location as MemoryLocation.StackPointer)
    }

    override fun allocateNewLocation(name: String) = symbolTable.allocateNewLocation(name)
}

fun generateLambdaIR(
        symbolTable: SymbolTable, expr: Expression
) = generateUniqueSExpressionIR(symbolTable, expr, "lambda") { environment, sExpression ->
    @Suppress("UNCHECKED_CAST")
    val argNames = (sExpression[0] as Cons<Expression>).map { (it as IdentifierExpression).name }
    val expressions = sExpression.drop(1)
    generateLambdaIRExpression(environment, argNames, expressions)
}

private fun generateLambdaIRExpression(
        symbolTable: SymbolTable,
        argNames: List<String>,
        expressions: List<Expression>
): IRExpression {
    val argName = argNames[0]
    val env = ClosedSymbolTable(symbolTable)
    env.setLocation(argName, MemoryLocation.StackPointer(0))
    val irExpressions = when (argNames.size) {
        1 -> expressions.map { it.toIRExpression(env) }
        else -> listOf(generateLambdaIRExpression(env, argNames.drop(1), expressions))
    }
    val value = when (irExpressions.size) {
        1 -> irExpressions.first()
        else -> DoIRExpression(irExpressions.dropLast(1), irExpressions.last())
    }
    return LambdaIRExpression(
            env.closures.map { it.second }.reversed(),
            value
    )
}

fun generateIfIR(
        symbolTable: SymbolTable, expr: Expression
) = generateUniqueSExpressionIR(symbolTable, expr, "if") { environment, sExpression ->
    val condition = sExpression[0]
    val ifTrue = sExpression[1]
    val ifFalse = if (sExpression.size > 2) sExpression[2] else null
    IfIRExpression(
            condition.toIRExpression(environment),
            ifTrue.toIRExpression(environment),
            ifFalse?.toIRExpression(environment) ?: NilLiteralIRExpression
    )
}

fun generateDoIR(
        symbolTable: SymbolTable, expr: Expression
) = generateUniqueSExpressionIR(symbolTable, expr, "do") { _, sExpression ->
    val expressions = sExpression.take(sExpression.size - 1).map { it.toIRExpression(symbolTable) }
    val value = sExpression.last().toIRExpression(symbolTable)
    DoIRExpression(expressions, value)
}

fun generateQuoteIR(expr: Expression) = expr.takeIfInstance<QuoteExpression>()?.let { LiteralIRExpression(it.cons) }

fun generateQuasiquoteIR(table: SymbolTable, expr: Expression) = expr.takeIfInstance<QuasiquoteExpression>()?.let {
    when (it.cons) {
        Nil -> NilLiteralIRExpression
        is Cons<*> -> QuasiquoteIRExpression(it.cons.map {
            when (it) {
                is UnquoteExpression -> UnquoteIRExpression(it.cons.toIRExpression(table))
                is UnquoteSplicingExpression -> UnquoteSplicingIRExpression(it.cons.toIRExpression(table))
                is Cons<*> -> UnquoteIRExpression(QuasiquoteExpression(it).toIRExpression(table))
                else -> it!!
            }
        }.reversed())
        else -> throw Exception("Cannot quasiquote ${it.cons}")
    }
}

fun generateInvokeIR(symbolTable: SymbolTable, sExpression: Expression) = sExpression
        .takeIfInstance<SExpression>()
        ?.let {
            if (it.size == 3 && it[0] is IdentifierExpression && (it[0] as IdentifierExpression).name in listOf(
                    "|", "&", "=",
                    "+i", "-i", "*i", "/i", "%i",
                    "+l", "-l", "*l", "/l", "%l",
                    "+f", "-f", "*f", "/f", "%f",
                    "+d", "-d", "*d", "/d", "%d",
                    "<i", ">i",
                    "<l", ">l",
                    "<f", ">f",
                    "<d", ">d")) {
                BinaryOperatorIRExpression(
                        (it[0] as IdentifierExpression).name,
                        it[1].toIRExpression(symbolTable),
                        it[2].toIRExpression(symbolTable)
                )
            } else {
                val expression = when (it.size) {
                    1, 2 -> it[0].toIRExpression(symbolTable)
                    else -> it.take(it.size - 1).toConsList().toIRExpression(symbolTable)
                }
                val arg = when (it.size) {
                    1 -> NilLiteralIRExpression
                    else -> it.last().toIRExpression(symbolTable)
                }
                if (expression is InvokeIRExpression
                        && expression.expression.isPure
                        && expression.arg.isPure)
                    InvokeIRExpression(PureIRExpression(expression), arg)
                else
                    InvokeIRExpression(expression, arg)
            }
        }
