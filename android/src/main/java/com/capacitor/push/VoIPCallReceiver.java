package com.capacitor.push;

import android.content.Context;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class VoIPCallReceiver {
    private static final String TAG = "VoIPCallReceiver";
    private static CapacitorPushPlugin pluginInstance;

    public static void setPluginInstance(CapacitorPushPlugin plugin) {
        pluginInstance = plugin;
        Log.d(TAG, "Plugin instance set");
    }

    public static void handleIncomingMessage(Context context, RemoteMessage remoteMessage) {
        Log.d(TAG, "Handling incoming message");
        
        if (pluginInstance != null) {
            pluginInstance.handlePushNotification(remoteMessage);
        } else {
            Log.w(TAG, "Plugin instance is null, cannot handle message");
            Log.w(TAG, "Plugin instance is null, checking for fallback type...");

            Map<String, String> dataMap = remoteMessage.getData();
            String type = dataMap.get("type");

            if ("call".equals(type)) {
                Log.d(TAG, "Fallback: Detected call type, showing incoming call UI");
                NotificationUtils.showIncomingCallUI(context, remoteMessage); // <-- 🔔 new method
            } else {
                Log.d(TAG, "Fallback: Regular message, showing fallback notification");
                NotificationUtils.showFallbackNotification(context, remoteMessage);
            }
        }
    }

    public static void notifyTokenRefresh(String token) {
        Log.d(TAG, "Token refresh notification: " + token);
        
        if (pluginInstance != null) {
            JSObject tokenData = new JSObject();
            tokenData.put("token", token);
            pluginInstance.handleTokenRefresh(tokenData);
        } else {
            Log.w(TAG, "Plugin instance is null, cannot notify token refresh");
        }
    }

    public static void handleCallAction(String sessionId, String action) {
        Log.d(TAG, "Handling call action: " + action + " for session: " + sessionId);
        
        if (pluginInstance != null) {
            pluginInstance.handleVoIPCallAction(sessionId, action);
        } else {
            Log.w(TAG, "Plugin instance is null, cannot handle call action");
        }
    }
}