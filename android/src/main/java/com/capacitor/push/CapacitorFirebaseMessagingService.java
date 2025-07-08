package com.capacitor.push;
import android.content.Context;

import android.util.Log;

import com.getcapacitor.JSObject;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CapacitorFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "CapacitorFCMService";

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

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Handle the message through the plugin
        handleMessage(remoteMessage);
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // Create token data object
        JSObject tokenData = new JSObject();
        tokenData.put("token", token);

        // Send token to plugin
        try {
            VoIPCallReceiver.notifyTokenRefresh(String.valueOf(tokenData));
        } catch (Exception e) {
            Log.e(TAG, "Error notifying token refresh", e);
        }
    }

    private void handleMessage(RemoteMessage remoteMessage) {
        try {
            Context context = getApplicationContext();
            // Get the plugin instance and handle the message
            VoIPCallReceiver.handleIncomingMessage(context,remoteMessage);
        } catch (Exception e) {
            Log.e(TAG, "Error handling message", e);
        }
    }
}
