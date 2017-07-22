package m

/**
 * Created by Aedan Smith.
 */

fun Iterator<IRExpression>.evaluate(environment: Environment) = forEach { it.evaluate(environment) }
fun IRExpression.evaluate(environment: Environment): Any {
    var value = this
    while (true) {
        value = when (value) {
            is IdentifierIRExpression -> value.evaluate(environment)
            is InvokeIRExpression -> value.evaluate(environment)
            is IfIRExpression -> value.evaluate(environment)
            is LambdaIRExpression -> value.evaluate(environment)
            is DefIRExpression -> value.evaluate(environment)
            else -> return value
        }
    }
}

fun IdentifierIRExpression.evaluate(environment: Environment) = memoryLocation(environment)

fun DefIRExpression.evaluate(environment: Environment): Int {
    val index = (environment.getLocation(name) as MemoryLocation.HeapPointer).index
    environment.setHeapValue(index, expression)
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
    expressions.forEach { it.evaluate(env) }
    val rValue = value.evaluate(env)
    env.pop()
    closures.forEach { env.pop() }
    rValue
}

fun IfIRExpression.evaluate(env: Environment) = if (condition.evaluate(env) as Boolean)
    ifTrue.evaluate(env)
else
    ifFalse.evaluate(env)

@Suppress("UNCHECKED_CAST")
fun InvokeIRExpression.evaluate(env: Environment) = (expression.evaluate(env) as (Any) -> Any)(arg.evaluate(env))
