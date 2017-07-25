package m

/**
 * Created by Aedan Smith.
 */

fun IdentifierIRExpression.evaluate(memory: Memory) = memoryLocation.get(memory)

fun DefIRExpression.evaluate(memory: Memory) {
    location.set(memory, expression.eval(memory))
}

fun LambdaIRExpression.evaluate(memory: Memory) = lambda(
        memory,
        closures.mapToArray { it.get(memory) },
        value,
        expressions
)

private inline fun <T> List<T>.mapToArray(func: (T) -> Any): Array<Any> {
    var index = 0
    val array = Array(size) { func(this[index++]) }
    return array
}

fun lambda(memory: Memory, closures: Array<Any>, value: IRExpression, expressions: List<IRExpression>) = { arg: Any ->
    closures.forEach { memory.push(it) }
    memory.push(arg)
    expressions.forEach { it.eval(memory) }
    val rValue = value.eval(memory)
    memory.pop()
    closures.forEach { memory.pop() }
    rValue
}

fun IfIRExpression.evaluate(memory: Memory) = if (condition.eval(memory) as Boolean)
    ifTrue.eval(memory)
else
    ifFalse.eval(memory)
