package m;

import kotlin.jvm.functions.Function1;

/**
 * Created by Aedan Smith.
 */

@SuppressWarnings("unchecked")
public final class Intrinsics {
    public static Object evaluate(InvokeIRExpression expression, Environment env) {
        return (((Function1<Object, Object>) expression.expression.eval(env))).invoke(expression.arg.eval(env));
    }
}
