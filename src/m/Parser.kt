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
fun VirtualMemory.getParsers() = this[PARSER_INDEX] as List<Parser>
fun Environment.getParsers() = virtualMemory.getParsers()

fun LookaheadIterator<Token>.parse(environment: Environment) = collect { nextExpression(environment) }

fun LookaheadIterator<Token>.nextExpression(
        environment: Environment,
        parsers: List<Parser> = environment.getParsers()
) = parsers.firstNonNull { it(environment)(this) } ?: throw Exception("Unexpected token ${this[0]}")

fun atomParser(token: Token, expression: Expression): Parser = mFunction { _, tokens ->
    tokens.takeIf { it[0] === token }?.let {
        it.drop(1)
        expression
    }
}

object DefExpression : TokenExpression(DefToken)
val defParser = atomParser(DefToken, DefExpression)

object LambdaExpression : TokenExpression(LambdaToken)
val lambdaParser = atomParser(LambdaToken, LambdaExpression)

object IfExpression : TokenExpression(IfToken)
val ifParser = atomParser(IfToken, IfExpression)

object TrueExpression : TokenExpression(TrueToken)
val trueParser = atomParser(TrueToken, TrueExpression)

object FalseExpression : TokenExpression(FalseToken)
val falseParser = atomParser(FalseToken, FalseExpression)

typealias StringLiteralExpression = String
val stringLiteralParser: Parser = mFunction { _, tokens ->
    tokens[0].takeIfInstance<StringLiteralToken>()?.let {
        tokens.drop(1)
        it.text
    }
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

val identifierParser: Parser = mFunction { _, tokens ->
    (tokens[0] as? IdentifierToken)?.let {
        IdentifierExpression(tokens[0].text.toString()).also { tokens.drop(1) }
    }
}
