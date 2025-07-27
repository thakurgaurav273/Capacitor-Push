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
public class NotificationUtils {

    public static final String CHANNEL_VOIP = "voip_calls";
    public static final String CHANNEL_MESSAGES = "messages_channel";

    // === VOIP INCOMING CALL NOTIFICATION ===

    public static void showIncomingCallUI(Context context, RemoteMessage remoteMessage) {
        Map<String, String> dataMap = remoteMessage.getData();
        String sessionId = dataMap.get("sessionId");
        String senderName = dataMap.get("senderName");
        String callType = dataMap.get("callType");
        String senderAvatar = dataMap.get("senderAvatar");
        String callAction = dataMap.get("callAction");
        if (sessionId == null || senderName == null) return;

        // ❌ Don't show notification for ended or cancelled call

        if("cancelled".equalsIgnoreCase(callAction)){
            // Send a broadcast to close VoIPCallActivity if it is visible for this session
            Intent finishIntent = new Intent("com.capacitor.push.ACTION_CALL_CANCELLED");
            finishIntent.putExtra("sessionId", sessionId);
            try {
                context.sendBroadcast(finishIntent);
            } catch (NoClassDefFoundError e) {
                // LocalBroadcastManager not available, fallback to normal broadcast:
                context.sendBroadcast(finishIntent);
            }
        }


        if ("cancelled".equalsIgnoreCase(callAction) || "rejected".equalsIgnoreCase(callAction) || "accepted".equalsIgnoreCase(callAction) || "ongoing".equalsIgnoreCase(callAction) || "ended".equalsIgnoreCase(callAction)) {
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

        Intent launchAppIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchAppIntent != null) {
            launchAppIntent.setAction("com.capacitor.push.ACCEPT_CALL");
            launchAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            launchAppIntent.putExtra("sessionId", sessionId);
            launchAppIntent.putExtra("type", callType);

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
            declineIntent.putExtra("type", callType);

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
        Log.d("TAG", "showFallbackNotification: "+ remoteMessage);
        String receiverType = dataMap.get("receiverType"); // "user" or "group" expected
        String sender = dataMap.get("sender");
        String receiver = dataMap.get("receiver");
        createMessageChannel(context);
        Intent tapIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assert tapIntent != null;

        String id;
        String convType;

        if ("user".equalsIgnoreCase(receiverType)) {
            id = sender;
            convType = "user";
        } else {
            id = receiver;
            convType = "group";
        }
        tapIntent.setAction("com.capacitor.push.NOTIFICATION_TAPPED");
        tapIntent.putExtra("id", id);        // or some notification ID
        tapIntent.putExtra("convType",convType); // pass any needed extras

// Important: add flags so the existing activity gets the intent via onNewIntent()
        tapIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

//        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
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
