package de.robv.android.xposed;

import java.lang.reflect.Member;

/** Compile-only stub; signatures mirror de.robv.android.xposed:api:82 exactly. */
public abstract class XC_MethodHook {
    public XC_MethodHook() {
    }

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    public static final class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;

        private MethodHookParam() {
        }

        public Object getResult() {
            throw new UnsupportedOperationException("stub");
        }

        public void setResult(Object result) {
            throw new UnsupportedOperationException("stub");
        }

        public Throwable getThrowable() {
            throw new UnsupportedOperationException("stub");
        }

        public boolean hasThrowable() {
            throw new UnsupportedOperationException("stub");
        }

        public void setThrowable(Throwable throwable) {
            throw new UnsupportedOperationException("stub");
        }
    }
}
