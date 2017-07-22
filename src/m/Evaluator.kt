package m

/**
 * Created by Aedan Smith.
 */

fun IdentifierIRExpression.evaluate(environment: Environment) = memoryLocation(environment)

fun DefIRExpression.evaluate(environment: Environment): Int {
    val index = (environment.getLocation(name) as MemoryLocation.HeapPointer).index
    environment.setHeapValue(index, expression.eval(environment))
    return index
}

fun LambdaIRExpression.evaluate(environment: Environment) = lambda(
        environment,
        closures.map { it(environment) },
        value,
        expressions
)

private inline fun <T> List<T>.map(func: (T) -> Any): Array<Any> {
    var index = 0
    val array = Array(size) { func(this[index++]) }
    return array
}

fun lambda(env: Environment, closures: Array<Any>, value: IRExpression, expressions: List<IRExpression>) = { arg: Any ->
    closures.forEach { env.push(it) }
    env.push(arg)
    expressions.forEach { it.eval(env) }
    val rValue = value.eval(env)
    env.pop()
    closures.forEach { env.pop() }
    rValue
}

fun IfIRExpression.evaluate(env: Environment) = if (condition.eval(env) as Boolean)
    ifTrue.eval(env)
else
    ifFalse.eval(env)

@Suppress("NOTHING_TO_INLINE")
inline fun InvokeIRExpression.evaluate(env: Environment) = Intrinsics.evaluate(this, env)
