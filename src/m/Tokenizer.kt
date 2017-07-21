package m

import java.io.InputStream

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

class CharSequenceLookaheadIterator(var charSequence: CharSequence) : LookaheadIterator<Char> {
    override fun get(i: Int) = charSequence[i]
    override fun hasNext() = charSequence.isNotEmpty()
    override fun iterator() = charSequence.iterator()
    override fun drop(i: Int) {
        charSequence = charSequence.subSequence(i, charSequence.length)
    }
}

fun CharSequence.lookaheadIterator() = CharSequenceLookaheadIterator(this)
fun InputStream.lookaheadIterator() = iterator().lookaheadIterator()
operator fun InputStream.iterator() = object : Iterator<Char> {
    override fun hasNext() = this@iterator.available() != 0
    override fun next() = this@iterator.read().toChar()
}

fun Iterator<Token>.noWhitespaceOrComments() = object : Iterator<Token> {
    var nextNonWhitespaceOrComment = nextNonWhitespaceOrCommentOrNull()
    tailrec fun nextNonWhitespaceOrCommentOrNull(): Token? {
        val next = nextOrNull()
        return if (next !== WhitespaceOrCommentToken) next else nextNonWhitespaceOrCommentOrNull()
    }

    fun nextOrNull() = if (this@noWhitespaceOrComments.hasNext()) this@noWhitespaceOrComments.next() else null
    override fun hasNext() = nextNonWhitespaceOrComment != null
    override fun next(): Token {
        val oldNextNonWhitespaceOrComment = nextNonWhitespaceOrComment
        nextNonWhitespaceOrComment = nextNonWhitespaceOrCommentOrNull()
        return oldNextNonWhitespaceOrComment ?: throw NoSuchElementException()
    }
}

val TOKENIZER_INDEX by GlobalMemoryRegistry
@Suppress("UNCHECKED_CAST")
fun VirtualMemory.getTokenizers() = this[TOKENIZER_INDEX] as List<Tokenizer>
fun Environment.getTokenizers() = virtualMemory.getTokenizers()

fun LookaheadIterator<Char>.tokenize(environment: Environment) = collect { nextToken(environment) }

private fun LookaheadIterator<Char>.nextToken(
        environment: Environment,
        tokenizers: List<Tokenizer> = environment.getTokenizers()
) = tokenizers.firstNonNull { it(environment)(this) } ?:
        throw Exception("Unexpected character ${this[0]} (${this[0].toInt()})")

fun charTokenizer(char: Char, token: Token): Tokenizer = mFunction { _, str ->
    str[0].takeIf { it == char }
            ?.let { token }
            ?.also { str.drop(1) }
}

object OParenToken : Token("(")
val oParenTokenizer = charTokenizer('(', OParenToken)

object CParenToken : Token(")")
val cParenTokenizer = charTokenizer(')', CParenToken)

val whitespaceTokenizer: Tokenizer = mFunction { _, str ->
    str[0].takeIf(Char::isWhitespace)
            ?.let { WhitespaceOrCommentToken }
            ?.also { str.drop(1) }
}

fun keywordTokenizer(string: String, token: Token): Tokenizer = mFunction { _, str ->
    str.takeIf { it startsWith string }
            ?.let { token }
            ?.also { str.drop(string.length) }
}

object DefToken : Token("def")
val defTokenizer = keywordTokenizer("def", DefToken)

object LambdaToken : Token("lambda")
val lambdaTokenizer = keywordTokenizer("lambda", LambdaToken)

object IfToken : Token("if")
val ifTokenizer = keywordTokenizer("if", IfToken)

object TrueToken : Token("#t")
val trueTokenizer = keywordTokenizer("#t", TrueToken)

object FalseToken : Token("#f")
val falseTokenizer = keywordTokenizer("#f", FalseToken)

class IdentifierToken(name: String) : Token(name)
val IDENTIFIER_IS_HEAD_INDEX by GlobalMemoryRegistry
val IDENTIFIER_IS_TAIL_INDEX by GlobalMemoryRegistry
@Suppress("UNCHECKED_CAST")
val identifierTokenizer: Tokenizer = mFunction { (memory, _), str ->
    val isHead = memory[IDENTIFIER_IS_HEAD_INDEX] as (Char) -> Boolean
    str.takeIf { isHead(str[0]) }?.let {
        val isTail = memory[IDENTIFIER_IS_TAIL_INDEX] as (Char) -> Boolean
        val chars = str.takeWhile { isHead(it) || isTail(it) }
        str.drop(chars.size)
        IdentifierToken(String(chars.toCharArray()))
    }
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

private infix fun Iterable<Char>.startsWith(charSequence: CharSequence) = iterator() startsWith charSequence
private infix fun Iterator<Char>.startsWith(charSequence: CharSequence) = charSequence.all { next() == it }
