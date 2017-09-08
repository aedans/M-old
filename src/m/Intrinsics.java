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

    public static final class Heap {
        private ArrayList heap = new ArrayList();

        private void expand(int i) {
            while (heap.size() <= i)
                heap.add(Nil.INSTANCE);
        }

        public Object get(int location) {
            return heap.get(location);
        }

        public void set(int location, Object object) {
            expand(location);
            heap.set(location, object);
        }
    }
}
