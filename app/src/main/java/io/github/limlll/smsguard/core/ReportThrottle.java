package io.github.limlll.smsguard.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Coalesces repeated denials per uid. A blind-SQLi exploit leaks data one bit per call, so an
 * attack is thousands of calls; reporting each one would flood the log and the broadcast queue.
 *
 * <p>Denials that arrive after the last report of an attack burst are only reported when the
 * same uid is denied again after the window, so counts shown to the user are lower bounds.
 */
public final class ReportThrottle {

    private static final class UidState {
        long lastReportAtMs;
        int suppressed;
    }

    private final long windowMs;
    private final Map<Integer, UidState> states = new HashMap<>();

    public ReportThrottle(long windowMs) {
        this.windowMs = windowMs;
    }

    /**
     * Records one denial.
     *
     * @return number of denials to report now (this one plus any suppressed since the last
     *     report), or 0 if the uid was reported within the window.
     */
    public synchronized int record(int uid, long nowMs) {
        UidState state = states.get(uid);
        if (state == null) {
            state = new UidState();
            state.lastReportAtMs = nowMs;
            states.put(uid, state);
            return 1;
        }
        state.suppressed++;
        if (nowMs - state.lastReportAtMs < windowMs) {
            return 0;
        }
        int count = state.suppressed;
        state.suppressed = 0;
        state.lastReportAtMs = nowMs;
        return count;
    }
}
