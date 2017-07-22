package m

/**
 * Created by Aedan Smith.
 */

typealias IRGenerator = (Environment) -> (Expression) -> IRExpression?
interface IRExpression {
    fun eval(environment: Environment): Any
}

object UnitIRExpression : LiteralIRExpression(Unit)
open class LiteralIRExpression(val literal: Any) : IRExpression {
    override fun eval(environment: Environment) = literal
}

val IR_GENERATOR_INDEX by GlobalMemoryRegistry
@Suppress("UNCHECKED_CAST")
fun Environment.getIRGenerators() = this.getHeapValue(m.IR_GENERATOR_INDEX) as List<IRGenerator>

fun LookaheadIterator<Expression>.generateIR(
        environment: Environment
): Iterator<IRExpression> = collect { it[0].toIRExpression(environment).also { drop(1) } }

@JvmOverloads
fun Expression.toIRExpression(
        environment: Environment,
        irGenerators: List<IRGenerator> = environment.getIRGenerators()
) = irGenerators.firstNonNull { it(environment)(this) }
        ?: throw Exception("Unexpected expression ${this} (${this::class})")

inline fun <reified T : Expression> literalIRGenerator(): IRGenerator = mFunction { _, expression ->
    expression.takeIfInstance<T>()?.let { LiteralIRExpression(it) }
}

val stringLiteralIRGenerator: IRGenerator = literalIRGenerator<StringLiteralExpression>()
val numberLiteralIRGenerator: IRGenerator = literalIRGenerator<NumberLiteralExpression>()

data class IdentifierIRExpression(val name: String, val memoryLocation: MemoryLocation) : IRExpression {
    override fun eval(environment: Environment) = evaluate(environment)
    override fun toString() = "$name : $memoryLocation"
}

data class GlobalIdentifierIRExpression(val name: String, val memoryLocation: MemoryLocation.HeapPointer) : IRExpression {
    var value: Any? = null
    override fun eval(environment: Environment): Any {
        val value = value
        return if (value != null) value else {
            this.value = memoryLocation(environment)
            this.value!!
        }
    }

    override fun toString() = "GLOBAL $name : $memoryLocation"
}

val identifierIRGenerator: IRGenerator = mFunction { env, expression ->
    expression.takeIfInstance<IdentifierExpression>()?.let {
        val location = env.getLocation(it.name) ?: throw Exception("Could not find symbol ${it.name}")
        when (location) {
            is MemoryLocation.HeapPointer -> GlobalIdentifierIRExpression(it.name, location)
            else -> IdentifierIRExpression(it.name, location)
        }
    }
}

inline fun <reified T> uniqueSExpression(
        crossinline func: (Environment, SExpression) -> IRExpression
): IRGenerator = mFunction { env, expression ->
    expression.takeIfInstance<SExpression>()?.takeIf { it.isNotEmpty() && it[0] is T }?.let {
        func(env, it)
    }
}

data class DefIRExpression(val name: String, val expression: IRExpression) : IRExpression {
    override fun eval(environment: Environment) = evaluate(environment)
    override fun toString() = "(def $name $expression)"
}

val defIRGenerator: IRGenerator = uniqueSExpression<DefExpression> { env, expression ->
    val name = (expression[1] as IdentifierExpression).name
    env.setLocation(name, MemoryLocation.HeapPointer(env.malloc()))
    @Suppress("NAME_SHADOWING")
    val expression = (expression[2] as Expression).toIRExpression(env)
    DefIRExpression(name, expression)
}

data class LambdaIRExpression(
        val closures: List<MemoryLocation>,
        val expressions: List<IRExpression>,
        val value: IRExpression
) : IRExpression {
    override fun eval(environment: Environment) = evaluate(environment)
    override fun toString() = "(lambda $closures " +
            expressions.joinToString(separator = " ") + (if (expressions.isEmpty()) "" else " ") +
            "$value)"
}

class ClosedEnvironment(val environment: Environment) : Environment, Memory by environment {
    val vars = mutableMapOf<String, MemoryLocation.StackPointer>()
    val closures = mutableListOf<Pair<String, MemoryLocation>>()

    override fun getLocation(name: String): MemoryLocation? {
        return vars[name] ?: environment.getLocation(name)?.let {
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
}

val lambdaIRGenerator: IRGenerator = uniqueSExpression<LambdaExpression> { environment, sExpression ->
    val argNames = sExpression[1] as SExpression
    val argName = (argNames[0] as IdentifierExpression).name
    val env = ClosedEnvironment(environment)
    env.setLocation(argName, MemoryLocation.StackPointer(0))
    val expressions = when (argNames.size) {
        1 -> sExpression.drop(2).map { it!!.toIRExpression(env) }
        else -> listOf((listOf(LambdaExpression, argNames.drop(1)) + sExpression.drop(2)).toIRExpression(env))
    }
    LambdaIRExpression(env.closures.map { it.second }.reversed(), expressions.dropLast(1), expressions.last())
}

data class IfIRExpression(
        val condition: IRExpression,
        val ifTrue: IRExpression,
        val ifFalse: IRExpression
) : IRExpression {
    override fun eval(environment: Environment) = evaluate(environment)
    override fun toString() = "(if $condition $ifTrue $ifFalse)"
}

val ifIRGenerator: IRGenerator = uniqueSExpression<IfExpression> { env, sExpression ->
    val condition = (sExpression[1] as Expression).toIRExpression(env)
    val ifTrue = (sExpression[2] as Expression).toIRExpression(env)
    val ifFalse = if (sExpression.size > 3) (sExpression[3] as Expression).toIRExpression(env) else UnitIRExpression
    IfIRExpression(condition, ifTrue, ifFalse)
}

object TrueIRExpression : LiteralIRExpression(true)
val trueIRGenerator: IRGenerator = mFunction { _, expression ->
    expression.takeIfInstance<TrueExpression>()?.let { TrueIRExpression }
}

object FalseIRExpression : LiteralIRExpression(false)
val falseIRGenerator: IRGenerator = mFunction { _, expression ->
    expression.takeIfInstance<FalseExpression>()?.let { FalseIRExpression }
}

data class InvokeIRExpression(@JvmField val expression: IRExpression, @JvmField val arg: IRExpression) : IRExpression {
    override fun eval(environment: Environment) = evaluate(environment)
    override fun toString() = "($expression $arg)"
}

val sExpressionIRGenerator: IRGenerator = mFunction { env, expression ->
    expression.takeIfInstance<SExpression>()?.let {
        @Suppress("NAME_SHADOWING")
        val expression = when (it.size) {
            2 -> it[0]?.toIRExpression(env)
            else -> it.subList(0, it.size - 1).toIRExpression(env)
        } as IRExpression
        val arg = (it.last() as Expression).toIRExpression(env)
        InvokeIRExpression(expression, arg)
    }
}
