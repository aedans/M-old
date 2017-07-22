package m;

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Aedan Smith.
 */

@SuppressWarnings("unchecked")
public final class Intrinsics {
    @NotNull
    public static Function1<Object, Object> toFunction(Object object) {
        return (Function1<Object, Object>) object;
    }
}
