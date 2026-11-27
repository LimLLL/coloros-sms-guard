package io.github.limlll.smsguard.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReportThrottleTest {

    private static final long WINDOW = 10_000;

    @Test
    public void firstDenialIsReportedImmediately() {
        assertEquals(1, new ReportThrottle(WINDOW).record(10_001, 0));
    }

    @Test
    public void burstIsCoalescedAndNoDenialIsLostOnceWindowPasses() {
        ReportThrottle throttle = new ReportThrottle(WINDOW);
        int reported = throttle.record(10_001, 0);
        for (int i = 1; i <= 500; i++) {
            assertEquals(0, throttle.record(10_001, i));
        }
        reported += throttle.record(10_001, WINDOW);
        assertEquals(502, reported);
    }

    @Test
    public void uidsAreThrottledIndependently() {
        ReportThrottle throttle = new ReportThrottle(WINDOW);
        assertEquals(1, throttle.record(10_001, 0));
        assertEquals(1, throttle.record(10_002, 1));
        assertEquals(0, throttle.record(10_001, 2));
    }
}
