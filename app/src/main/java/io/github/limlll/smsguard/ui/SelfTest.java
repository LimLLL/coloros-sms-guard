package io.github.limlll.smsguard.ui;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

/**
 * Probes the vulnerable providers exactly like an exploit would (update() from an app without
 * READ_SMS), so the user can see whether the device is protected. This app itself holds no SMS
 * permission, which makes it a faithful stand-in for a malicious caller.
 */
final class SelfTest {

    private static final String[] AUTHORITIES = {"push-mms", "push-shop", "service-number"};
    private static final String MODULE_MARKER = "SmsGuard";

    private SelfTest() {
    }

    /** Performs binder calls; must not run on the main thread. */
    static String run(ContentResolver resolver) {
        StringBuilder report = new StringBuilder();
        boolean vulnerable = false;
        for (String authority : AUTHORITIES) {
            String line;
            try {
                line = probe(resolver, authority);
            } catch (SecurityException e) {
                String message = String.valueOf(e.getMessage());
                line = message.contains(MODULE_MARKER) ? "✅ 已被本模块拦截" : "✅ 系统拒绝访问（可能已打官方补丁）";
            } catch (RuntimeException e) {
                vulnerable = true;
                line = "❌ 无权限应用可以调用，存在漏洞（" + e.getClass().getSimpleName() + "）";
            }
            report.append(authority).append("：").append(line).append('\n');
        }
        report.append('\n').append(vulnerable
                ? "结论：未受保护。确认模块已启用、作用域正确并已重启手机。"
                : "结论：已受保护。");
        return report.toString();
    }

    /** @return a result line if the call was not rejected; SecurityException means it was. */
    private static String probe(ContentResolver resolver, String authority) {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(authority);
        if (client == null) {
            return "➖ 本机没有这个组件";
        }
        try {
            // A column that cannot exist makes the UPDATE fail to compile, so a reachable
            // provider changes no rows; any outcome other than SecurityException means reachable.
            ContentValues values = new ContentValues();
            values.put("smsguard_probe_column", 0);
            client.update(Uri.parse("content://" + authority), values, "0=1", null);
            throw new IllegalStateException("update accepted");
        } catch (android.os.RemoteException e) {
            throw new IllegalStateException(e);
        } finally {
            client.close();
        }
    }
}
