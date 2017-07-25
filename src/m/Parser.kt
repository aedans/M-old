package m

/**
 * Created by Aedan Smith.
 */

typealias Expression = Any
typealias SExpression = List<Expression>

fun Iterator<Token>.parse() = lookaheadIterator().parse()
fun LookaheadIterator<Token>.parse() = collect { nextExpression() }

fun LookaheadIterator<Token>.nextExpression() = null ?:
        parseIdentifier(this) ?:
        parseStringLiteral(this) ?:
        parseNumberLiteral(this) ?:
        parseApostrophe(this) ?:
        parseSExpression(this) ?:
        throw Exception("Unexpected token ${this[0]}")

class IdentifierExpression(val name: String) {
    override fun toString() = name
}

fun parseIdentifier(tokens: LookaheadIterator<Token>) = (tokens[0] as? IdentifierToken)?.let {
    IdentifierExpression(tokens[0].text.toString().also { tokens.drop(1) })
}

typealias StringLiteralExpression = String
fun parseStringLiteral(tokens: LookaheadIterator<Token>) = tokens[0].takeIfInstance<StringLiteralToken>()?.let {
    tokens.drop(1)
    it.text
}

typealias NumberLiteralExpression = Number
fun parseNumberLiteral(tokens: LookaheadIterator<Token>) = tokens[0].takeIfInstance<NumberLiteralToken>()?.let {
    tokens.drop(1)
    val number: Number? = when (it.type) {
        BYTE_TYPE -> it.text.toString().toByte()
        SHORT_TYPE -> it.text.toString().toShort()
        INT_TYPE -> it.text.toString().toInt()
        LONG_TYPE -> it.text.toString().toLong()
        FLOAT_TYPE -> it.text.toString().toFloat()
        DOUBLE_TYPE -> it.text.toString().toDouble()
        else -> throw Exception("Unrecognized number type ${it.type}")
    }
    number
}

fun parseApostrophe(tokens: LookaheadIterator<Token>): SExpression? = tokens[0].takeIf { it === ApostropheToken }?.let {
    tokens.drop(1)
    @Suppress("UNCHECKED_CAST")
    val value = tokens.nextExpression() as SExpression
    listOf(IdentifierExpression("quote"), value)
}

typealias CParenExpression = CParenToken
fun parseSExpression(tokens: LookaheadIterator<Token>): Expression? = tokens[0].takeIf { it === OParenToken }?.let {
    tokens
            .also { it.drop(1) }
            .parse()
            .asSequence()
            .takeWhile { it !== CParenExpression }
            .toList()
} ?: CParenToken.takeIf { tokens[0] === it }
        ?.also { tokens.drop(1) }
        ?.let { CParenExpression }
