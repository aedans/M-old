package m;

import kotlin.jvm.functions.Function1;

/**
 * Created by Aedan Smith.
 */

@SuppressWarnings("unchecked")
final class Intrinsics {
    static Object evaluate(InvokeIRExpression expression, Memory memory) {
        return (((Function1<Object, Object>) expression.expression.eval(memory))).invoke(expression.arg.eval(memory));
    }
}
