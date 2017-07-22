package m

/**
 * Created by Aedan Smith.
 */

typealias IRExpression = Any
typealias IRGenerator = (Environment) -> (Expression) -> IRExpression?

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

inline fun <reified T : Expression> expressionIRGenerator(): IRGenerator = mFunction { _, expression ->
    expression.takeIfInstance<T>()
}

val stringLiteralIRGenerator: IRGenerator = expressionIRGenerator<StringLiteralExpression>()
val numberLiteralIRGenerator: IRGenerator = expressionIRGenerator<NumberLiteralExpression>()

data class IdentifierIRExpression(val name: String, val memoryLocation: MemoryLocation) {
    override fun toString() = "$name : $memoryLocation"
}

val identifierIRGenerator: IRGenerator = mFunction { env, expression ->
    expression.takeIfInstance<IdentifierExpression>()?.let {
        val location = env.getLocation(it.name) ?: throw Exception("Could not find symbol ${it.name}")
        IdentifierIRExpression(it.name, location)
    }
}

inline fun <reified T> uniqueSExpression(
        crossinline func: (Environment, SExpression) -> IRExpression
): IRGenerator = mFunction { env, expression ->
    expression.takeIfInstance<SExpression>()?.takeIf { it.isNotEmpty() && it[0] is T }?.let {
        func(env, it)
    }
}

data class DefIRExpression(val name: String, val expression: IRExpression) {
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
) {
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

data class IfIRExpression(val condition: IRExpression, val ifTrue: IRExpression, val ifFalse: IRExpression) {
    override fun toString() = "(if $condition $ifTrue $ifFalse)"
}

val ifIRGenerator: IRGenerator = uniqueSExpression<IfExpression> { env, sExpression ->
    val condition = (sExpression[1] as Expression).toIRExpression(env)
    val ifTrue = (sExpression[2] as Expression).toIRExpression(env)
    val ifFalse = if (sExpression.size > 3) (sExpression[3] as Expression).toIRExpression(env) else Unit
    IfIRExpression(condition, ifTrue, ifFalse)
}

val trueIRGenerator: IRGenerator = mFunction { _, expression ->
    expression.takeIfInstance<TrueExpression>()?.let { true }
}

val falseIRGenerator: IRGenerator = mFunction { _, expression ->
    expression.takeIfInstance<FalseExpression>()?.let { false }
}

data class InvokeIRExpression(val expression: Expression, val arg: Expression) {
    override fun toString() = "($expression $arg)"
}

val sExpressionIRGenerator: IRGenerator = mFunction { env, expression ->
    expression.takeIfInstance<SExpression>()?.let {
        @Suppress("NAME_SHADOWING")
        val expression = when (it.size) {
            2 -> it[0]?.toIRExpression(env)
            else -> it.subList(0, it.size - 1).toIRExpression(env)
        } as Expression
        val arg = (it.last() as Expression).toIRExpression(env)
        InvokeIRExpression(expression, arg)
    }
}
