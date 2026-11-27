package de.robv.android.xposed.callbacks;

/** Compile-only stub; only the fields this module reads are declared. */
public abstract class XC_LoadPackage {
    private XC_LoadPackage() {
    }

    public static final class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public boolean isFirstApplication;

        private LoadPackageParam() {
        }
    }
}
