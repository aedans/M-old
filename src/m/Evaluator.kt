package m

import java.util.*

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
): Any = evaluators.firstNonNull { it(environment)(this) }?.evaluate(environment, evaluators) ?: this

val identifierEvaluator: Evaluator = mFunction { (virtualMemory, _), expression ->
    expression.takeIfInstance<IdentifierIRExpression>()?.memoryLocation?.invoke(virtualMemory)
}

val defEvaluator: Evaluator = mFunction { (virtualMemory, symbolTable), expression ->
    expression.takeIfInstance<DefIRExpression>()?.let {
        val index = (symbolTable[it.name] as MemoryLocation.HeapPointer).index
        virtualMemory[index] = it.expression
        index
    }
}

val lambdaEvaluator: Evaluator = mFunction { env, expression ->
    expression.takeIfInstance<LambdaIRExpression>()?.let { it ->
        val closure = env.virtualMemory.stack.clone() as Vector<*>;
        { arg: Any ->
            closure.forEach { env.virtualMemory.stack.push(it) }
            env.virtualMemory.stack.push(arg)
            it.expressions.forEach { it.evaluate(env) }
            val value = it.value.evaluate(env)
            env.virtualMemory.stack.pop()
            closure.forEach { env.virtualMemory.stack.pop() }
            value
        }
    }
}

val invokeEvaluator: Evaluator = mFunction { env, expression ->
    expression.takeIfInstance<InvokeIRExpression>()?.let {
        @Suppress("UNCHECKED_CAST")
        (it.expression.evaluate(env) as (Any) -> Any)
                .invoke(it.arg.evaluate(env))
    }
}
