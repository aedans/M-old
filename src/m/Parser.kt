package m

/**
 * Created by Aedan Smith.
 */

typealias Expression = Any
typealias SExpression = List<Any>
typealias Parser = (Environment) -> (LookaheadIterator<Token>) -> Expression?

val PARSER_INDEX by GlobalMemoryRegistry
@Suppress("UNCHECKED_CAST")
fun Environment.getParsers() = getHeapValue(PARSER_INDEX) as List<Parser>

fun LookaheadIterator<Token>.parse(environment: Environment) = collect { nextExpression(environment) }

fun LookaheadIterator<Token>.nextExpression(environment: Environment) = environment
        .getParsers()
        .firstNonNull { it(environment)(this) }
        ?: throw Exception("Unexpected token ${this[0]}")

class IdentifierExpression(val name: String) {
    override fun toString() = "$$name"
}

val identifierParser: Parser = mFunction { _, tokens ->
    (tokens[0] as? IdentifierToken)?.let {
        IdentifierExpression(tokens[0].text.toString().also { tokens.drop(1) })
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

val apostropheParser: Parser = mFunction { env, tokens ->
    tokens[0].takeIf { it === ApostropheToken }?.let {
        tokens.drop(1)
        @Suppress("UNCHECKED_CAST")
        val value = tokens.nextExpression(env) as SExpression
        listOf(IdentifierExpression("quote"), value)
    }
}

typealias CParenExpression = CParenToken
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
