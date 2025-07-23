package com.capacitor.push;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Random;

@CapacitorPlugin(
        name = "CapacitorPush",
        permissions = {
                @Permission(
                        alias = "notifications",
                        strings = {
                                Manifest.permission.POST_NOTIFICATIONS,
                                Manifest.permission.WAKE_LOCK,
                                Manifest.permission.VIBRATE
                        }
                ),
                @Permission(
                        alias = "phone",
                        strings = {
                                Manifest.permission.CALL_PHONE,
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.SYSTEM_ALERT_WINDOW,
                                Manifest.permission.USE_FULL_SCREEN_INTENT
                        }
                )
        }
)
public class CapacitorPushPlugin extends Plugin {
    private static final String TAG = "CapacitorPush";
    private static final String CHANNEL_ID = "capacitor_push_notifications";
    private static final String VOIP_CHANNEL_ID = "capacitor_voip_notifications";

    private CapacitorPush implementation;

    @Override
    public void load() {
        Log.d(TAG, "Loading CapacitorPush plugin");
        
        // Initialize Firebase first
        initializeFirebase();
        
        implementation = new CapacitorPush(getContext(), this);
        createNotificationChannels();

        // Set this plugin instance in the bridge
        VoIPCallReceiver.setPluginInstance(this);
        
        // Get initial token after Firebase initialization
        getInitialToken();
    }
    private static CapacitorPushPlugin instance;

    public CapacitorPushPlugin() {
        instance = this;
    }
    public static void sendVoipCallAcceptedEvent(String sessionId) {
        if (instance != null) {
            JSObject callAction = new JSObject();
            callAction.put("sessionId", sessionId);
            callAction.put("type", "voip");
            instance.notifyListeners("voipCallAccepted", callAction, true); // true = deliver even if in background
        }
    }

    public static void sendVoipCallDeclineEvent(String sessionId) {
        if (instance != null) {
            JSObject callAction = new JSObject();
            callAction.put("sessionId", sessionId);
            callAction.put("type", "voip");
            instance.notifyListeners("voipCallRejected", callAction, true); // true = deliver even if in background
        }
    }

