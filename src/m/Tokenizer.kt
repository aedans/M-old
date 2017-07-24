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

typealias Tokenizer = (Environment) -> (LookaheadIterator<Char>) -> Token?

private fun Iterator<Token>.noWhitespaceOrComments() = asSequence().filter { it !== WhitespaceOrCommentToken }.iterator()

val TOKENIZER_INDEX by GlobalMemoryRegistry
@Suppress("UNCHECKED_CAST")
fun Environment.getTokenizers() = getHeapValue(TOKENIZER_INDEX) as List<Tokenizer>

fun LookaheadIterator<Char>.tokenize(environment: Environment) = collect { nextToken(environment) }
        .noWhitespaceOrComments()
        .lookaheadIterator()

private fun LookaheadIterator<Char>.nextToken(environment: Environment) = environment
        .getTokenizers()
        .firstNonNull { it(environment)(this) }
        ?: throw Exception("Unexpected character ${this[0]}")

private fun charTokenizer(char: Char, token: Token): Tokenizer = mFunction { _, str ->
    str[0].takeIf { it == char }
            ?.let { token }
            ?.also { str.drop(1) }
}

object OParenToken : Token("(")
val oParenTokenizer = charTokenizer('(', OParenToken)

object CParenToken : Token(")")
val cParenTokenizer = charTokenizer(')', CParenToken)

object ApostropheToken : Token("'")
val apostropheTokenizer = charTokenizer('\'', ApostropheToken)

val whitespaceTokenizer: Tokenizer = mFunction { _, str ->
    str[0].takeIf(Char::isWhitespace)
            ?.let { WhitespaceOrCommentToken }
            ?.also { str.drop(1) }
}

class StringLiteralToken(string: String) : Token(string)
val stringLiteralTokenizer: Tokenizer = mFunction { _, str ->
    str.takeIf { it[0] == '"' }?.let {
        it.drop(1)
        StringLiteralToken(it.tokenizeStringLiteral())
    }
}

private fun LookaheadIterator<Char>.tokenizeStringLiteral(): String = when (this[0]) {
    '"' -> "".also { drop(1) }
    '\\' -> this[1].also { drop(2) } + tokenizeStringLiteral()
    else -> this[0].also { drop(1) } + tokenizeStringLiteral()
}

typealias NumberType = Int
val BYTE_TYPE = 0
val SHORT_TYPE = 1
val INT_TYPE = 2
val LONG_TYPE = 3
val FLOAT_TYPE = 4
val DOUBLE_TYPE = 5
class NumberLiteralToken(string: String, val type: NumberType) : Token(string)

val numberLiteralTokenizer: Tokenizer = mFunction { _, str ->
    val number = String(str.takeWhile { it in '0'..'9' || it == '.' }.toCharArray())
    number.takeIf { it.isNotEmpty() }?.let {
        str.drop(number.length)
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

class IdentifierToken(name: String) : Token(name)
val IDENTIFIER_IS_HEAD_INDEX by GlobalMemoryRegistry
val IDENTIFIER_IS_TAIL_INDEX by GlobalMemoryRegistry
@Suppress("UNCHECKED_CAST")
val identifierTokenizer: Tokenizer = mFunction { env, str ->
    val isHead = env.getHeapValue(IDENTIFIER_IS_HEAD_INDEX) as (Char) -> Boolean
    str.takeIf { isHead(str[0]) }?.let {
        val isTail = env.getHeapValue(IDENTIFIER_IS_TAIL_INDEX) as (Char) -> Boolean
        val chars = str.takeWhile { isHead(it) || isTail(it) }
        str.drop(chars.size)
        IdentifierToken(String(chars.toCharArray()))
    }
}
