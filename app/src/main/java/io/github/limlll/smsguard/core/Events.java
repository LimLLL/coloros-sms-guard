package io.github.limlll.smsguard.core;

/** Broadcast contract between the hook (com.android.phone process) and the module app. */
public final class Events {

    public static final String MODULE_PACKAGE = "io.github.limlll.smsguard";

    public static final String ACTION_BLOCKED = MODULE_PACKAGE + ".action.BLOCKED";
    /** Periodic proof that the hook is loaded in the phone process. */
    public static final String ACTION_HEARTBEAT = MODULE_PACKAGE + ".action.HEARTBEAT";

    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_UID = "uid";
    /** All packages sharing the blocked uid; may be absent if the uid has no package. */
    public static final String EXTRA_PACKAGES = "packages";
    public static final String EXTRA_PROVIDER = "provider";
    public static final String EXTRA_METHOD = "method";
    public static final String EXTRA_COUNT = "count";

    private Events() {
    }
}
