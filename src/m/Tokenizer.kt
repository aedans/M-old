package m

/**
 * Created by Aedan Smith.
 */

open class Token(val text: CharSequence) {
    override fun toString() = "${this::class.simpleName}(\"$text\")"
    override fun hashCode() = text.hashCode()
    override fun equals(other: Any?) = other is Token && other.text == text
}

object WhitespaceOrCommentToken : Token(" ")

private fun Iterator<Token>.noWhitespaceOrComments() = asSequence()
        .filter { it !== WhitespaceOrCommentToken }
        .iterator()

fun Iterator<Char>.tokenize() = lookaheadIterator().tokenize()
fun LookaheadIterator<Char>.tokenize() = collect { nextToken() }
        .noWhitespaceOrComments()

private fun LookaheadIterator<Char>.nextToken() = null ?:
        tokenizeOParen(this) ?:
        tokenizeCParen(this) ?:
        tokenizeApostrophe(this) ?:
        tokenizeBacktick(this) ?:
        tokenizeComma(this) ?:
        tokenizeTilde(this) ?:
        tokenizeWhitespace(this) ?:
        tokenizeComment(this) ?:
        tokenizeCharLiteral(this) ?:
        tokenizeStringLiteral(this) ?:
        tokenizeNumberLiteral(this) ?:
        tokenizeIdentifier(this) ?:
        throw Exception("Unexpected character ${this[0]}")

fun tokenizeChar(str: LookaheadIterator<Char>, char: Char, token: Token) = str[0].takeIf { it == char }
        ?.let { token }
        ?.also { str.drop(1) }

object OParenToken : Token("(")
fun tokenizeOParen(str: LookaheadIterator<Char>) = tokenizeChar(str, '(', OParenToken)

object CParenToken : Token(")")
fun tokenizeCParen(str: LookaheadIterator<Char>) = tokenizeChar(str, ')', CParenToken)

object ApostropheToken : Token("'")
fun tokenizeApostrophe(str: LookaheadIterator<Char>) = tokenizeChar(str, '\'', ApostropheToken)

object BacktickToken : Token("`")
fun tokenizeBacktick(str: LookaheadIterator<Char>) = tokenizeChar(str, '`', BacktickToken)

object CommaToken : Token(",")
fun tokenizeComma(str: LookaheadIterator<Char>) = tokenizeChar(str, ',', CommaToken)

object TildeToken : Token("~")
fun tokenizeTilde(str: LookaheadIterator<Char>) = tokenizeChar(str, '~', TildeToken)

fun Char.isWhitespace() = this == ' ' || this == '\t' || this == '\n' || this == '\r'
fun tokenizeWhitespace(str: LookaheadIterator<Char>) = str[0].takeIf(Char::isWhitespace)
        ?.let { WhitespaceOrCommentToken }
        ?.also { str.drop(1) }

fun tokenizeComment(str: LookaheadIterator<Char>): WhitespaceOrCommentToken? = str[0].takeIf { it == ';' }?.let {
    while (!str.next().let { it == '\n' || it == '\r' });
    WhitespaceOrCommentToken
}

class CharLiteralToken(val char: Char) : Token(char.toString())
fun tokenizeCharLiteral(str: LookaheadIterator<Char>) = str[0].takeIf { it == '\\' }?.let {
    str.drop(1)
    CharLiteralToken(str.next())
}

class StringLiteralToken(string: String) : Token(string)
fun tokenizeStringLiteral(str: LookaheadIterator<Char>) = str[0].takeIf { it == '"' }?.let {
    str.drop(1)
    StringLiteralToken(str.tokenizeStringLiteral())
}

private fun LookaheadIterator<Char>.tokenizeStringLiteral(): String {
    val output = mutableListOf<Char>()
    tokenizeStringLiteral(output)
    return String(output.toCharArray())
}

private tailrec fun LookaheadIterator<Char>.tokenizeStringLiteral(output: MutableList<Char>): Unit = when (this[0]) {
    '"' -> drop(1)
    '\\' -> {
        output.add(this[1])
        drop(2)
        tokenizeStringLiteral(output)
    }
    else -> {
        output.add(this[0])
        drop(1)
        tokenizeStringLiteral(output)
    }
}

typealias NumberType = Int
val BYTE_TYPE = 0
val SHORT_TYPE = 1
val INT_TYPE = 2
val LONG_TYPE = 3
val FLOAT_TYPE = 4
val DOUBLE_TYPE = 5
class NumberLiteralToken(string: String, val type: NumberType) : Token(string)

fun tokenizeNumberLiteral(str: LookaheadIterator<Char>): NumberLiteralToken? {
    var number = ""
    while (str[0].let { it in '0'..'9' || it == '.' }) {
        number += str.next()
    }
    return number.takeIf { it.isNotEmpty() }?.let {
        var drop = true
        val type = when (str[0]) {
            'b', 'B' -> BYTE_TYPE
            's', 'S' -> SHORT_TYPE
            'i', 'I' -> INT_TYPE
            'l', 'L' -> LONG_TYPE
            'f', 'F' -> FLOAT_TYPE
            'd', 'D' -> DOUBLE_TYPE
            else -> {
                drop = false
                if (number.contains('.')) FLOAT_TYPE else INT_TYPE
            }
        }
        if (drop) str.drop(1)
        NumberLiteralToken(number, type)
    }
}

private fun isIdentifierHead(it: Char) = it in 'a'..'z' || it in 'A'..'Z' || it in "_:!?+-*/=<>|&"
private fun isIdentifierTail(it: Char) = isIdentifierHead(it) || it in '0'..'9'

class IdentifierToken(name: String) : Token(name)
@Suppress("UNCHECKED_CAST")
fun tokenizeIdentifier(str: LookaheadIterator<Char>) = str[0].takeIf { isIdentifierHead(it) }?.let {
    val string = StringBuilder()
    while (isIdentifierTail(str[0])) {
        string.append(str.next())
    }
    IdentifierToken(string.toString())
}
