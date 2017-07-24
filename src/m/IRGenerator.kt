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
    override fun toString() = "$literal"
}

val IR_GENERATOR_INDEX by GlobalMemoryRegistry
@Suppress("UNCHECKED_CAST")
fun Environment.getIRGenerators() = this.getHeapValue(m.IR_GENERATOR_INDEX) as List<IRGenerator>

fun LookaheadIterator<Expression>.generateIR(environment: Environment): Iterator<IRExpression> = collect {
    it[0].toIRExpression(environment).also { drop(1) }
}

fun Expression.toIRExpression(environment: Environment) = environment
        .getIRGenerators()
        .firstNonNull { it(environment)(this) }
        ?: throw Exception("Unexpected expression ${this}")

private inline fun <reified T : Expression> literalIRGenerator(): IRGenerator = mFunction { _, expression ->
    expression.takeIfInstance<T>()?.let { LiteralIRExpression(it) }
}

private inline fun uniqueSExpressionIRGenerator(
        name: String,
        crossinline func: (Environment, SExpression) -> IRExpression
): IRGenerator = mFunction { env, expression ->
    expression
            .takeIfInstance<SExpression>()
            ?.takeIf { it[0].let { it is IdentifierExpression && it.name == name } }
            ?.let {
                func(env, it.drop(1))
            }
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

data class DefIRExpression(val name: String, val expression: IRExpression) : IRExpression {
    override fun eval(environment: Environment) = evaluate(environment)
    override fun toString() = "(def $name $expression)"
}

val defIRGenerator: IRGenerator = uniqueSExpressionIRGenerator("def") { env, sExpression ->
    val name = (sExpression[0] as IdentifierExpression).name
    val expression = sExpression[1]
    env.setLocation(name, MemoryLocation.HeapPointer(env.malloc()))
    DefIRExpression(name, expression.toIRExpression(env))
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

class ClosedEnvironment(val environment: Environment) : Environment, DynamicMemory by environment {
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

val lambdaIRGenerator: IRGenerator = uniqueSExpressionIRGenerator("lambda") { environment, sExpression ->
    @Suppress("UNCHECKED_CAST")
    val argNames = (sExpression[0] as SExpression).map { (it as IdentifierExpression).name }
    val expressions = sExpression.drop(1)
    generateLambdaIRExpression(environment, argNames, expressions)
}

private fun generateLambdaIRExpression(
        environment: Environment,
        argNames: List<String>,
        expressions: List<Expression>
): LambdaIRExpression {
    val argName = argNames[0]
    val env = ClosedEnvironment(environment)
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
    override fun eval(environment: Environment) = evaluate(environment)
    override fun toString() = "(if $condition $ifTrue $ifFalse)"
}

val ifIRGenerator: IRGenerator = uniqueSExpressionIRGenerator("if") { env, sExpression ->
    val condition = sExpression[0]
    val ifTrue = sExpression[1]
    val ifFalse = if (sExpression.size > 2) sExpression[2] else null
    IfIRExpression(
            condition.toIRExpression(env),
            ifTrue.toIRExpression(env),
            ifFalse?.toIRExpression(env) ?: UnitIRExpression
    )
}

val quoteIRGenerator: IRGenerator = uniqueSExpressionIRGenerator("quote") { _, sExpression ->
    @Suppress("UNCHECKED_CAST")
    LiteralIRExpression((sExpression[0] as SExpression).toConsTree())
}

data class InvokeIRExpression(@JvmField val expression: IRExpression, @JvmField val arg: IRExpression) : IRExpression {
    @Suppress("HasPlatformType")
    override fun eval(environment: Environment) = Intrinsics.evaluate(this, environment)
    override fun toString() = "($expression $arg)"
}

val invokeIRGenerator: IRGenerator = mFunction { env, expression ->
    expression.takeIfInstance<SExpression>()?.let {
        @Suppress("NAME_SHADOWING")
        val expression = when (it.size) {
            2 -> it[0].toIRExpression(env)
            else -> it.subList(0, it.size - 1).toIRExpression(env)
        }
        val arg = it.last().toIRExpression(env)
        InvokeIRExpression(expression, arg)
    }
}
