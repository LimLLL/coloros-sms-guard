package io.github.limlll.smsguard.hook;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** LSPosed entry point, registered in assets/xposed_init. */
public final class HookEntry implements IXposedHookLoadPackage {

    /** Process that hosts com.android.providers.telephony (shared uid android.uid.phone). */
    private static final String PHONE_PROCESS = "com.android.phone";

    // Both scoped packages load into the same process, but Transport must be hooked only once.
    private static final AtomicBoolean installed = new AtomicBoolean();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!PHONE_PROCESS.equals(lpparam.processName) || !installed.compareAndSet(false, true)) {
            return;
        }
        Reporter.log("loading into " + lpparam.processName + " via " + lpparam.packageName);
        TransportHook.install();
    }
}
