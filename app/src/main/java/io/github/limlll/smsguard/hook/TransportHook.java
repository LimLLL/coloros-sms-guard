package io.github.limlll.smsguard.hook;

import android.Manifest;
import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.limlll.smsguard.core.GuardPolicy;
import io.github.limlll.smsguard.core.ReportThrottle;

/**
 * Rejects calls from apps without READ_SMS into the vulnerable OPlus telephony providers.
 *
 * <p>Hooks ContentProvider.Transport (the binder stub every provider call enters through)
 * instead of the provider classes, so inherited methods are covered and the check runs on the
 * binder thread where Binder.getCallingUid() is still the remote caller. Every provider call in
 * the phone process passes through here, so the fast path must stay allocation-free.
 */
final class TransportHook extends XC_MethodHook {

    /** IContentProvider calls that reach provider code; getType returns no SMS data. */
    private static final String[] HOOKED_METHODS = {
            "query", "insert", "bulkInsert", "update", "delete", "applyBatch", "call",
            "openFile", "openAssetFile", "openTypedAssetFile",
            "canonicalize", "uncanonicalize", "refresh"};

    private static final long REPORT_WINDOW_MS = 10_000;

    private final Method getContentProvider;
    private final ReportThrottle throttle = new ReportThrottle(REPORT_WINDOW_MS);
    private final Heartbeat heartbeat = new Heartbeat();
    private final AtomicBoolean resolveErrorLogged = new AtomicBoolean();
    private final AtomicBoolean permissionErrorLogged = new AtomicBoolean();

    private TransportHook(Method getContentProvider) {
        this.getContentProvider = getContentProvider;
    }

    static void install() {
        try {
            Class<?> transport = Class.forName("android.content.ContentProvider$Transport");
            Method getter = transport.getDeclaredMethod("getContentProvider");
            getter.setAccessible(true);
            TransportHook hook = new TransportHook(getter);
            int hooked = 0;
            for (String name : HOOKED_METHODS) {
                hooked += XposedBridge.hookAllMethods(transport, name, hook).size();
            }
            Reporter.log("installed " + hooked + " hooks on ContentProvider.Transport");
        } catch (Throwable t) {
            Reporter.log("install FAILED, SMS providers are NOT protected");
            XposedBridge.log(t);
        }
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        ContentProvider provider = resolveProvider(param.thisObject);
        if (provider == null) {
            return;
        }
        Context context = provider.getContext();
        heartbeat.maybeSend(context);

        String providerClass = provider.getClass().getName();
        if (!GuardPolicy.isGuardedProvider(providerClass)) {
            return;
        }
        int uid = Binder.getCallingUid();
        if (!GuardPolicy.isAppUid(uid)) {
            return;
        }
        if (!GuardPolicy.shouldBlock(uid, holdsReadSms(context, uid))) {
            return;
        }

        String method = param.method.getName();
        param.setThrowable(new SecurityException(
                "SmsGuard: uid " + uid + " needs READ_SMS to call " + providerClass + "#" + method));
        int count = throttle.record(uid, System.currentTimeMillis());
        if (count > 0) {
            Reporter.reportBlocked(context, uid, providerClass, method, count);
        }
    }

    /** Fails closed: if the check itself breaks, the app is treated as unprivileged. */
    private boolean holdsReadSms(Context context, int uid) {
        if (context == null) {
            return false;
        }
        try {
            return context.checkPermission(Manifest.permission.READ_SMS, Binder.getCallingPid(), uid)
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            if (permissionErrorLogged.compareAndSet(false, true)) {
                Reporter.log("READ_SMS check failed, blocking caller: " + t);
            }
            return false;
        }
    }

    /**
     * Without the provider we cannot tell guarded from unguarded calls; letting the call through
     * keeps the phone process working, and the failure is logged once so it is not silent.
     */
    private ContentProvider resolveProvider(Object transport) {
        try {
            return (ContentProvider) getContentProvider.invoke(transport);
        } catch (Throwable t) {
            if (resolveErrorLogged.compareAndSet(false, true)) {
                Reporter.log("cannot resolve provider from Transport, calls pass UNCHECKED");
                XposedBridge.log(t);
            }
            return null;
        }
    }
}
