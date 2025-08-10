package com.capacitor.push;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;

import com.getcapacitor.JSObject;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class NotificationUtils {

    public static final String CHANNEL_VOIP = "voip_calls";
    public static final String CHANNEL_MESSAGES = "messages_channel";

    // Avatar in-memory cache
    public static final Map<String, Bitmap> avatarMap =
            Collections.synchronizedMap(new HashMap<>());

    public static void putAvatar(String sessionId, Bitmap bmp) {
        avatarMap.put(sessionId, bmp);
    }

    public static Bitmap getAvatar(String sessionId) {
        return avatarMap.get(sessionId);
    }

    public static void removeAvatar(String sessionId) {
        avatarMap.remove(sessionId);
    }

    // Download and cache avatar before showing the call UI
    public static void preloadAvatarAsync(Context context, String sessionId, String avatarUrl) {
        new Thread(() -> {
            Bitmap avatar = getBitmapFromURL(avatarUrl);
            if (avatar != null) {
                putAvatar(sessionId, avatar);
                Log.d("NotificationUtils", "preloadAvatarAsync: " + avatar);
            }
        }).start();
    }

    public static void showNativeAndroidCallScreen(Context context, String senderName, String sessionId, String callType){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PhoneAccountUtils.registerPhoneAccount(context);
                Bundle extras = new Bundle();
                extras.putString("senderName", senderName);
                extras.putString("sessionId", sessionId);
                extras.putString("callType", callType);

                Uri callerUri = Uri.fromParts(PhoneAccount.SCHEME_SIP, senderName, null);
                extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, callerUri);

                TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                PhoneAccountHandle handle = PhoneAccountUtils.getPhoneAccountHandle(context);

                boolean enabled = false;
                for (PhoneAccountHandle h : telecomManager.getCallCapablePhoneAccounts()) {
                    if (PhoneAccountUtils.PHONE_ACCOUNT_ID.equals(h.getId())) {
                        enabled = true;
                        break;
                    }
                }

                if (!enabled) {
                    Intent intent = new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }

                boolean permitted = true;
                try {
                    permitted = telecomManager.isIncomingCallPermitted(handle);
                } catch (Exception ignored) {}

                if (enabled && permitted) {
                    telecomManager.addNewIncomingCall(handle, extras);
                    return;
                }
            }}
        catch (SecurityException ignored) {
            // Fallback to custom notification
        }
    }
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public static void showIncomingCallUI(Context context, RemoteMessage remoteMessage) {
        Map<String, String> dataMap = remoteMessage.getData();
        String sessionId = dataMap.get("sessionId");
        String senderName = dataMap.get("senderName");
        String callType = dataMap.get("callType");
        String senderAvatar = dataMap.get("senderAvatar");
        String callAction = dataMap.get("callAction");

        if (sessionId == null || senderName == null) return;

        // Preload avatar asynchronously
        if (!TextUtils.isEmpty(senderAvatar)) {
            preloadAvatarAsync(context, sessionId, senderAvatar);
        }

        // Handle call cancellation broadcast
        if ("cancelled".equalsIgnoreCase(callAction)) {
            Intent finishIntent = new Intent("com.capacitor.push.ACTION_CALL_CANCELLED");
            finishIntent.putExtra("sessionId", sessionId);
            try {
                context.sendBroadcast(finishIntent);
            } catch (NoClassDefFoundError e) {
                context.sendBroadcast(finishIntent);
            }
        }

        // Cancel notification & end ongoing call if needed
        if ("cancelled".equalsIgnoreCase(callAction) ||
                "rejected".equalsIgnoreCase(callAction) ||
                "accepted".equalsIgnoreCase(callAction) ||
                "ongoing".equalsIgnoreCase(callAction) ||
                "ended".equalsIgnoreCase(callAction)) {

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(sessionId.hashCode());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                MyConnectionService.endCall(sessionId);
            }
            return;
        }

        // Native Android Call UI for API 33+ (Android 13)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            showNativeAndroidCallScreen(context, senderName,sessionId, callType);
        }
    }

    public static void showFallbackNotification(Context context, RemoteMessage remoteMessage) {
        Map<String, String> dataMap = remoteMessage.getData();
        String title = dataMap.get("title");
        String body = dataMap.get("body");
        String senderAvatar = dataMap.get("senderAvatar");

        String receiverType = dataMap.get("receiverType");
        String sender = dataMap.get("sender");
        String receiver = dataMap.get("receiver");

        createMessageChannel(context);

        Intent tapIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assert tapIntent != null;

        String id = "user".equalsIgnoreCase(receiverType) ? sender : receiver;
        String convType = "user".equalsIgnoreCase(receiverType) ? "user" : "group";

        tapIntent.setAction("com.capacitor.push.NOTIFICATION_TAPPED");
        tapIntent.putExtra("id", id);
        tapIntent.putExtra("convType", convType);
        tapIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                .setContentTitle(title != null ? title : "New Message")
                .setContentText(body != null ? body : "")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (!TextUtils.isEmpty(senderAvatar)) {
            Bitmap avatar = getBitmapFromURL(senderAvatar);
            if (avatar != null) {
                builder.setLargeIcon(avatar);
            }
        } else {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_phone));
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            int msgId;
            try {
                msgId = Integer.parseInt(dataMap.get("tag"));
            } catch (Exception e) {
                msgId = new Random().nextInt();
            }
            manager.notify(msgId, builder.build());
        }
    }

    private static void recreateVoipChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.deleteNotificationChannel(CHANNEL_VOIP);
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_VOIP, "VoIP Calls", NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Used for incoming call notifications");
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                channel.setBypassDnd(true);
                channel.setShowBadge(false);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 1000, 1000, 1000});

                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                if (soundUri == null) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }

                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                channel.setSound(soundUri, audioAttributes);

                manager.createNotificationChannel(channel);
                Log.d("NotificationUtils", "VoIP channel recreated with sound: " + soundUri);
            }
        }
    }

    private static void createMessageChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(CHANNEL_MESSAGES) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Notifications for incoming messages");

                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                channel.setSound(soundUri, audioAttributes);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 250, 250, 250});
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static Bitmap getBitmapFromURL(String strURL) {
        try {
            if (strURL == null) return null;
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
