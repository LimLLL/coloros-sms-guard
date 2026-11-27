package io.github.limlll.smsguard.hook;

import android.content.Context;
import android.os.SystemClock;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Periodically tells the module app that the hook is alive in the phone process. Piggybacks on
 * provider traffic instead of a timer, so it needs no thread of its own.
 */
final class Heartbeat {

    private static final long INTERVAL_MS = 60 * 60_000L;
    /** Early in boot the user is still locked and broadcasts are refused; retry soon. */
    private static final long RETRY_MS = 60_000L;

    private final AtomicLong nextAtElapsedMs = new AtomicLong();

    void maybeSend(Context context) {
        if (context == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        long next = nextAtElapsedMs.get();
        if (now < next || !nextAtElapsedMs.compareAndSet(next, now + INTERVAL_MS)) {
            return;
        }
        if (!Reporter.sendHeartbeat(context)) {
            nextAtElapsedMs.set(now + RETRY_MS);
        }
    }
}
