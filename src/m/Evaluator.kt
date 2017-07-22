package m

/**
 * Created by Aedan Smith.
 */

typealias Evaluator = (Environment) -> (IRExpression) -> Any?

val EVALUATOR_INDEX by GlobalMemoryRegistry
@Suppress("UNCHECKED_CAST")
fun Environment.getEvaluators() = getHeapValue(EVALUATOR_INDEX) as List<Evaluator>

fun Iterator<IRExpression>.evaluate(environment: Environment) = forEach { it.evaluate(environment) }
@Suppress("NOTHING_TO_INLINE")
inline fun IRExpression.evaluate(environment: Environment): Any {
    var value = this
    while (true) {
        value = environment.getEvaluators().firstNonNull { it(environment)(value) } ?: return value
    }
}

val identifierEvaluator: Evaluator = mFunction { env, expression ->
    expression.takeIfInstance<IdentifierIRExpression>()?.memoryLocation?.invoke(env)
}

val defEvaluator: Evaluator = mFunction { env, expression ->
    expression.takeIfInstance<DefIRExpression>()?.let {
        val index = (env.getLocation(it.name) as MemoryLocation.HeapPointer).index
        env.setHeapValue(index, it.expression)
        index
    }
}

val lambdaEvaluator: Evaluator = mFunction { env, expression ->
    expression.takeIfInstance<LambdaIRExpression>()?.let {
        lambda(env, it.closures.map { it(env) }, it.value, it.expressions)
    }
}

fun lambda(env: Environment, closures: List<Any>, value: IRExpression, expressions: List<IRExpression>) = { arg: Any ->
    closures.forEach { env.push(it) }
    env.push(arg)
    expressions.forEach { it.evaluate(env) }
    val rValue = value.evaluate(env)
    env.pop()
    closures.forEach { env.pop() }
    rValue
}

val ifEvaluator: Evaluator = mFunction { env, expression ->
    expression.takeIfInstance<IfIRExpression>()?.let {
        if (it.condition.evaluate(env) as Boolean)
            it.ifTrue.evaluate(env)
        else
            it.ifFalse.evaluate(env)
    }
}

val invokeEvaluator: Evaluator = mFunction { env, expression ->
    expression.takeIfInstance<InvokeIRExpression>()?.let {
        @Suppress("UNCHECKED_CAST")
        (it.expression.evaluate(env) as (Any) -> Any)
                .invoke(it.arg.evaluate(env))
    }
}
