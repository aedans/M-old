package m

/**
 * Created by Aedan Smith.
 */

typealias Evaluator = (Environment) -> (IRExpression) -> Any?

val EVALUATOR_INDEX by GlobalMemoryRegistry
@Suppress("UNCHECKED_CAST")
fun VirtualMemory.getEvaluators() = this[EVALUATOR_INDEX] as List<Evaluator>
fun Environment.getEvaluators() = virtualMemory.getEvaluators()

fun Iterator<IRExpression>.evaluate(environment: Environment) = forEach { it.evaluate(environment) }
fun IRExpression.evaluate(
        environment: Environment,
        evaluators: List<Evaluator> = environment.getEvaluators()
) = evaluators.firstNonNull { it(environment)(this) } ?: this

val identifierEvaluator: Evaluator = mFunction { (virtualMemory, _), expression ->
    expression.takeIfInstance<IdentifierIRExpression>()?.let {
        when (it.variableType) {
            is VariableType.Value -> it.variableType.value
            is VariableType.HeapPointer -> virtualMemory[it.variableType.index]
        }
    }
}

val defEvaluator: Evaluator = mFunction { (virtualMemory, symbolTable), expression ->
    expression.takeIfInstance<DefIRExpression>()?.let {
        val index = (symbolTable[it.name] as VariableType.HeapPointer).index
        virtualMemory[index] = it.expression
        index
    }
}

val invokeEvaluator: Evaluator = mFunction { env, expression ->
    expression.takeIfInstance<InvokeIRExpression>()?.let {
        @Suppress("UNCHECKED_CAST")
        (it.expression.evaluate(env) as (Any) -> Any)
                .invoke(it.arg.evaluate(env))
    }
}
