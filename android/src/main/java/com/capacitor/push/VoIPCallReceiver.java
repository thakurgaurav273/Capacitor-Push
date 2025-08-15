package com.capacitor.push;

import android.Manifest;
import android.content.Context;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.getcapacitor.JSObject;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * VoIPCallReceiver
 * Handles incoming VOIP call notifications and refresh token events
 * for a Capacitor-based push notification plugin.
 */
public class VoIPCallReceiver {
    private static final String TAG = "VoIPCallReceiver";
    // Reference to the plugin instance for event callbacks
    private static CapacitorPushPlugin pluginInstance;
    /**
     * Sets the plugin instance for callbacks.
     * Should be called once the CapacitorPushPlugin is initialized.
     *
     * @param plugin The CapacitorPushPlugin instance
     */
    public static void setPluginInstance(CapacitorPushPlugin plugin) {
        pluginInstance = plugin;
        Log.d(TAG, "Plugin instance set");
    }
    /**
     * Handles incoming Firebase remote messages.
     * Checks if the message is a VOIP call or a text notification based on the payload 'type'.
     * Requires READ_PHONE_STATE permission if the incoming call UI interacts with phone state.
     *
     * @param context       Current application context
     * @param remoteMessage The incoming message from Firebase
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public static void handleIncomingMessage(Context context, RemoteMessage remoteMessage) {
        Log.d(TAG, "Handling incoming message");
        Map<String, String> dataMap = remoteMessage.getData();
        String type = dataMap.get("type");

        Log.e(TAG, "Incoming payload"+type);

        if ("call".equals(type)) {
            // Show incoming call UI for call-type notifications
            NotificationUtils.showIncomingCallUI(context, remoteMessage);
        } else {
            // Show standard text notification for other types
            Log.d(TAG, "Fallback: Regular text message notification");
            NotificationUtils.showTextNotificationUI(context, remoteMessage);
        }

    }
    /**
     * Notifies the plugin of a refreshed Firebase token for push notifications.
     * Sends token data back to the bridge plugin if available.
     *
     * @param token The new push notification token
     */
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
}