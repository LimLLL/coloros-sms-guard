package io.github.limlll.smsguard.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GuardPolicyTest {

    @Test
    public void guardsOnlyTheVulnerableOplusProviders() {
        assertTrue(GuardPolicy.isGuardedProvider("com.android.providers.telephony.PushMessageProvider"));
        assertTrue(GuardPolicy.isGuardedProvider("com.android.providers.telephony.PushShopProvider"));
        assertTrue(GuardPolicy.isGuardedProvider("com.android.providers.telephony.ServiceNumberProvider"));
        // The regular SMS providers already enforce READ_SMS and must stay untouched.
        assertFalse(GuardPolicy.isGuardedProvider("com.android.providers.telephony.SmsProvider"));
        assertFalse(GuardPolicy.isGuardedProvider("com.android.providers.telephony.MmsSmsProvider"));
    }

    @Test
    public void coreSystemUidsAreNeverBlocked() {
        int[] systemUids = {0, 1000, 1001, 2000, 10 * 100_000 + 1001};
        for (int uid : systemUids) {
            assertFalse("uid " + uid, GuardPolicy.shouldBlock(uid, false));
        }
    }

    @Test
    public void appsWithoutReadSmsAreBlockedInEveryUser() {
        assertTrue(GuardPolicy.shouldBlock(10_320, false));
        // ColorOS app clones live in user 999.
        assertTrue(GuardPolicy.shouldBlock(999 * 100_000 + 10_320, false));
        // Isolated processes have no permissions of their own.
        assertTrue(GuardPolicy.shouldBlock(99_123, false));
    }

    @Test
    public void appsHoldingReadSmsKeepAccess() {
        assertFalse(GuardPolicy.shouldBlock(10_246, true));
    }
}
