package io.github.limlll.smsguard.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Intercept history, aggregated to one record per blocked uid. Only touched from the main thread
 * (receiver and activity), so no extra synchronization is needed.
 */
final class InterceptStore {

    static final class Record {
        int uid;
        String[] packages;
        int count;
        long firstAt;
        long lastAt;
        String provider;
        String method;
    }

    private static final String TAG = "SmsGuard";
    private static final String PREFS = "intercepts";
    private static final String KEY_HEARTBEAT = "heartbeat_at";
    private static final String RECORD_PREFIX = "uid:";

    private final SharedPreferences prefs;

    InterceptStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    void addBlocked(int uid, String[] packages, String provider, String method, int count, long at) {
        String key = RECORD_PREFIX + uid;
        Record record = parse(prefs.getString(key, null));
        if (record == null) {
            record = new Record();
            record.uid = uid;
            record.firstAt = at;
        }
        record.count += count;
        record.lastAt = at;
        record.provider = provider;
        record.method = method;
        // Keep the last known names if the app has since been uninstalled.
        if (packages != null && packages.length > 0) {
            record.packages = packages;
        }
        prefs.edit().putString(key, toJson(record)).apply();
    }

    void setHeartbeat(long at) {
        prefs.edit().putLong(KEY_HEARTBEAT, at).apply();
    }

    long heartbeatAt() {
        return prefs.getLong(KEY_HEARTBEAT, 0);
    }

    /** Most recently active first. */
    List<Record> records() {
        List<Record> records = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith(RECORD_PREFIX) && entry.getValue() instanceof String) {
                Record record = parse((String) entry.getValue());
                if (record != null) {
                    records.add(record);
                }
            }
        }
        records.sort((a, b) -> Long.compare(b.lastAt, a.lastAt));
        return records;
    }

    void clearRecords() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(RECORD_PREFIX)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    private static String toJson(Record record) {
        try {
            JSONObject json = new JSONObject()
                    .put("uid", record.uid)
                    .put("count", record.count)
                    .put("firstAt", record.firstAt)
                    .put("lastAt", record.lastAt)
                    .put("provider", record.provider)
                    .put("method", record.method);
            if (record.packages != null) {
                JSONArray packages = new JSONArray();
                for (String pkg : record.packages) {
                    packages.put(pkg);
                }
                json.put("packages", packages);
            }
            return json.toString();
        } catch (JSONException e) {
            throw new IllegalStateException("cannot serialize intercept record", e);
        }
    }

    private static Record parse(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(raw);
            Record record = new Record();
            record.uid = json.getInt("uid");
            record.count = json.getInt("count");
            record.firstAt = json.getLong("firstAt");
            record.lastAt = json.getLong("lastAt");
            record.provider = json.optString("provider", null);
            record.method = json.optString("method", null);
            JSONArray packages = json.optJSONArray("packages");
            if (packages != null) {
                record.packages = new String[packages.length()];
                for (int i = 0; i < packages.length(); i++) {
                    record.packages[i] = packages.getString(i);
                }
            }
            return record;
        } catch (JSONException e) {
            Log.w(TAG, "dropping unreadable intercept record", e);
            return null;
        }
    }
}
