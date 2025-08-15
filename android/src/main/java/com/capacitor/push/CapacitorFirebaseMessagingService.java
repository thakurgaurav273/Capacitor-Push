package com.capacitor.push;
import android.Manifest;
import android.content.Context;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.getcapacitor.JSObject;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Firebase Messaging Service for handling FCM messages and token events in Capacitor-based apps.
 * Processes push notifications, handles FCM token refresh, and passes events to your plugin/VoIP call receiver.
 */
public class CapacitorFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "CapacitorFCMService";
    /**
     * Called when the service is first created.
     * Initializes Firebase if not already done.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
                FirebaseApp.initializeApp(getApplicationContext());
                Log.d(TAG, "Firebase initialized in messaging service");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase in messaging service", e);
        }
    }

    /**
     * Called when a message is received from FCM.
     * Logs payloads and delegates handling to VoIPCallReceiver/plugin.
     *
     * @param remoteMessage The received Firebase RemoteMessage.
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        try {
            Context context = getApplicationContext();
            // Get the plugin instance and handle the message
            VoIPCallReceiver.handleIncomingMessage(context,remoteMessage);
        } catch (Exception e) {
            Log.e(TAG, "Error handling message", e);
        }
    }

    /**
     * Called when a new FCM token is generated for this device/app.
     * Wraps token info and notifies the plugin via VoIPCallReceiver.
     *
     * @param token The new registration token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // Create token data object for plugin
        JSObject tokenData = new JSObject();
        tokenData.put("token", token);

        // Notify plugin about token refresh
        try {
            VoIPCallReceiver.notifyTokenRefresh(String.valueOf(tokenData));
        } catch (Exception e) {
            Log.e(TAG, "Error notifying token refresh", e);
        }
    }
}
