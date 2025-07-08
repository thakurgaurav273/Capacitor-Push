package com.capacitor.push;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CallActionReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String sessionId = intent.getStringExtra("sessionId");

        if ("CALL_ACCEPT".equals(action)) {
            Log.d("CallActionReceiver", "✅ Call accepted: " + sessionId);
            VoIPCallReceiver.handleCallAction(sessionId, "accept");

            // Optionally: launch activity directly here if needed
        } else if ("CALL_REJECT".equals(action)) {
            Log.d("CallActionReceiver", "❌ Call rejected: " + sessionId);
            VoIPCallReceiver.handleCallAction(sessionId, "reject");
        }

        // Cancel the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(sessionId.hashCode());
        }
    }
}
