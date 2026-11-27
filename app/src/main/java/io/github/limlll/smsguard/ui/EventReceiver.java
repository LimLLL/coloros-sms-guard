package io.github.limlll.smsguard.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import io.github.limlll.smsguard.core.Events;

/**
 * Stores events sent by the hook. The manifest restricts senders to holders of
 * READ_PRIVILEGED_PHONE_STATE, so records cannot be forged by ordinary apps.
 */
public final class EventReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsGuard";

    @Override
    public void onReceive(Context context, Intent intent) {
        InterceptStore store = new InterceptStore(context);
        long at = intent.getLongExtra(Events.EXTRA_TIME, System.currentTimeMillis());
        String action = intent.getAction();

        if (Events.ACTION_HEARTBEAT.equals(action)) {
            store.setHeartbeat(at);
        } else if (Events.ACTION_BLOCKED.equals(action)) {
            int uid = intent.getIntExtra(Events.EXTRA_UID, -1);
            if (uid < 0) {
                Log.w(TAG, "ignoring block event without uid");
                return;
            }
            store.addBlocked(uid,
                    intent.getStringArrayExtra(Events.EXTRA_PACKAGES),
                    intent.getStringExtra(Events.EXTRA_PROVIDER),
                    intent.getStringExtra(Events.EXTRA_METHOD),
                    Math.max(1, intent.getIntExtra(Events.EXTRA_COUNT, 1)),
                    at);
            // A block also proves the hook is alive.
            store.setHeartbeat(at);
        }
    }
}
