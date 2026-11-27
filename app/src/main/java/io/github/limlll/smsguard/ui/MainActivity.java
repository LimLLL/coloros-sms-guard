package io.github.limlll.smsguard.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Single-screen log viewer: hook status plus the apps that were blocked from reading SMS. */
public final class MainActivity extends Activity {

    /** Heartbeats are hourly, so two missed ones suggest the hook is no longer loaded. */
    private static final long STALE_HEARTBEAT_MS = 2 * 60 * 60_000L;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private InterceptStore store;
    private TextView statusView;
    private TextView recordsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new InterceptStore(this);

        int padding = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setFitsSystemWindows(true);

        statusView = new TextView(this);
        statusView.setTextSize(15);

        Button refresh = new Button(this);
        refresh.setText("刷新");
        refresh.setOnClickListener(v -> render());
        Button clear = new Button(this);
        clear.setText("清空记录");
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage("清空所有拦截记录？")
                .setPositiveButton("清空", (dialog, which) -> {
                    store.clearRecords();
                    render();
                })
                .setNegativeButton("取消", null)
                .show());
        Button selfTest = new Button(this);
        selfTest.setText("自检");
        selfTest.setOnClickListener(v -> runSelfTest(selfTest));
        LinearLayout buttons = new LinearLayout(this);
        buttons.addView(selfTest);
        buttons.addView(refresh);
        buttons.addView(clear);

        recordsView = new TextView(this);
        recordsView.setTextIsSelectable(true);
        recordsView.setPadding(0, dp(8), 0, 0);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(recordsView);

        root.addView(statusView);
        root.addView(buttons);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        statusView.setText(describeStatus(store.heartbeatAt()));

        List<InterceptStore.Record> records = store.records();
        if (records.isEmpty()) {
            recordsView.setText("暂无拦截记录。");
            return;
        }
        StringBuilder text = new StringBuilder("以下应用没有短信权限，却试图访问短信数据库（CVE-2025-10184 的利用方式），已被拦截：\n\n");
        for (InterceptStore.Record record : records) {
            text.append("⚠ ").append(describeApp(record)).append('\n')
                    .append("  uid ").append(record.uid).append(" · 共拦截 ").append(record.count).append(" 次\n")
                    .append("  首次 ").append(timeFormat.format(new Date(record.firstAt))).append('\n')
                    .append("  最近 ").append(timeFormat.format(new Date(record.lastAt))).append('\n')
                    .append("  调用 ").append(shortClassName(record.provider)).append('#').append(record.method)
                    .append("\n\n");
        }
        recordsView.setText(text);
    }

    private void runSelfTest(Button trigger) {
        trigger.setEnabled(false);
        new Thread(() -> {
            String report = SelfTest.run(getContentResolver());
            runOnUiThread(() -> {
                trigger.setEnabled(true);
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle("漏洞自检")
                        .setMessage(report)
                        .setPositiveButton("好", null)
                        .show();
                // The hook's broadcast may land shortly after the probe returns.
                recordsView.postDelayed(this::render, 1_000);
            });
        }, "smsguard-selftest").start();
    }

    private String describeStatus(long heartbeatAt) {
        if (heartbeatAt == 0) {
            return "未收到模块心跳。请确认：\n"
                    + "1. LSPosed 中已启用本模块，并勾选「电话/短信存储」和「电话服务」\n"
                    + "2. 已重启手机\n"
                    + "3. 已允许本应用自启动（否则收不到电话进程发来的记录）";
        }
        String status = "模块已在电话进程生效\n最近心跳：" + timeFormat.format(new Date(heartbeatAt));
        if (System.currentTimeMillis() - heartbeatAt > STALE_HEARTBEAT_MS) {
            status += "\n（超过 2 小时没有心跳，模块可能未加载或本应用被限制自启动）";
        }
        return status;
    }

    private String describeApp(InterceptStore.Record record) {
        if (record.packages == null || record.packages.length == 0) {
            return "未知应用";
        }
        PackageManager pm = getPackageManager();
        StringBuilder names = new StringBuilder();
        for (String pkg : record.packages) {
            if (names.length() > 0) {
                names.append("、");
            }
            if (pkg.equals(getPackageName())) {
                names.append("本应用自检");
                continue;
            }
            try {
                names.append(pm.getApplicationInfo(pkg, 0).loadLabel(pm)).append(" (").append(pkg).append(')');
            } catch (PackageManager.NameNotFoundException e) {
                names.append(pkg).append("（已卸载）");
            }
        }
        return names.toString();
    }

    private static String shortClassName(String className) {
        if (className == null) {
            return "?";
        }
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
