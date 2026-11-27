package io.github.limlll.smsguard.hook;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

import java.util.Arrays;

import de.robv.android.xposed.XposedBridge;
import io.github.limlll.smsguard.core.Events;

/**
 * Sends hook events to the LSPosed log and to the module app. Runs inside provider calls of the
 * phone process, so it must never throw back into the hooked method.
 */
final class Reporter {

    private static final String TAG = "SmsGuard: ";

    private Reporter() {
    }

    static void log(String message) {
        XposedBridge.log(TAG + message);
    }

    /** The LSPosed log line is written first so a record survives even if the broadcast is lost. */
    static void reportBlocked(Context context, int uid, String provider, String method, int count) {
        try {
            String[] packages = context == null ? null : context.getPackageManager().getPackagesForUid(uid);
            log("blocked uid=" + uid + " packages=" + Arrays.toString(packages)
                    + " target=" + provider + "#" + method + " count=" + count);
            if (context == null) {
                return;
            }
            context.sendBroadcast(newIntent(Events.ACTION_BLOCKED)
                    .putExtra(Events.EXTRA_UID, uid)
                    .putExtra(Events.EXTRA_PACKAGES, packages)
                    .putExtra(Events.EXTRA_PROVIDER, provider)
                    .putExtra(Events.EXTRA_METHOD, method)
                    .putExtra(Events.EXTRA_COUNT, count));
        } catch (Throwable t) {
            log("failed to report block of uid " + uid + ": " + t);
        }
    }

    /** @return false if the event could not be delivered yet and should be retried later. */
    static boolean sendHeartbeat(Context context) {
        try {
            // The module app is not direct-boot aware, so it cannot receive before first unlock.
            UserManager userManager = context.getSystemService(UserManager.class);
            if (userManager == null || !userManager.isUserUnlocked()) {
                return false;
            }
            context.sendBroadcast(newIntent(Events.ACTION_HEARTBEAT));
            return true;
        } catch (Throwable t) {
            log("heartbeat failed: " + t);
            return false;
        }
    }

    private static Intent newIntent(String action) {
        return new Intent(action)
                .setPackage(Events.MODULE_PACKAGE)
                // The app may never have been launched (stopped state) or may be cached.
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES | Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra(Events.EXTRA_TIME, System.currentTimeMillis());
    }
}
