package com.aedans.m;

import kotlin.jvm.functions.Function1;

/**
 * Created by Aedan Smith.
 */

@SuppressWarnings("unchecked")
public final class Intrinsics {
    public static Object evaluateCall(Object func, Object arg) {
        return ((Function1) func).invoke(arg);
    }
}
