package com.capacitor.push;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.getcapacitor.JSObject;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;
//
///*public class NotificationUtils {
//    private static final String CHANNEL_ID = "messages_channel";
//
//    public static Bitmap getBitmapFromURL(String strURL) {
//        if (strURL != null) {
//            try {
//                URL url = new URL(strURL);
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.setDoInput(true);
//                connection.connect();
//                InputStream input = connection.getInputStream();
//                return BitmapFactory.decodeStream(input);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return null;
//    }
//
//    public static void showIncomingCallUI(Context context, RemoteMessage remoteMessage) {
//        Map<String, String> dataMap = remoteMessage.getData();
//        String sessionId = dataMap.get("sessionId");
//        String senderName = dataMap.get("senderName");
//        String callType = dataMap.get("callType");
//        String senderAvatar = dataMap.get("senderAvatar");
//
//        if (sessionId == null || senderName == null) {
//            return;
//        }
//
//        // Prepare intent to open full screen VoIPCallActivity
//        Intent fullScreenIntent = new Intent(context, VoIPCallActivity.class);
//        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
////         Pass voipData as JSON
//        JSObject voipData = new JSObject();
//        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
//            voipData.put(entry.getKey(), entry.getValue());
//        }
//        voipData.put("id", remoteMessage.getMessageId());
////        fullScreenIntent.putExtra("sessionId",sessionId);
////        fullScreenIntent.putExtra("senderName",senderName);
////        fullScreenIntent.putExtra("callType",callType);
////        fullScreenIntent.putExtra("senderAvatar",senderAvatar);
////        fullScreenIntent.putExtra("",sessionId);
//        fullScreenIntent.putExtra("voipData", voipData.toString());
//        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
//                context,
//                0,
//                fullScreenIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        // Setup VoIP call channel
//        String channelId = "voip_calls";
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    channelId,
//                    "VoIP Calls",
//                    NotificationManager.IMPORTANCE_HIGH
//            );
//            channel.setDescription("Incoming call notifications");
//            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
//            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
//            channel.enableVibration(true);
//
//            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
//            AudioAttributes audioAttributes = new AudioAttributes.Builder()
//                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
//                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                    .build();
//            channel.setSound(soundUri, audioAttributes);
//
//
//            NotificationManager manager = context.getSystemService(NotificationManager.class);
//            if (manager != null) {
//                manager.createNotificationChannel(channel);
//            }
//        }
//
//        // Setup actions
//        Intent acceptIntent = new Intent(context, CallActionReceiver.class);
//        acceptIntent.setAction("CALL_ACCEPT");
//        acceptIntent.putExtra("sessionId", sessionId);
//        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
//                context,
//                0,
//                acceptIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        Intent rejectIntent = new Intent(context, CallActionReceiver.class);
//        rejectIntent.setAction("CALL_REJECT");
//        rejectIntent.putExtra("sessionId", sessionId);
//        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(
//                context,
//                1,
//                rejectIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        // Build Notification
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
////                .setContentTitle(senderName)
////                .setContentText("Gaurav " + callType + " call")
//                .setSmallIcon(R.drawable.ic_phone) // Use your app’s icon
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setCategory(NotificationCompat.CATEGORY_CALL)
//                .setAutoCancel(false)
//                .setOngoing(true)
//                .setFullScreenIntent(fullScreenPendingIntent, true)
//                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
////                .addAction(R.drawable.ic_notification_large, "Answer", acceptPendingIntent)
////                .addAction(R.drawable.ic_video_call, "Reject", rejectPendingIntent);
//
//        // Optionally add avatar as large icon
////        if (senderAvatar != null && !senderAvatar.isEmpty()) {
////            Bitmap avatar = NotificationUtils.getBitmapFromURL(senderAvatar);
////            if (avatar != null) {
////                builder.setLargeIcon(avatar);
////            }
////        }
//
//        // Post notification
//        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        if (notificationManager != null) {
//            int notificationId = sessionId.hashCode(); // consistent ID for canceling later
//            notificationManager.notify(notificationId, builder.build());
//        }
//    }
//
//    public static void showFallbackNotification(Context context, RemoteMessage remoteMessage) {
//        Log.d("NotificationUtils", "Showing fallback notification");
//
//        Map<String, String> dataMap = remoteMessage.getData();
//        String title = dataMap.get("title");
//        String body = dataMap.get("body");
//
//        // Create notification channel if needed
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID,
//                    "Messages",
//                    NotificationManager.IMPORTANCE_HIGH
//            );
//            channel.setDescription("Notifications for incoming messages");
//            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//            if (manager != null) {
//                manager.createNotificationChannel(channel);
//            }
//        }
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setContentTitle(title != null ? title : "New Message")
//                .setContentText(body != null ? body : "")
//                .setPriority(NotificationCompat.PRIORITY_MAX)
//                .setSmallIcon(android.R.drawable.ic_dialog_info)
//                .setAutoCancel(true);
//
//        // Optional large icon
////        String largeIconUrl = dataMap.get("senderAvatar");
////        if (!TextUtils.isEmpty(largeIconUrl)) {
////            builder.setLargeIcon(getBitmapFromURL(largeIconUrl));
////        } else {
////            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_phone));
////        }
//
//        // Open app intent
//        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                context,
//                0,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//        builder.setContentIntent(pendingIntent);
//
//        Notification notification = builder.build();
//        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        int msgId;
//        try {
//            msgId = Integer.parseInt(dataMap.get("tag"));
//        } catch (Exception e) {
//            msgId = new Random().nextInt();
//        }
//
//        if (manager != null) {
//            manager.notify(msgId, notification);
//        }
//    }
//}*/
//
//public class NotificationUtils {
//
//    public static final String CHANNEL_VOIP = "voip_calls";
//    public static final String CHANNEL_MESSAGES = "messages_channel";
//
//    // === VOIP INCOMING CALL NOTIFICATION ===
//    public static void showIncomingCallUI(Context context, RemoteMessage remoteMessage) {
//        Map<String, String> dataMap = remoteMessage.getData();
//        String sessionId = dataMap.get("sessionId");
//        String senderName = dataMap.get("senderName");
//        String callType = dataMap.get("callType");
//
//        if (sessionId == null || senderName == null) return;
//
//        // ✅ Create or update VoIP notification channel
//        createVoipChannel(context);
//
//        // ✅ Full screen intent
//        Intent fullScreenIntent = new Intent(context, VoIPCallActivity.class);
//        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        JSObject voipData = new JSObject();
//        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
//            voipData.put(entry.getKey(), entry.getValue());
//        }
//        voipData.put("id", remoteMessage.getMessageId());
//        fullScreenIntent.putExtra("voipData", voipData.toString());
//
//        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
//                context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        // ✅ Build full-screen notification
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_VOIP)
//                .setSmallIcon(R.drawable.ic_phone)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setCategory(NotificationCompat.CATEGORY_CALL)
//                .setFullScreenIntent(fullScreenPendingIntent, true)
//                .setOngoing(true) // Not swipe-dismissible
//                .setAutoCancel(false)
//                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
//
//        // ✅ Post notification
//        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        if (manager != null) {
//            manager.notify(sessionId.hashCode(), builder.build());
//        }
//    }
//
//    private static void createVoipChannel(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationManager manager = context.getSystemService(NotificationManager.class);
//            if (manager != null && manager.getNotificationChannel(CHANNEL_VOIP) == null) {
//                NotificationChannel channel = new NotificationChannel(
//                        CHANNEL_VOIP, "VoIP Calls", NotificationManager.IMPORTANCE_HIGH
//                );
//                channel.setDescription("Used for incoming call notifications");
//                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
//                channel.enableVibration(true);
//
//                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
//                AudioAttributes audioAttributes = new AudioAttributes.Builder()
//                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
//                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                        .build();
//                channel.setSound(soundUri, audioAttributes);
//
//                manager.createNotificationChannel(channel);
//            }
//        }
//    }
//
//    // === MESSAGE NOTIFICATION ===
//    public static void showFallbackNotification(Context context, RemoteMessage remoteMessage) {
//        Map<String, String> dataMap = remoteMessage.getData();
//        String title = dataMap.get("title");
//        String body = dataMap.get("body");
//
//        // ✅ Create or update message notification channel
//        createMessageChannel(context);
//
//        // ✅ Intent to open app
//        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MESSAGES)
//                .setContentTitle(title != null ? title : "New Message")
//                .setContentText(body != null ? body : "")
//                .setSmallIcon(android.R.drawable.ic_dialog_info)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(true)
//                .setContentIntent(pendingIntent);
//
//        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        if (manager != null) {
//            int msgId;
//            try {
//                msgId = Integer.parseInt(dataMap.get("tag"));
//            } catch (Exception e) {
//                msgId = new Random().nextInt();
//            }
//            manager.notify(msgId, builder.build());
//        }
//    }
//
//    private static void createMessageChannel(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationManager manager = context.getSystemService(NotificationManager.class);
//            if (manager != null && manager.getNotificationChannel(CHANNEL_MESSAGES) == null) {
//                NotificationChannel channel = new NotificationChannel(
//                        CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH
//                );
//                channel.setDescription("Notifications for incoming messages");
//                manager.createNotificationChannel(channel);
//            }
//        }
//    }
//
//    // === UTILITY ===
//    public static Bitmap getBitmapFromURL(String strURL) {
//        try {
//            if (strURL == null) return null;
//            URL url = new URL(strURL);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setDoInput(true);
//            connection.connect();
//            InputStream input = connection.getInputStream();
//            return BitmapFactory.decodeStream(input);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//}
//public class NotificationUtils {
//
//    public static final String CHANNEL_VOIP = "voip_calls";
//    public static final String CHANNEL_MESSAGES = "messages_channel";
//
//    // === VOIP INCOMING CALL NOTIFICATION ===
//    public static void showIncomingCallUI(Context context, RemoteMessage remoteMessage) {
//        Map<String, String> dataMap = remoteMessage.getData();
//        String sessionId = dataMap.get("sessionId");
//        String senderName = dataMap.get("senderName");
//        String callType = dataMap.get("callType");
//        String senderAvatar = dataMap.get("senderAvatar");
//
//        if (sessionId == null || senderName == null) return;
//
//        // ✅ Create VoIP channel with ringtone
//        createVoipChannel(context);
//
//        // ✅ Full screen intent
//        Intent fullScreenIntent = new Intent(context, VoIPCallActivity.class);
//        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        JSObject voipData = new JSObject();
//        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
//            voipData.put(entry.getKey(), entry.getValue());
//        }
//        voipData.put("id", remoteMessage.getMessageId());
//        fullScreenIntent.putExtra("voipData", voipData.toString());
//
//        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
//                context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        // ✅ Setup Accept/Decline actions
//        Intent acceptIntent = new Intent(context, CallActionReceiver.class);
//        acceptIntent.setAction("CALL_ACCEPT");
//        acceptIntent.putExtra("sessionId", sessionId);
//        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
//                context, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        Intent declineIntent = new Intent(context, CallActionReceiver.class);
//        declineIntent.setAction("CALL_REJECT");
//        declineIntent.putExtra("sessionId", sessionId);
//        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(
//                context, 1, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        // ✅ Build full-screen notification with proper content
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_VOIP)
//                .setContentTitle(senderName)
//                .setContentText("Incoming " + (callType != null ? callType : "voice") + " call")
//                .setSmallIcon(R.drawable.ic_phone)
//                .setPriority(NotificationCompat.PRIORITY_MAX) // Use MAX for calls
//                .setCategory(NotificationCompat.CATEGORY_CALL)
//                .setFullScreenIntent(fullScreenPendingIntent, true)
//                .setOngoing(true) // Not dismissible
//                .setAutoCancel(false)
//                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//                .setContentIntent(fullScreenPendingIntent) // Tap to open call screen
//                .addAction(R.drawable.ic_phone, "Accept", acceptPendingIntent)
//                .addAction(R.drawable.ic_phone, "Decline", declinePendingIntent);
//
//        // ✅ Add avatar as large icon if available
//        if (!TextUtils.isEmpty(senderAvatar)) {
//            Bitmap avatar = getBitmapFromURL(senderAvatar);
//            if (avatar != null) {
//                builder.setLargeIcon(avatar);
//            }
//        }
//
//        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        if (manager != null) {
//            manager.notify(sessionId.hashCode(), builder.build());
//        }
//    }
//
//    // === MESSAGE NOTIFICATION ===
//    public static void showFallbackNotification(Context context, RemoteMessage remoteMessage) {
//        Map<String, String> dataMap = remoteMessage.getData();
//        String title = dataMap.get("title");
//        String body = dataMap.get("body");
//        String senderAvatar = dataMap.get("senderAvatar");
//
//        // ✅ Create message channel with normal notification sound
//        createMessageChannel(context);
//
//        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MESSAGES)
//                .setContentTitle(title != null ? title : "New Message")
//                .setContentText(body != null ? body : "")
//                .setSmallIcon(android.R.drawable.ic_dialog_info)
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Lower priority for messages
//                .setAutoCancel(true)
//                .setContentIntent(pendingIntent);
//
//        // ✅ Add avatar as large icon if available
//        if (!TextUtils.isEmpty(senderAvatar)) {
//            Bitmap avatar = getBitmapFromURL(senderAvatar);
//            if (avatar != null) {
//                builder.setLargeIcon(avatar);
//            }
//        } else {
//            // Fallback to default icon if no avatar URL
//            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_phone));
//        }
//
//        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        if (manager != null) {
//            int msgId;
//            try {
//                msgId = Integer.parseInt(dataMap.get("tag"));
//            } catch (Exception e) {
//                msgId = new Random().nextInt();
//            }
//            manager.notify(msgId, builder.build());
//        }
//    }
//
//    // VoIP channel with ringtone and high importance
//    private static void createVoipChannel(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationManager manager = context.getSystemService(NotificationManager.class);
//            if (manager != null && manager.getNotificationChannel(CHANNEL_VOIP) == null) {
//                NotificationChannel channel = new NotificationChannel(
//                        CHANNEL_VOIP, "VoIP Calls", NotificationManager.IMPORTANCE_HIGH
//                );
//                channel.setDescription("Used for incoming call notifications");
//                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
//                channel.enableVibration(true);
//                channel.setBypassDnd(true); // Bypass Do Not Disturb
//                channel.setShowBadge(false); // Don't show badge for calls
//
//                // Continuous vibration pattern for calls
//                channel.setVibrationPattern(new long[]{0, 1000, 1000, 1000, 1000, 1000});
//
//                // Ringtone for calls
//                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
//                AudioAttributes audioAttributes = new AudioAttributes.Builder()
//                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
//                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                        .build();
//                channel.setSound(soundUri, audioAttributes);
//
//                manager.createNotificationChannel(channel);
//            }
//        }
//    }
//
//    // Message channel with normal notification sound
//    private static void createMessageChannel(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationManager manager = context.getSystemService(NotificationManager.class);
//            if (manager != null && manager.getNotificationChannel(CHANNEL_MESSAGES) == null) {
//                NotificationChannel channel = new NotificationChannel(
//                        CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_DEFAULT
//                );
//                channel.setDescription("Notifications for incoming messages");
//
//                // Normal notification sound for messages
//                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//                AudioAttributes audioAttributes = new AudioAttributes.Builder()
//                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                        .build();
//                channel.setSound(soundUri, audioAttributes);
//
//                // Less aggressive vibration pattern
//                channel.enableVibration(true);
//                channel.setVibrationPattern(new long[]{0, 250, 250, 250});
//
//                manager.createNotificationChannel(channel);
//            }
//        }
//    }
//
//    // === UTILITY ===
//    public static Bitmap getBitmapFromURL(String strURL) {
//        try {
//            if (strURL == null) return null;
//            URL url = new URL(strURL);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setDoInput(true);
//            connection.connect();
//            InputStream input = connection.getInputStream();
//            return BitmapFactory.decodeStream(input);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//}

