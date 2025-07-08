package com.capacitor.push;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.Map;
import java.util.Random;

public class NotificationUtils {
    private static final String CHANNEL_ID = "messages_channel";

    public static Bitmap getBitmapFromURL(String strURL) {
        if (strURL != null) {
            try {
                URL url = new URL(strURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static void showIncomingCallUI(Context context, RemoteMessage remoteMessage) {
        Map<String, String> dataMap = remoteMessage.getData();
        String sessionId = dataMap.get("sessionId");
        String senderName = dataMap.get("senderName");
        String callType = dataMap.get("callType");
        String senderAvatar = dataMap.get("senderAvatar");

        if (sessionId == null || senderName == null) {
            return;
        }

        // Prepare intent to open full screen VoIPCallActivity
        Intent fullScreenIntent = new Intent(context, VoIPCallActivity.class);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Pass voipData as JSON
        JSObject voipData = new JSObject();
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            voipData.put(entry.getKey(), entry.getValue());
        }
        voipData.put("id", remoteMessage.getMessageId());
        fullScreenIntent.putExtra("voipData", voipData.toString());

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Setup VoIP call channel
        String channelId = "voip_calls";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "VoIP Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Incoming call notifications");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Setup actions
        Intent acceptIntent = new Intent(context, CallActionReceiver.class);
        acceptIntent.setAction("CALL_ACCEPT");
        acceptIntent.putExtra("sessionId", sessionId);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent rejectIntent = new Intent(context, CallActionReceiver.class);
        rejectIntent.setAction("CALL_REJECT");
        rejectIntent.putExtra("sessionId", sessionId);
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                rejectIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(senderName)
                .setContentText("Incoming " + callType + " call")
                .setSmallIcon(R.drawable.ic_phone) // Use your app’s icon
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_notification_large, "Answer", acceptPendingIntent)
                .addAction(R.drawable.ic_video_call, "Reject", rejectPendingIntent);

        // Optionally add avatar as large icon
        if (senderAvatar != null && !senderAvatar.isEmpty()) {
            Bitmap avatar = NotificationUtils.getBitmapFromURL(senderAvatar);
            if (avatar != null) {
                builder.setLargeIcon(avatar);
            }
        }

        // Post notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            int notificationId = sessionId.hashCode(); // consistent ID for canceling later
            notificationManager.notify(notificationId, builder.build());
        }
    }

    public static void showFallbackNotification(Context context, RemoteMessage remoteMessage) {
        Log.d("NotificationUtils", "Showing fallback notification");

        Map<String, String> dataMap = remoteMessage.getData();
        String title = dataMap.get("title");
        String body = dataMap.get("body");

        // Create notification channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Messages",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for incoming messages");
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title != null ? title : "New Message")
                .setContentText(body != null ? body : "")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true);

        // Optional large icon
        String largeIconUrl = dataMap.get("senderAvatar");
        if (!TextUtils.isEmpty(largeIconUrl)) {
            builder.setLargeIcon(getBitmapFromURL(largeIconUrl));
        } else {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_phone));
        }

        // Open app intent
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setContentIntent(pendingIntent);

        Notification notification = builder.build();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int msgId;
        try {
            msgId = Integer.parseInt(dataMap.get("tag"));
        } catch (Exception e) {
            msgId = new Random().nextInt();
        }

        if (manager != null) {
            manager.notify(msgId, notification);
        }
    }
}
