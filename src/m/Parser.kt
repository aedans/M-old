package m

/**
 * Created by Aedan Smith.
 */

typealias Expression = Any
typealias SExpression = List<*>
data class IdentifierExpression(val name: String) {
    override fun toString() = name
}

open class TokenExpression(token: Token) {
    val name = token.text.toString().toUpperCase()
    override fun equals(other: Any?) = other is TokenExpression && other.name == name
    override fun hashCode() = name.hashCode()
    override fun toString() = name
}

typealias Parser = (Environment) -> (LookaheadIterator<Token>) -> Expression?

val PARSER_INDEX by GlobalMemoryRegistry
@Suppress("UNCHECKED_CAST")
fun Environment.getParsers() = getHeapValue(PARSER_INDEX) as List<Parser>

fun LookaheadIterator<Token>.parse(environment: Environment) = collect { nextExpression(environment) }

fun LookaheadIterator<Token>.nextExpression(environment: Environment) = environment
        .getParsers()
        .firstNonNull { it(environment)(this) }
        ?: throw Exception("Unexpected token ${this[0]}")

fun atomParser(token: Token, expression: Expression): Parser = mFunction { _, tokens ->
    tokens.takeIf { it[0] === token }?.let {
        it.drop(1)
        expression
    }
}

inline fun uniqueSExpressionParser(
        token: Token,
        crossinline func: (Environment, LookaheadIterator<Token>) -> Expression
): Parser = mFunction { env, tokens ->
    tokens.takeIf { it[0] === OParenToken && it[1] == token }?.let {
        tokens.drop(2)
        val expr = func(env, tokens)
        tokens.takeIf { it[0] === CParenToken } ?: throw Exception("Missing closing parentheses")
        tokens.drop(1)
        expr
    }
}

object TrueExpression : TokenExpression(TrueToken)
val trueParser = atomParser(TrueToken, TrueExpression)

object FalseExpression : TokenExpression(FalseToken)
val falseParser = atomParser(FalseToken, FalseExpression)

val identifierParser: Parser = mFunction { _, tokens ->
    (tokens[0] as? IdentifierToken)?.let {
        IdentifierExpression(tokens[0].text.toString()).also { tokens.drop(1) }
    }
}

typealias StringLiteralExpression = String
val stringLiteralParser: Parser = mFunction { _, tokens ->
    tokens[0].takeIfInstance<StringLiteralToken>()?.let {
        tokens.drop(1)
        it.text
    }
}

typealias NumberLiteralExpression = Number
val numberLiteralParser: Parser = mFunction { _, tokens ->
    tokens[0].takeIfInstance<NumberLiteralToken>()?.let {
        tokens.drop(1)
        when (it.type) {
            BYTE_TYPE -> it.text.toString().toByte()
            SHORT_TYPE -> it.text.toString().toShort()
            INT_TYPE -> it.text.toString().toInt()
            LONG_TYPE -> it.text.toString().toLong()
            FLOAT_TYPE -> it.text.toString().toFloat()
            DOUBLE_TYPE -> it.text.toString().toDouble()
            else -> throw Exception("Unrecognized number type ${it.type}")
        }
    }
}

data class DefExpression(val name: String, val expression: Expression)
val defParser = uniqueSExpressionParser(DefToken) { env, tokens ->
    val name = (tokens.nextExpression(env) as IdentifierExpression).name
    val expression = tokens.nextExpression(env)
    DefExpression(name, expression)
}

data class LambdaExpression(val argNames: List<String>, val expressions: List<Expression>)
val lambdaParser = uniqueSExpressionParser(LambdaToken) { env, tokens ->
    @Suppress("UNCHECKED_CAST")
    val argNames = (tokens.nextExpression(env) as List<IdentifierExpression>).map { it.name }
    val expressions = mutableListOf<Expression>()
    while (tokens[0] != CParenToken) {
        expressions.add(tokens.nextExpression(env))
    }
    LambdaExpression(argNames, expressions)
}

data class IfExpression(val condition: Expression, val ifTrue: Expression, val ifFalse: Expression?)
val ifParser = uniqueSExpressionParser(IfToken) { env, tokens ->
    val condition = tokens.nextExpression(env)
    val ifTrue = tokens.nextExpression(env)
    val ifFalse = if (tokens[0] === CParenToken) null else tokens.nextExpression(env)
    IfExpression(condition, ifTrue, ifFalse)
}

object CParenExpression : TokenExpression(CParenToken)
val sExpressionParser: Parser = mFunction { env, tokens ->
    tokens.takeIf { it[0] === OParenToken }?.let {
        it
                .also { it.drop(1) }
                .parse(env)
                .asSequence()
                .takeWhile { it !== CParenExpression }
                .toList()
    } ?: CParenToken.takeIf { tokens[0] === it }
            ?.also { tokens.drop(1) }
            ?.let { CParenExpression }
}
