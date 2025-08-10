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

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MyConnectionService extends ConnectionService {
    private static final Map<String, Connection> activeConnections = new HashMap<>();
    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle accountHandle, ConnectionRequest request) {
        final String sessionId = request.getExtras() != null ? request.getExtras().getString("sessionId") : null;
        final String callType = request.getExtras() != null ? request.getExtras().getString("callType") : null;
        final String senderName = request.getExtras() != null ? request.getExtras().getString("senderName") : null;

        Bitmap avatarBitmap = NotificationUtils.getAvatar(sessionId);
        Icon avatarIcon;
        if (avatarBitmap != null) {
            avatarIcon = Icon.createWithBitmap(avatarBitmap);
        } else {
            avatarIcon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_phone);
        }

        Connection connection = new Connection() {
            // Utility method to check if the app is currently in the foreground
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

            @Override
            public void onAnswer(int videoState) {
                super.onAnswer(videoState);

                if (!isAppInForeground(getApplicationContext())){
                    bringAppToForeground();
                }
                Log.d("TAG", "onAnswer: ");
                new android.os.Handler().postDelayed(() -> {
                    CapacitorPushPlugin.sendVoipCallAcceptedEvent(sessionId, callType);
//                    setActive();
                }, 150);
                setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
                destroy();
            }

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
            @Override
            public void onDisconnect() {
                super.onDisconnect();
                setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
                activeConnections.remove(sessionId);
                destroy();
            }
        };

        // Set the connection address - this is crucial for call history
        Uri callerUri = request.getAddress();
        if (callerUri == null && senderName != null) {
            // Create a proper URI if not provided
            callerUri = Uri.fromParts(PhoneAccount.SCHEME_SIP, senderName, null);
        }
        connection.setAddress(callerUri, TelecomManager.PRESENTATION_ALLOWED);

        connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        String display = senderName != null ? senderName : "Incoming Call";
        connection.setCallerDisplayName(display, TelecomManager.PRESENTATION_ALLOWED);
        StatusHints hints = new StatusHints(display, avatarIcon, null);
        connection.setStatusHints(hints);
        connection.setRinging();
        activeConnections.put(sessionId, connection);

        return connection;
    }
    private void bringAppToForeground() {
        Intent mainIntent = getApplicationContext()
                .getPackageManager()
                .getLaunchIntentForPackage(getApplicationContext().getPackageName());
        if (mainIntent != null) {
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
        }
    }
    public static void endCall(String sessionId) {
        Connection conn = activeConnections.get(sessionId);
        if (conn != null) {
            conn.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
            conn.destroy();
            activeConnections.remove(sessionId);
        }
    }



}