    public static void sendNotificationClicked(String id, String convType){
        if (instance != null) {
            JSObject onTap = new JSObject();
            onTap.put("id", id);
            onTap.put("convType", convType);
            instance.notifyListeners("pushNotificationActionPerformed", onTap, true); // true = deliver even if in background
        }
    }
    private void initializeFirebase() {
        try {
            if (FirebaseApp.getApps(getContext()).isEmpty()) {
                FirebaseApp.initializeApp(getContext());
                Log.d(TAG, "Firebase initialized successfully");
            } else {
                Log.d(TAG, "Firebase already initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }
    }

    private void getInitialToken() {
        // Add a small delay to ensure Firebase is fully initialized
        getActivity().runOnUiThread(() -> {
            try {
                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "Fetching initial FCM registration token failed", task.getException());
                                return;
                            }

                            String token = task.getResult();
                            if (token != null && !token.isEmpty()) {
                                Log.d(TAG, "Initial FCM Registration Token: " + token);
                                JSObject tokenData = new JSObject();
                                tokenData.put("token", token);
                                notifyListeners("registration", tokenData);
                            } else {
                                Log.w(TAG, "FCM token is null or empty");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get initial FCM token", e);
                        });
            } catch (Exception e) {
                Log.e(TAG, "Exception getting initial token", e);
            }
        });
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getContext().getSystemService(NotificationManager.class);

            // Regular notifications channel
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Push Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Push notifications from the app");
            notificationManager.createNotificationChannel(channel);

            // VoIP notifications channel
            NotificationChannel voipChannel = new NotificationChannel(
                    VOIP_CHANNEL_ID,
                    "VoIP Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );
            voipChannel.setDescription("Incoming VoIP calls");
            voipChannel.enableVibration(true);
            voipChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationManager.createNotificationChannel(voipChannel);
        }
    }

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");
        JSObject ret = new JSObject();
        ret.put("value", value);
        call.resolve(ret);
    }

    @PluginMethod
    public void register(PluginCall call) {
        Log.d(TAG, "Register method called");
        
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            call.reject("Registration failed: " + task.getException().getMessage());
                            return;
                        }

                        String token = task.getResult();
                        if (token != null && !token.isEmpty()) {
                            Log.d(TAG, "FCM Registration Token: " + token);

                            JSObject tokenData = new JSObject();
                            tokenData.put("token", token);
                            notifyListeners("registration", tokenData);

                            call.resolve(tokenData);
                        } else {
                            Log.w(TAG, "FCM token is null or empty");
                            call.reject("Token is null or empty");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get FCM token", e);
                        call.reject("Failed to get token: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in register method", e);
            call.reject("Exception: " + e.getMessage());
        }
    }

    @PluginMethod
    public void flushPending(PluginCall call) {
        // Implementation for flushing pending notifications
        call.resolve();
    }

    @PluginMethod
    public void getToken(PluginCall call) {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Failed to get token", task.getException());
                            call.reject("Failed to get token: " + task.getException().getMessage());
                            return;
                        }

                        String token = task.getResult();
                        if (token != null && !token.isEmpty()) {
                            Log.d(TAG, "FCM Token retrieved: " + token);
                            JSObject ret = new JSObject();
                            ret.put("fcmToken", token);
                            ret.put("apnsToken", ""); // Not applicable for Android
                            call.resolve(ret);
                        } else {
                            Log.w(TAG, "FCM token is null or empty");
                            call.reject("Token is null or empty");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get FCM token", e);
                        call.reject("Failed to get token: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in getToken method", e);
            call.reject("Exception: " + e.getMessage());
        }
    }

    @PluginMethod
    public void getVoIPToken(PluginCall call) {
        // VoIP token simulation for Android
        JSObject ret = new JSObject();
        ret.put("voipToken", "android_voip_token_simulation");
        call.resolve(ret);
    }

    @PluginMethod
    public void enableVoIP(PluginCall call) {
        Boolean enable = call.getBoolean("enable", true);
        implementation.setVoIPEnabled(enable);
        call.resolve();
    }

    @PluginMethod
    public void setBadgeCount(PluginCall call) {
        Integer count = call.getInt("count", 0);
        // Android doesn't have native badge count, but you can use ShortcutBadger library
        Log.d(TAG, "Badge count set to: " + count);
        call.resolve();
    }

    @PluginMethod
    public void requestPermissions(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionForAlias("notifications", call, "permissionCallback");
        } else {
            JSObject ret = new JSObject();
            ret.put("receive", "granted");
            call.resolve(ret);
        }
    }

    @PluginMethod
    public void checkPermissions(PluginCall call) {
        JSObject ret = new JSObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String permission = getPermissionState("notifications").toString();
            ret.put("receive", permission);
        } else {
            ret.put("receive", "granted");
        }

        call.resolve(ret);
    }

    @PluginMethod
    public void testVoIPSetup(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("isVoIPSupported", true);
        ret.put("hasVoIPToken", true);
        ret.put("voipToken", "android_voip_token_simulation");
        call.resolve(ret);
    }

    @PermissionCallback
    private void permissionCallback(PluginCall call) {
        JSObject ret = new JSObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String permission = getPermissionState("notifications").toString();
            ret.put("receive", permission);
        } else {
            ret.put("receive", "granted");
        }

        call.resolve(ret);
    }

    private Bitmap getBitmapFromURL(String strURL) {
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
                return null;
            }
        } else {
            return null;
        }
    }
    // Method to handle incoming push notifications
    public void handlePushNotification(RemoteMessage remoteMessage) {
        Log.d("TAG", "📥 Handling push notification");

        Map<String, String> dataMap = remoteMessage.getData();
        String type = dataMap.get("type");
        String callAction = dataMap.get("callAction");
        String title = dataMap.get("title");
        String body = dataMap.get("body");

        if ("call".equals(type) & "initiated".equals(callAction)) {
            handleVoIPNotification(remoteMessage);
            return;
        }

        // Create notification channel for Android 8+
        String channelId = "messages_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Messages",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for incoming messages");
            NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), channelId)
                .setContentTitle(title != null ? title : "New Message")
                .setContentText(body != null ? body : "")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Keep consistent with working notification
                .setAutoCancel(true);

        // Set large icon if available
        String largeIconUrl = remoteMessage.getData().get("senderAvatar");
        if (!TextUtils.isEmpty(largeIconUrl)) {
            builder.setLargeIcon(getBitmapFromURL(largeIconUrl));
        } else {
            builder.setLargeIcon(BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_phone));
        }


        // Tap intent → opens your main app (change as needed)
        Intent intent = getContext().getPackageManager()
                .getLaunchIntentForPackage(getContext().getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setContentIntent(pendingIntent);

        // Show notification
        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        int msgId = 0;
        try {
            msgId = Integer.parseInt(dataMap.get("tag")); // unique ID per message
        } catch (Exception e) {
            msgId = new Random().nextInt(); // fallback
        }

        if (notificationManager != null) {
            notificationManager.notify(msgId, notification);
        }

        // Notify JS listeners as well
        JSObject jsNotification = new JSObject();
        jsNotification.put("id", remoteMessage.getMessageId());
        jsNotification.put("title", title);
        jsNotification.put("body", body);

        JSObject jsData = new JSObject();
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            jsData.put(entry.getKey(), entry.getValue());
        }
        jsNotification.put("data", jsData);

        notifyListeners("pushNotificationReceived", jsNotification);
    }


    private void handleVoIPNotification(RemoteMessage remoteMessage) {
        Log.d(TAG, "Handling VoIP (call) notification");

        JSObject voipNotification = new JSObject();
        voipNotification.put("id", remoteMessage.getMessageId());

        Map<String, String> dataMap = remoteMessage.getData();

        // Extract core call data
        voipNotification.put("callType", dataMap.get("callType")); // audio/video
        voipNotification.put("callAction", dataMap.get("callAction")); // initiated, cancelled, etc.
        voipNotification.put("sessionId", dataMap.get("sessionId"));
        voipNotification.put("senderId", dataMap.get("sender"));
        voipNotification.put("senderName", dataMap.get("senderName"));
        voipNotification.put("senderAvatar", dataMap.get("senderAvatar"));
        voipNotification.put("receiverId", dataMap.get("receiver"));
        voipNotification.put("receiverName", dataMap.get("receiverName"));
        voipNotification.put("receiverAvatar", dataMap.get("receiverAvatar"));
        voipNotification.put("conversationId", dataMap.get("conversationId"));

        // Pass full data payload too
        JSObject fullData = new JSObject();
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            fullData.put(entry.getKey(), entry.getValue());
        }
        voipNotification.put("data", fullData);

        // Show incoming call UI only if not cancelled
        String callAction = dataMap.get("callAction");
        if ("cancelled".equalsIgnoreCase(callAction)) {
            Log.d(TAG, "Call was cancelled - skipping UI");
        } else {
            showVoIPCallScreen(voipNotification);
        }

        notifyListeners("voipNotificationReceived", voipNotification);
    }

    private void showVoIPCallScreen(JSObject voipData) {
        Intent intent = new Intent(getContext(), VoIPCallActivity.class);
        intent.putExtra("voipData", voipData.toString());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        getContext().startActivity(intent);
    }

    private void showNotification(JSObject notification) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted");
                return;
            }
        }

        Intent intent = new Intent(getContext(), getActivity().getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(notification.getString("title"))
                .setContentText(notification.getString("body"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        int notificationId = notification.getString("id") != null ? notification.getString("id").hashCode() : 0;
        notificationManager.notify(notificationId, builder.build());
    }

    // Method to handle notification click
    public void handleNotificationClick(String notificationId, String actionId) {
        JSObject actionPerformed = new JSObject();
        actionPerformed.put("actionId", actionId);

        JSObject notification = new JSObject();
        notification.put("id", notificationId);
        actionPerformed.put("notification", notification);

        notifyListeners("pushNotificationActionPerformed", actionPerformed);
    }

    // Method to handle VoIP call actions
    public void handleVoIPCallAction(String sessionId, String action) {
        JSObject callAction = new JSObject();
        callAction.put("sessionId", sessionId);
        callAction.put("type", "voip");

        switch (action) {
            case "accept":
                notifyListeners("voipCallAccepted", callAction);
                break;
            case "reject":
                notifyListeners("voipCallRejected", callAction);
                break;
            case "cancel":
                notifyListeners("voipCallCancelled", callAction);
                break;
        }
    }

    // PUBLIC METHOD FOR TOKEN REFRESH
    public void handleTokenRefresh(JSObject tokenData) {
        Log.d(TAG, "Token refreshed: " + tokenData.toString());
        notifyListeners("registration", tokenData);
    }
}