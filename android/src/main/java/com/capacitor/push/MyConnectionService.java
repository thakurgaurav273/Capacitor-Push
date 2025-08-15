package com.capacitor.push;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.StatusHints;
import android.graphics.drawable.Icon;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom Android ConnectionService for managing VoIP connections.
 * Handles incoming calls, call events (answer/reject/disconnect), and call state management.
 */


@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MyConnectionService extends ConnectionService {
    // Map to track active connections using sessionId as key
    private static final Map<String, Connection> activeConnections = new HashMap<>();
    /**
     * Called when an incoming call is created by the Telecom framework.
     * Configures connection properties, caller display info, and status hints.
     *
     * @param accountHandle The PhoneAccountHandle for this call.
     * @param request       Connection request containing extras and caller address.
     * @return Configured Connection instance representing the call.
     */
    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle accountHandle, ConnectionRequest request) {
        final String sessionId = request.getExtras() != null ? request.getExtras().getString("sessionId") : null;
        final String callType = request.getExtras() != null ? request.getExtras().getString("callType") : null;
        final String senderName = request.getExtras() != null ? request.getExtras().getString("senderName") : null;
        // Create and configure the Connection for this incoming call
        Connection connection = new Connection() {
            /**
             * Utility function to check if the app is currently running in the foreground.
             *
             * @param context Context used to access ActivityManager.
             * @return true if app is in foreground, false otherwise.
             */
            private boolean isAppInForeground(Context context) {
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager == null) return false;
                List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
                if (appProcesses == null) return false;
                final String packageName = context.getPackageName();
                for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                    if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                            appProcess.processName.equals(packageName)) {
                        return true;
                    }
                }
                return false;
            }
            /**
             * Called when user answers the call.
             * Notifies plugin, updates app UI, disconnects and cleans up the connection.
             *
             * @param videoState The video state of the call.
             */
            @Override
            public void onAnswer(int videoState) {
                super.onAnswer(videoState);
                if (!isAppInForeground(getApplicationContext())){
                    // Bring app to foreground if not active when call is answered
                    bringAppToForeground();
                    // Delay event to ensure foreground launch is settled
                    new android.os.Handler().postDelayed(() -> {
                        CapacitorPushPlugin.sendVoipCallAcceptedEvent(sessionId, callType);
                    }, 150);
                }else {
                        CapacitorPushPlugin.sendVoipCallAcceptedEvent(sessionId, callType);
                }
                // Disconnect and destroy connection after answer event
                setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
                destroy();
            }
            /**
             * Called when user rejects the call.
             * Notifies plugin, brings app to foreground and cleans up connection.
             */
            @Override
            public void onReject() {
                super.onReject();
                bringAppToForeground();
                new android.os.Handler().postDelayed(() -> {
                    CapacitorPushPlugin.sendVoipCallDeclineEvent(sessionId);
                }, 150);
                setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));
                destroy();
            }
            /**
             * Called when the call disconnects (e.g., user ends call).
             * Cleans up the connection and removes it from the map.
             */
            @Override
            public void onDisconnect() {
                super.onDisconnect();
                setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
                activeConnections.remove(sessionId);
                destroy();
            }
        };

        // Set the connection's address for call logs/history
        Uri callerUri = request.getAddress();
        if (callerUri == null && senderName != null) {
            // Use SIP scheme if no address is provided
            callerUri = Uri.fromParts(PhoneAccount.SCHEME_SIP, sessionId, null);
        }
        connection.setAddress(callerUri, TelecomManager.PRESENTATION_ALLOWED);
        // Set display name for the caller (appears in UI)
        String display = senderName != null ? senderName : "Incoming Call" + callType;
        connection.setCallerDisplayName(display, TelecomManager.PRESENTATION_ALLOWED);
        StatusHints hints = new StatusHints("WhatsApp Clone", null, null);
        // Set call status hints (subtitle/branding in call screen)
        connection.setStatusHints(hints);
        // Set the call as ringing
        connection.setRinging();
        // Register connection with its session ID
        activeConnections.put(sessionId, connection);

        return connection;
    }
    /**
     * Utility method to bring the application to the foreground.
     */
    private void bringAppToForeground() {
        Intent mainIntent = getApplicationContext()
                .getPackageManager()
                .getLaunchIntentForPackage(getApplicationContext().getPackageName());
        if (mainIntent != null) {
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
        }
    }
    /**
     * Ends a call for the given sessionId by disconnecting and cleaning up resources.
     *
     * @param sessionId The unique call/session identifier.
     */
    public static void endCall(String sessionId) {
        Connection conn = activeConnections.get(sessionId);
        if (conn != null) {
            conn.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
            conn.destroy();
            activeConnections.remove(sessionId);
        }
    }



}
