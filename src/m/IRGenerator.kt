package m

/**
 * Created by Aedan Smith.
 */

typealias IRExpression = Any
typealias IRGenerator = (Environment) -> (Expression) -> IRExpression?

val IR_GENERATOR_INDEX by GlobalMemoryRegistry
@Suppress("UNCHECKED_CAST")
fun VirtualMemory.getIRGenerators() = this[IR_GENERATOR_INDEX] as List<IRGenerator>
fun Environment.getIRGenerators() = virtualMemory.getIRGenerators()

fun LookaheadIterator<Expression>.generateIR(
        environment: Environment
): Iterator<IRExpression> = collect { it[0].toIRExpression(environment).also { drop(1) } }

@JvmOverloads
fun Expression.toIRExpression(
        environment: Environment,
        irGenerators: List<IRGenerator> = environment.getIRGenerators()
) = irGenerators.firstNonNull { it(environment)(this) }
        ?: throw Exception("Unexpected expression ${this} (${this::class})")

inline fun <reified T : Expression> expressionIRGenerator(): IRGenerator =
        mFunction { _, expression -> expression.takeIfInstance<T>() }

val stringLiteralIRGenerator: IRGenerator = expressionIRGenerator<StringLiteralExpression>()

data class IdentifierIRExpression(val name: String, val memoryLocation: MemoryLocation) {
    override fun toString() = "name :: $memoryLocation"
}

val identifierIRGenerator: IRGenerator = mFunction { env, expression ->
    expression.takeIfInstance<IdentifierExpression>()?.let {
        val variableType = env.symbolTable[it.name] ?: throw Exception("Could not find symbol ${it.name}")
        IdentifierIRExpression(it.name, variableType)
    }
}

data class DefIRExpression(val name: String, val expression: IRExpression) {
    override fun toString() = "(def $name $expression)"
}

val defIRGenerator: IRGenerator = mFunction { env, expression ->
    expression.takeIfInstance<SExpression>()?.takeIf { it[0] is DefExpression }?.let {
        val name = (it[1] as IdentifierExpression).name
        @Suppress("NAME_SHADOWING")
        val expression = (it[2] as Expression).toIRExpression(env)
        env.symbolTable[name] = MemoryLocation.HeapPointer(env.virtualMemory.malloc())
        DefIRExpression(name, expression)
    }
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