public class NotificationUtils {

    public static final String CHANNEL_VOIP = "voip_calls";
    public static final String CHANNEL_MESSAGES = "messages_channel";


    public static boolean isAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return false;

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses != null) {
            final String packageName = context.getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.processName.equals(packageName)) {
                    return appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            || appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
                }
            }
        }
        return false;
    }


    // === VOIP INCOMING CALL NOTIFICATION ===

    public static void showIncomingCallUI(Context context, RemoteMessage remoteMessage) {
        Map<String, String> dataMap = remoteMessage.getData();
        String sessionId = dataMap.get("sessionId");
        String senderName = dataMap.get("senderName");
        String callType = dataMap.get("callType");
        String senderAvatar = dataMap.get("senderAvatar");
        String callAction = dataMap.get("callAction");
//        if (isAppInForeground(context)) {
//            // App is foreground: Provide in-app handling (show your own UI, or no-op)
//            return;
//        }
        if (sessionId == null || senderName == null) return;

        // ❌ Don't show notification for ended or cancelled call
        if ("cancelled".equalsIgnoreCase(callAction) || "accepted".equalsIgnoreCase(callAction) || "ongoing".equalsIgnoreCase(callAction) || "ended".equalsIgnoreCase(callAction)) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(sessionId.hashCode());
            }
            return;
        }

        // ✅ Recreate VoIP channel with ringtone
        recreateVoipChannel(context);

        // ✅ Full screen intent
        Intent fullScreenIntent = new Intent(context, VoIPCallActivity.class);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        JSObject voipData = new JSObject();
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            voipData.put(entry.getKey(), entry.getValue());
        }
        voipData.put("id", remoteMessage.getMessageId());
        fullScreenIntent.putExtra("voipData", voipData.toString());

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // ✅ Accept/Decline Intents
//        Intent acceptIntent = new Intent(context, CallActionReceiver.class);
//        acceptIntent.setAction("CALL_ACCEPT");
//        acceptIntent.putExtra("sessionId", sessionId);
//        acceptIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
//                context, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );

        Intent launchAppIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchAppIntent != null) {
            launchAppIntent.setAction("com.capacitor.push.ACCEPT_CALL");
            launchAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            launchAppIntent.putExtra("sessionId", sessionId);
            // Optionally, add all needed extras here (voipData etc)
        }
        PendingIntent acceptPendingIntent = PendingIntent.getActivity(
                context, 1001, launchAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent declineIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (declineIntent != null) {
            declineIntent.setAction("com.capacitor.push.REJECT_CALL");
            declineIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |  Intent.FLAG_ACTIVITY_SINGLE_TOP);
            declineIntent.putExtra("sessionId", sessionId);
        }

        PendingIntent declinePendingIntent = PendingIntent.getActivity(
                context,
                2,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // ✅ Build full-screen notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_VOIP)
                .setContentTitle(senderName)
                .setContentText("Incoming " + (callType != null ? callType : "voice") + " call")
                .setSmallIcon(R.drawable.ic_phone)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(fullScreenPendingIntent)
                .setVibrate(new long[]{0, 1000, 1000, 1000})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));

        // ✅ Add colored icon actions
        builder.addAction(new NotificationCompat.Action.Builder(
                R.drawable.ic_call_accept, "Accept", acceptPendingIntent
        ).build());

        builder.addAction(new NotificationCompat.Action.Builder(
                R.drawable.ic_call_decline, "Decline", declinePendingIntent
        ).build());

        // ✅ Optional avatar
        if (!TextUtils.isEmpty(senderAvatar)) {
            Bitmap avatar = getBitmapFromURL(senderAvatar);
            if (avatar != null) {
                builder.setLargeIcon(avatar);
            }
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(sessionId.hashCode(), builder.build());
        }
    }

    // === FALLBACK MESSAGE NOTIFICATION ===
    public static void showFallbackNotification(Context context, RemoteMessage remoteMessage) {
        Map<String, String> dataMap = remoteMessage.getData();
        String title = dataMap.get("title");
        String body = dataMap.get("body");
        String senderAvatar = dataMap.get("senderAvatar");


        createMessageChannel(context);


        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
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
            if (avatar != null) builder.setLargeIcon(avatar);
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

    // === CHANNEL CREATION ===
    private static void recreateVoipChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.deleteNotificationChannel(CHANNEL_VOIP); // Force recreation (for debug/dev)
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
                if (soundUri == null) soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                channel.setSound(soundUri, audioAttributes);
                manager.createNotificationChannel(channel);

                // Debug info
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

    // === UTILITY ===
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
