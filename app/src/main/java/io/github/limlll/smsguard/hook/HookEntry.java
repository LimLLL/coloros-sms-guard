package io.github.limlll.smsguard.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** LSPosed entry point, registered in assets/xposed_init. */
public final class HookEntry implements IXposedHookLoadPackage {

    /** Process that hosts com.android.providers.telephony (shared uid android.uid.phone). */
    private static final String PHONE_PROCESS = "com.android.phone";

    /**
     * LSPosed loads the module once per matching scope entry, each time in a fresh class loader,
     * so a static flag cannot dedupe; system properties are shared by the whole process.
     */
    private static final String INSTALLED_PROPERTY = "io.github.limlll.smsguard.installed";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!PHONE_PROCESS.equals(lpparam.processName)
                || System.getProperties().putIfAbsent(INSTALLED_PROPERTY, Boolean.TRUE) != null) {
            return;
        }
        Reporter.log("loading into " + lpparam.processName + " via " + lpparam.packageName);
        TransportHook.install();
    }
}
