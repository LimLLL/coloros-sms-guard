package de.robv.android.xposed;

import java.util.Set;

/** Compile-only stub; signatures mirror de.robv.android.xposed:api:82 exactly. */
public final class XposedBridge {
    private XposedBridge() {
    }

    public static Set<?> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        throw new UnsupportedOperationException("stub");
    }

    public static synchronized void log(String text) {
        throw new UnsupportedOperationException("stub");
    }

    public static synchronized void log(Throwable t) {
        throw new UnsupportedOperationException("stub");
    }
}
