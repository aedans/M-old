package m

/**
 * Created by Aedan Smith.
 */

typealias Expression = Any
typealias SExpression = ConsCell

fun Iterator<Token>.parse() = lookaheadIterator().parse()
fun LookaheadIterator<Token>.parse() = collect { nextExpression() }
fun LookaheadIterator<Token>.nextExpression(): Expression = null ?:
        parseIdentifier(this) ?:
        parseStringLiteral(this) ?:
        parseNumberLiteral(this) ?:
        parseApostrophe(this) ?:
        parseBacktick(this) ?:
        parseComma(this) ?:
        parseTilde(this) ?:
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

fun parseReaderMacro(tokens: LookaheadIterator<Token>, token: Token, lambda: (Expression) -> Expression) = tokens[0]
        .takeIf { it === token }
        ?.also { tokens.drop(1) }
        ?.let { tokens.nextExpression() }
        ?.let(lambda)

data class QuoteExpression(val cons: Any) {
    override fun toString() = "'$cons"
}

fun parseApostrophe(tokens: LookaheadIterator<Token>) = parseReaderMacro(tokens, ApostropheToken) {
    QuoteExpression(it.toConsTree())
}

data class QuasiquoteExpression(val cons: Any) {
    override fun toString() = "`$cons"
}

fun parseBacktick(tokens: LookaheadIterator<Token>) = parseReaderMacro(tokens, BacktickToken) {
    QuasiquoteExpression(it.toConsTree())
}

data class UnquoteExpression(val cons: Any) {
    override fun toString() = ",$cons"
}

fun parseComma(tokens: LookaheadIterator<Token>) = parseReaderMacro(tokens, CommaToken) {
    UnquoteExpression(it.toConsTree())
}

data class UnquoteSplicingExpression(val cons: Any) {
    override fun toString() = "~$cons"
}

fun parseTilde(tokens: LookaheadIterator<Token>) = parseReaderMacro(tokens, TildeToken) {
    UnquoteSplicingExpression(it.toConsTree())
}

typealias CParenExpression = CParenToken
fun parseSExpression(tokens: LookaheadIterator<Token>) = when (tokens[0]) {
    OParenToken -> tokens
            .also { tokens.drop(1) }
            .parse()
            .asSequence()
            .takeWhile { it !== CParenExpression }
            .iterator()
            .toConsTree()
    CParenToken -> CParenExpression
            .also { tokens.drop(1) }
    else -> null
}
