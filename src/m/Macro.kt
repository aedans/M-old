package m

/**
 * Created by Aedan Smith.
 */

class Macro(function: (Any) -> Any) : (Expression) -> Expression by function {
    override fun toString() = "m.Macro"
}

fun Iterator<Expression>.expandMacros(env: RuntimeEnvironment) = lookaheadIterator().expandMacros(env)
fun LookaheadIterator<Expression>.expandMacros(env: RuntimeEnvironment) = collect { next().expand(env) }
fun Expression.expand(env: RuntimeEnvironment): Expression = takeIfInstance<SExpression>()
        ?.map { it.expand(env) }
        ?.iterator()
        ?.toConsTree()
        ?.let { expr ->
            expr as SExpression
            expr[0].takeIfInstance<IdentifierExpression>()
                    ?.let { env.getVar(it.name) }
                    ?.takeIfInstance<Macro>()
                    ?.let { it(expr.cdr) }
                    ?: expr
        }
        ?: this
