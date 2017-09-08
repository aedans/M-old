package m;

import kotlin.jvm.functions.Function1;

import java.util.ArrayList;

/**
 * Created by Aedan Smith.
 */

@SuppressWarnings("unchecked")
public final class Intrinsics {
    public static Object evaluateInvoke(InvokeIRExpression expression, Memory memory) {
        return ((Function1) expression.expression.eval(memory)).invoke(expression.arg.eval(memory));
    }

    public static Object evaluateIdentifier(IdentifierIRExpression expression, Memory memory) {
        return expression.memoryLocation.get(memory);
    }
}
