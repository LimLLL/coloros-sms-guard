package io.github.limlll.smsguard.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Decides which telephony content-provider calls must be rejected to close CVE-2025-10184.
 *
 * <p>The OPlus providers below skip write-permission checks and build SQL from the caller's
 * selection inside update(), so any app can blind-inject into mmssms.db. Requiring READ_SMS
 * restores the boundary Android intends: callers that could read SMS anyway keep working.
 */
public final class GuardPolicy {

    private static final Set<String> GUARDED_PROVIDERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "com.android.providers.telephony.PushMessageProvider",
            "com.android.providers.telephony.PushShopProvider",
            "com.android.providers.telephony.ServiceNumberProvider")));

    // Mirrors android.os.UserHandle.PER_USER_RANGE and Process.FIRST_APPLICATION_UID.
    private static final int PER_USER_RANGE = 100_000;
    private static final int FIRST_APPLICATION_UID = 10_000;

    private GuardPolicy() {
    }

    public static boolean isGuardedProvider(String providerClassName) {
        return GUARDED_PROVIDERS.contains(providerClassName);
    }

    /**
     * True for app uids in any user (e.g. ColorOS app clones in user 999) and for isolated
     * processes; false for core system uids such as system, phone and shell.
     */
    public static boolean isAppUid(int uid) {
        return uid % PER_USER_RANGE >= FIRST_APPLICATION_UID;
    }

    public static boolean shouldBlock(int callingUid, boolean callerHoldsReadSms) {
        return isAppUid(callingUid) && !callerHoldsReadSms;
    }
}
